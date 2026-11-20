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
import pe.odontosync.dao.ClinicaDAO;
import pe.odontosync.dao.JdbcClinicaDAO;
import pe.odontosync.dao.JdbcRecetaDAO;
import pe.odontosync.model.ClinicaResumen;
import pe.odontosync.model.RecetaResumen;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.RecetaPdfService;
import pe.odontosync.service.RecetaService;

/** Descarga protegida del PDF de una receta médica individual. */
@WebServlet("/modulo/recetas/pdf")
public final class RecetaPdfServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private transient RecetaService recetaService;
    private transient RecetaPdfService pdfService;
    private transient ClinicaDAO clinicaDAO;

    @Override
    public void init() {
        recetaService = new RecetaService(new JdbcRecetaDAO());
        pdfService = new RecetaPdfService();
        clinicaDAO = new JdbcClinicaDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {

        User current = currentDentist(req, res);
        if (current == null) return;

        try {
            long recipeId = parseId(req.getParameter("id"));
            RecetaResumen receta = recetaService.detail(current, recipeId);

            ClinicaResumen clinica = clinicaDAO.findById(current.clinicId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "La clínica de la receta no está disponible."
                ));

            byte[] pdf = pdfService.generate(receta, clinica);

            res.setContentType("application/pdf");
            res.setContentLength(pdf.length);
            res.setHeader(
                "Content-Disposition",
                "attachment; filename=\"receta-" + receta.idReceta() + ".pdf\""
            );
            res.setHeader("Cache-Control", "no-store");

            res.getOutputStream().write(pdf);
        } catch (IllegalArgumentException ex) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo generando PDF de receta", ex);
            res.sendError(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "No se pudo generar el PDF de la receta."
            );
        }
    }

    private User currentDentist(HttpServletRequest req, HttpServletResponse res)
        throws IOException {

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }

        if (user.role() != Role.ODONTOLOGO || !user.role().allows("recetas")) {
            AuditLog.accessDenied(user, "/modulo/recetas/pdf");
            res.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Solo el odontólogo puede descargar una receta."
            );
            return null;
        }

        return user;
    }

    private long parseId(String raw) {
        try {
            long id = Long.parseLong(StringUtils.trimToEmpty(raw));
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Selecciona una receta válida.", ex);
        }
    }
}
