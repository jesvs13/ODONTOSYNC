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
import pe.odontosync.dao.JdbcPacienteDAO;
import pe.odontosync.dao.JdbcRecetaDAO;
import pe.odontosync.model.PacienteDetalle;
import pe.odontosync.model.RecetaResumen;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.PacienteService;
import pe.odontosync.service.RecetaService;

/** Consulta protegida de una receta médica individual. */
@WebServlet("/modulo/recetas/ver")
public final class RecetaDetalleServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private transient RecetaService service;
    private transient PacienteService pacienteService;

    @Override
    public void init() {
        service = new RecetaService(new JdbcRecetaDAO());
        pacienteService = new PacienteService(new JdbcPacienteDAO());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {

        User current = currentDentist(req, res);
        if (current == null) return;

        try {
            long recipeId = parseId(req.getParameter("id"));
            RecetaResumen recipe = service.detail(current, recipeId);

            PacienteDetalle patient = pacienteService.findVisible(
                current,
                recipe.idPaciente()
            ).orElseThrow(() -> new IllegalArgumentException(
                "El paciente de esta receta no está disponible para tu perfil."
            ));

            req.setAttribute("user", current);
            req.setAttribute("active", "historias");
            req.setAttribute("recipe", recipe);
            req.setAttribute("recipePatient", patient);
            req.setAttribute("returnPatientId", recipe.idPaciente());
            req.getRequestDispatcher(
                "/WEB-INF/views/receta-detalle.jsp"
            ).forward(req, res);
        } catch (IllegalArgumentException ex) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo consultando receta individual", ex);
            res.sendError(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "No se pudo consultar la receta en MySQL."
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
            AuditLog.accessDenied(user, "/modulo/recetas/ver");
            res.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Solo el odontólogo puede consultar una receta."
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
            throw new IllegalArgumentException(
                "Selecciona una receta válida.",
                ex
            );
        }
    }
}
