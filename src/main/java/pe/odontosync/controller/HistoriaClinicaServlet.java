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
import pe.odontosync.dao.JdbcRecetaDAO;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.HistoriaClinicaService;
import pe.odontosync.service.RecetaService;

/** Historia clínica longitudinal visible únicamente al odontólogo. */
@WebServlet("/historias")
public final class HistoriaClinicaServlet extends HttpServlet {
    private final HistoriaClinicaService service =
        new HistoriaClinicaService(new JdbcClinicaAtencionDAO());
    private final RecetaService recetaService =
        new RecetaService(new JdbcRecetaDAO());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {

        User current = currentDentist(req, res);
        if (current == null) return;

        try {
            String query = value(req, "q");
            req.setAttribute("user", current);
            req.setAttribute("active", "historias");
            req.setAttribute("historyQuery", query);

            String patient = value(req, "paciente");
            if (patient.isBlank()) {
                req.setAttribute("historyList", service.list(current, query));
            } else {
                long patientId = parseId(patient, "paciente");
                req.setAttribute("historyView", service.load(current, patientId));
                req.setAttribute(
                    "patientRecipes",
                    recetaService.historyForPatient(current, patientId)
                );
                req.setAttribute("historyTab", normalizeTab(value(req, "tab")));
            }

            loadFlashMessages(req);
            req.getRequestDispatcher("/WEB-INF/views/historias.jsp").forward(req, res);
        } catch (IllegalArgumentException ex) {
            flash(req, "historyError", ex.getMessage());
            res.sendRedirect(req.getContextPath() + "/historias");
        } catch (SQLException ex) {
            AppLog.error("Fallo consultando historia clínica", ex);
            res.sendError(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "No se pudo consultar la historia clínica en MySQL."
            );
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {

        User current = currentDentist(req, res);
        if (current == null) return;

        req.setCharacterEncoding("UTF-8");
        if (!validCsrf(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Solicitud rechazada.");
            return;
        }

        long patientId = safeId(value(req, "id_paciente"));
        String returnTab = "resumen";

        try {
            patientId = parseId(value(req, "id_paciente"), "paciente");

            switch (value(req, "accion")) {
                case "notas" -> {
                    service.updateGeneralNotes(
                        current,
                        patientId,
                        value(req, "observaciones_generales")
                    );
                    AuditLog.businessChange(
                        current,
                        "UPDATE_NOTES",
                        "HISTORIA_CLINICA",
                        patientId
                    );
                    flash(
                        req,
                        "historySuccess",
                        "Observaciones generales actualizadas."
                    );
                    returnTab = "resumen";
                }

                case "antecedente" -> {
                    service.addMedicalHistory(
                        current,
                        patientId,
                        value(req, "tipo"),
                        value(req, "descripcion"),
                        value(req, "observaciones")
                    );
                    AuditLog.businessChange(
                        current,
                        "ADD_ANTECEDENTE",
                        "HISTORIA_CLINICA",
                        patientId
                    );
                    flash(
                        req,
                        "historySuccess",
                        "Antecedente médico agregado a la historia."
                    );
                    returnTab = "antecedentes";
                }

                case "alergia" -> {
                    service.addAllergy(
                        current,
                        patientId,
                        value(req, "alergeno"),
                        value(req, "reaccion"),
                        value(req, "observaciones")
                    );
                    AuditLog.businessChange(
                        current,
                        "ADD_ALERGIA",
                        "HISTORIA_CLINICA",
                        patientId
                    );
                    flash(
                        req,
                        "historySuccess",
                        "Alergia agregada a la historia."
                    );
                    returnTab = "antecedentes";
                }

                case "receta" -> {
                    long recipeId = recetaService.create(
                        current,
                        patientId,
                        value(req, "medicamentos"),
                        value(req, "indicaciones")
                    );
                    AuditLog.businessChange(
                        current,
                        "CREATE",
                        "RECETA",
                        recipeId
                    );
                    flash(
                        req,
                        "historySuccess",
                        "Receta médica emitida correctamente."
                    );
                    returnTab = "recetas";
                }

                default -> throw new IllegalArgumentException(
                    "Acción de historia clínica no válida."
                );
            }
        } catch (IllegalArgumentException ex) {
            flash(req, "historyError", ex.getMessage());
            returnTab = actionTab(value(req, "accion"));
        } catch (SQLException ex) {
            AppLog.error("Fallo guardando historia clínica", ex);
            String state = ex.getSQLState();

            String message;
            if ("receta".equals(value(req, "accion"))) {
                message = state != null && state.startsWith("42")
                    ? "MySQL rechazó la operación. Ejecuta permisos_recetas.sql."
                    : "No se pudo registrar la receta en MySQL.";
                returnTab = "recetas";
            } else {
                message = state != null && state.startsWith("28")
                    ? "MySQL rechazó los permisos clínicos. Ejecuta permisos_atencion_historia.sql."
                    : "No se pudo guardar la historia clínica. Revisa el log de Tomcat si el problema continúa.";
                returnTab = actionTab(value(req, "accion"));
            }

            flash(req, "historyError", message);
        }

        String target = req.getContextPath() + "/historias";
        if (patientId > 0) {
            target += "?paciente=" + patientId + "&tab=" + returnTab;
        }
        res.sendRedirect(target);
    }

    private User currentDentist(HttpServletRequest req, HttpServletResponse res)
        throws IOException {

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }

        if (user.role() != Role.ODONTOLOGO) {
            res.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "La historia clínica solo está disponible para Odontología."
            );
            return null;
        }

        return user;
    }

    private String normalizeTab(String raw) {
        return switch (raw) {
            case "", "resumen" -> "resumen";
            case "antecedentes" -> "antecedentes";
            case "consultas" -> "consultas";
            case "recetas" -> "recetas";
            default -> "resumen";
        };
    }

    private String actionTab(String action) {
        return switch (action) {
            case "alergia", "antecedente" -> "antecedentes";
            case "receta" -> "recetas";
            default -> "resumen";
        };
    }

    private void loadFlashMessages(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;

        req.setAttribute("success", session.getAttribute("historySuccess"));
        req.setAttribute("error", session.getAttribute("historyError"));
        session.removeAttribute("historySuccess");
        session.removeAttribute("historyError");
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
            throw new IllegalArgumentException(
                "Identificador de " + label + " no válido."
            );
        }
    }

    private long safeId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void flash(HttpServletRequest req, String key, String message) {
        req.getSession().setAttribute(key, message);
    }
}
