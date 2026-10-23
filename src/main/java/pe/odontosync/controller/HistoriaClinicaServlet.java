package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import org.apache.commons.lang3.StringUtils;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.JdbcClinicaAtencionDAO;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.HistoriaClinicaService;

/** Historia clínica longitudinal visible únicamente al odontólogo. */
@WebServlet("/historias")
public final class HistoriaClinicaServlet extends HttpServlet {
    private final HistoriaClinicaService service = new HistoriaClinicaService(new JdbcClinicaAtencionDAO());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentDentist(req, res);
        if (current == null) return;
        try {
            String query = value(req, "q");
            req.setAttribute("user", current);
            req.setAttribute("active", "historias");
            req.setAttribute("historyList", service.list(current, query));
            req.setAttribute("historyQuery", query);
            String patient = value(req, "paciente");
            if (!patient.isBlank()) {
                req.setAttribute("historyView", service.load(current, parseId(patient, "paciente")));
            }
            HttpSession session = req.getSession(false);
            if (session != null) {
                req.setAttribute("success", session.getAttribute("historySuccess"));
                req.setAttribute("error", session.getAttribute("historyError"));
                session.removeAttribute("historySuccess");
                session.removeAttribute("historyError");
            }
            req.getRequestDispatcher("/WEB-INF/views/historias.jsp").forward(req, res);
        } catch (IllegalArgumentException ex) {
            flash(req, "historyError", ex.getMessage());
            res.sendRedirect(req.getContextPath() + "/historias");
        } catch (SQLException ex) {
            AppLog.error("Fallo consultando historia clínica", ex);
            res.sendError(503, "No se pudo consultar la historia clínica en MySQL.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentDentist(req, res);
        if (current == null) return;
        req.setCharacterEncoding("UTF-8");
        if (!validCsrf(req)) {
            res.sendError(403, "Solicitud rechazada.");
            return;
        }
        long patientId = safeId(value(req, "id_paciente"));
        try {
            patientId = parseId(value(req, "id_paciente"), "paciente");
            switch (value(req, "accion")) {
                case "notas" -> {
                    service.updateGeneralNotes(current, patientId, value(req, "observaciones_generales"));
                    AuditLog.businessChange(current, "UPDATE_NOTES", "HISTORIA_CLINICA", patientId);
                    flash(req, "historySuccess", "Observaciones generales actualizadas.");
                }
                case "antecedente" -> {
                    service.addMedicalHistory(
                        current, patientId, value(req, "tipo"), value(req, "descripcion"), value(req, "observaciones")
                    );
                    AuditLog.businessChange(current, "ADD_ANTECEDENTE", "HISTORIA_CLINICA", patientId);
                    flash(req, "historySuccess", "Antecedente médico agregado a la historia.");
                }
                case "alergia" -> {
                    service.addAllergy(
                        current, patientId, value(req, "alergeno"), value(req, "reaccion"), value(req, "observaciones")
                    );
                    AuditLog.businessChange(current, "ADD_ALERGIA", "HISTORIA_CLINICA", patientId);
                    flash(req, "historySuccess", "Alergia agregada a la historia.");
                }
                default -> throw new IllegalArgumentException("Acción de historia clínica no válida.");
            }
        } catch (IllegalArgumentException ex) {
            flash(req, "historyError", ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo guardando historia clínica", ex);
            String state = ex.getSQLState();
            flash(req, "historyError", state != null && state.startsWith("28")
                ? "MySQL rechazó los permisos clínicos. Ejecuta permisos_atencion_historia.sql."
                : "No se pudo guardar la historia clínica. Revisa el log de Tomcat si el problema continúa.");
        }
        res.sendRedirect(req.getContextPath() + "/historias" + (patientId > 0 ? "?paciente=" + patientId : ""));
    }

    private User currentDentist(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        if (user.role() != Role.ODONTOLOGO) {
            res.sendError(403, "La historia clínica solo está disponible para Odontología.");
            return null;
        }
        return user;
    }

    private boolean validCsrf(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute("csrf");
        return expected != null && expected.equals(req.getParameter("csrf"));
    }

    private String value(HttpServletRequest req, String name) {
        return StringUtils.trimToEmpty(req.getParameter(name));
    }

    private long parseId(String raw, String label) {
        try {
            long id = Long.parseLong(raw);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identificador de " + label + " no válido.");
        }
    }

    private long safeId(String raw) {
        try { return Long.parseLong(raw); } catch (Exception ignored) { return 0; }
    }

    private void flash(HttpServletRequest req, String key, String message) {
        req.getSession().setAttribute(key, message);
    }
}
