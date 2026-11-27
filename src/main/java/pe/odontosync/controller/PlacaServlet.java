package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.config.RadiografiaConfig;
import pe.odontosync.model.PlacaDetalle;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.PlacaService;

/** Carga y acceso protegido: la ruta nunca acepta claves de almacenamiento. */
@WebServlet(urlPatterns = {"/modulo/placas", "/modulo/placas/archivo"})
@MultipartConfig(fileSizeThreshold = 1048576, maxFileSize = 10485760L,
    maxRequestSize = 11534336L)
public final class PlacaServlet extends HttpServlet {
    private static final int MAX_BYTES = 10 * 1024 * 1024;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User current = dentist(req, res);
        if (current == null) return;
        if ("/modulo/placas".equals(req.getServletPath())) {
            res.sendRedirect(req.getContextPath() + "/historias");
            return;
        }
        try {
            long id = positive(req.getParameter("id"));
            PlacaService service = RadiografiaConfig.service();
            PlacaDetalle plate = service.detail(current, id);
            String mime = plate.tipoMime();
            String extension = switch (mime == null ? "" : mime) {
                case "image/png" -> ".png";
                case "image/jpeg" -> ".jpg";
                case "application/pdf" -> ".pdf";
                default -> throw new IllegalArgumentException("Formato no disponible.");
            };
            // Los PDF se entregan exclusivamente como adjuntos; no se relaja CSP.
            boolean inline = mime.startsWith("image/") && "ver".equals(req.getParameter("modo"));
            byte[] bytes = service.download(current, id);
            String name = URLEncoder.encode(plate.nombreArchivo(), StandardCharsets.UTF_8)
                .replace("+", "%20");
            res.setHeader("Cache-Control", "no-store, private, max-age=0");
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("Content-Disposition", (inline ? "inline" : "attachment")
                + "; filename=\"radiografia-" + id + extension + "\"; filename*=UTF-8''" + name);
            res.setContentType(mime);
            res.setContentLength(bytes.length);
            AuditLog.businessChange(current, inline ? "VIEW" : "DOWNLOAD", "RADIOGRAFIA", id);
            res.getOutputStream().write(bytes);
        } catch (IllegalArgumentException ex) {
            AuditLog.accessDenied(current, "/modulo/placas/archivo");
            if (!res.isCommitted()) res.sendError(404, "Radiografía no disponible.");
        } catch (SQLException | IOException | IllegalStateException ex) {
            AppLog.error("Fallo recuperando archivo radiográfico", ex);
            if (!res.isCommitted()) res.sendError(503, "No se pudo consultar el archivo radiográfico.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User current = dentist(req, res);
        if (current == null) return;
        if (!"/modulo/placas".equals(req.getServletPath())) {
            res.sendError(405);
            return;
        }
        req.setCharacterEncoding("UTF-8");
        long patientId = 0;
        long returnPatientId = returnPatientHint(req.getQueryString());
        Part file = null;
        try {
            HttpSession session = req.getSession(false);
            String expected = (String) session.getAttribute("csrf");
            if (expected == null || !expected.equals(req.getParameter("csrf"))) {
                res.sendError(403, "Solicitud rechazada.");
                return;
            }
            patientId = positive(req.getParameter("id_paciente"));
            PlacaService service = RadiografiaConfig.service();
            String action = req.getParameter("accion");
            if ("crear".equals(action)) {
                file = req.getPart("archivo");
                if (file == null || file.getSize() <= 0 || file.getSize() > MAX_BYTES) {
                    throw new IllegalArgumentException("Selecciona un archivo de hasta 10 MB.");
                }
                byte[] bytes;
                try (var input = file.getInputStream()) { bytes = input.readNBytes(MAX_BYTES + 1); }
                LocalDate date;
                try { date = LocalDate.parse(req.getParameter("fecha_radiografia") == null
                    ? "" : req.getParameter("fecha_radiografia")); }
                catch (DateTimeParseException ex) { throw new IllegalArgumentException("Fecha de radiografía no válida."); }
                long id = service.create(current, patientId, req.getParameter("tipo_radiografia"),
                    date, req.getParameter("region_dental"), req.getParameter("observaciones"),
                    file.getSubmittedFileName(), file.getContentType(), bytes);
                AuditLog.businessChange(current, "CREATE", "RADIOGRAFIA", id);
                session.setAttribute("historySuccess", "Radiografía registrada correctamente.");
            } else if ("anular".equals(action)) {
                long id = positive(req.getParameter("id_placa"));
                if (service.detail(current, id).idPaciente() != patientId) {
                    throw new IllegalArgumentException("Radiografía no disponible para este paciente.");
                }
                service.annul(current, id, req.getParameter("motivo"));
                AuditLog.businessChange(current, "ANNUL", "RADIOGRAFIA", id);
                session.setAttribute("historySuccess", "Radiografía anulada. El registro y el archivo se conservan.");
            } else {
                throw new IllegalArgumentException("Acción no válida.");
            }
        } catch (IllegalArgumentException ex) {
            req.getSession(false).setAttribute("historyError", ex.getMessage());
        } catch (IllegalStateException | ServletException ex) {
            req.getSession(false).setAttribute("historyError", "No se pudo procesar la carga. Verifica el formato y el límite de 10 MB.");
        } catch (SQLException | IOException ex) {
            AppLog.error("Fallo guardando radiografía", ex);
            req.getSession(false).setAttribute("historyError", "No se pudo completar la operación radiográfica.");
        } finally {
            if (file != null) {
                try { file.delete(); } catch (IOException ex) { AppLog.warn("No se pudo limpiar la carga temporal de radiografía."); }
            }
        }
        long targetPatient = patientId > 0 ? patientId : returnPatientId;
        res.sendRedirect(req.getContextPath() + "/historias" +
            (targetPatient > 0 ? "?paciente=" + targetPatient + "&tab=radiografias" : ""));
    }

    // Only a navigation hint if multipart parsing fails; Historia rechecks patient access.
    private long returnPatientHint(String query) {
        if (query == null || !query.matches("paciente=[0-9]{1,19}")) return 0;
        try { return positive(query.substring("paciente=".length())); }
        catch (IllegalArgumentException ex) { return 0; }
    }

    private User dentist(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) { res.sendRedirect(req.getContextPath() + "/login"); return null; }
        if (user.role() != Role.ODONTOLOGO || !user.role().allows("placas")) {
            AuditLog.accessDenied(user, req.getServletPath());
            res.sendError(403, "Acceso no autorizado.");
            return null;
        }
        return user;
    }

    private long positive(String raw) {
        try {
            long value = Long.parseLong(raw);
            if (value > 0) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Identificador no válido.");
    }
}
