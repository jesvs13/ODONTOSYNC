package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import pe.odontosync.User;
import pe.odontosync.dao.JdbcOdontologoDAO;
import pe.odontosync.model.OdontologoDetalle;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.OdontologoService;

/**
 * Directorio y CRUD administrativo de odontólogos.
 */
@WebServlet("/odontologos")
public final class OdontologoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String MODULE_ODONTOLOGOS = "odontologos";
    private static final String PATH_ODONTOLOGOS = "/odontologos";
    private static final String VIEW_ODONTOLOGOS = "/WEB-INF/views/odontologos.jsp";

    private static final String ATTR_SUCCESS = "dentistSuccess";
    private static final String ATTR_ERROR = "dentistError";

    private static final String PARAM_ESTADO = "estado";
    private static final String ENTITY_ODONTOLOGO = "ODONTOLOGO";

    /*
     * transient evita el problema de serialización detectado por SonarQube.
     * El servicio se inicializa mediante el ciclo de vida del Servlet.
     */
    private transient OdontologoService service;

    @Override
    public void init() {
        service = new OdontologoService(new JdbcOdontologoDAO());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {

        User current = currentUser(req, res);
        if (current == null) {
            return;
        }

        try {
            String query = value(req, "q");

            req.setAttribute("user", current);
            req.setAttribute("active", MODULE_ODONTOLOGOS);
            req.setAttribute("query", query);
            req.setAttribute(
                "canManageDentists",
                service.canManage(current.role())
            );
            req.setAttribute(
                MODULE_ODONTOLOGOS,
                service.list(current, query)
            );
            req.setAttribute(
                "especialidades",
                service.specialties()
            );

            loadEditDentist(req, res, current);
            loadFlashMessages(req);

            req.getRequestDispatcher(VIEW_ODONTOLOGOS)
                .forward(req, res);

        } catch (IllegalArgumentException ex) {

            flash(req, ATTR_ERROR, ex.getMessage());
            redirect(req, res, PATH_ODONTOLOGOS);

        } catch (SQLException ex) {

            AppLog.error(
                "Fallo consultando odontólogos",
                ex
            );

            sendError(
                res,
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "No se pudo consultar la información de odontólogos."
            );

        } catch (ServletException | IOException ex) {

            AppLog.error(
                "Fallo procesando la vista de odontólogos",
                ex
            );

            sendError(
                res,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "No se pudo completar la solicitud."
            );
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) {

        User current = currentUser(req, res);
        if (current == null) {
            return;
        }

        if (!configureEncoding(req, res)) {
            return;
        }

        if (!validCsrf(req)) {
            sendError(
                res,
                HttpServletResponse.SC_FORBIDDEN,
                "Solicitud rechazada."
            );
            return;
        }

        if (!service.canManage(current.role())) {
            sendError(
                res,
                HttpServletResponse.SC_FORBIDDEN,
                "Solo Administración puede modificar odontólogos."
            );
            return;
        }

        char[] password = value(req, "clave").toCharArray();

        try {

            processAction(req, res, current, password);

        } catch (IllegalArgumentException ex) {

            flash(req, ATTR_ERROR, ex.getMessage());

        } catch (SQLException ex) {

            AppLog.error(
                "Fallo guardando CRUD de odontólogos",
                ex
            );

            flash(
                req,
                ATTR_ERROR,
                sqlErrorMessage(ex)
            );

        } finally {

            Arrays.fill(password, '\0');
        }

        if (!res.isCommitted()) {
            redirect(req, res, PATH_ODONTOLOGOS);
        }
    }

    private void processAction(
        HttpServletRequest req,
        HttpServletResponse res,
        User current,
        char[] password
    ) throws SQLException {

        String action = value(req, "accion");

        switch (action) {

            case "crear" -> createDentist(
                req,
                res,
                current,
                password
            );

            case "actualizar" -> updateDentist(
                req,
                current,
                password
            );

            case PARAM_ESTADO -> changeDentistStatus(
                req,
                current
            );

            default -> throw new IllegalArgumentException(
                "Acción no válida."
            );
        }
    }

    private void createDentist(
        HttpServletRequest req,
        HttpServletResponse res,
        User current,
        char[] password
    ) throws SQLException {

        long id = service.create(
            current,
            value(req, "nombres"),
            value(req, "apellidos"),
            value(req, "correo"),
            value(req, "telefono"),
            password,
            value(req, "colegiatura"),
            value(req, "fecha_ingreso"),
            value(req, PARAM_ESTADO),
            specialtyIds(req)
        );

        AuditLog.businessChange(
            current,
            "CREATE",
            ENTITY_ODONTOLOGO,
            id
        );

        flash(
            req,
            ATTR_SUCCESS,
            "Odontólogo registrado. Su cuenta ya puede iniciar sesión si está ACTIVO."
        );

        redirect(
            req,
            res,
            PATH_ODONTOLOGOS + "?editar=" + id
        );
    }

    private void updateDentist(
        HttpServletRequest req,
        User current,
        char[] password
    ) throws SQLException {

        long id = parseId(
            value(req, "id_odontologo")
        );

        service.update(
            current,
            id,
            value(req, "nombres"),
            value(req, "apellidos"),
            value(req, "correo"),
            value(req, "telefono"),
            password,
            value(req, "colegiatura"),
            value(req, "fecha_ingreso"),
            value(req, PARAM_ESTADO),
            specialtyIds(req)
        );

        AuditLog.businessChange(
            current,
            "UPDATE",
            ENTITY_ODONTOLOGO,
            id
        );

        flash(
            req,
            ATTR_SUCCESS,
            "Perfil profesional y cuenta de acceso actualizados correctamente."
        );
    }

    private void changeDentistStatus(
        HttpServletRequest req,
        User current
    ) throws SQLException {

        long id = parseId(
            value(req, "id_odontologo")
        );

        service.changeStatus(
            current,
            id,
            value(req, PARAM_ESTADO)
        );

        AuditLog.businessChange(
            current,
            "STATUS_CHANGE",
            ENTITY_ODONTOLOGO,
            id
        );

        flash(
            req,
            ATTR_SUCCESS,
            "Estado del odontólogo actualizado. Sus registros históricos se conservan."
        );
    }

    private void loadEditDentist(
        HttpServletRequest req,
        HttpServletResponse res,
        User current
    ) throws SQLException {

        String edit = req.getParameter("editar");

        if (StringUtils.isBlank(edit)) {
            return;
        }

        if (!service.canManage(current.role())) {

            sendError(
                res,
                HttpServletResponse.SC_FORBIDDEN,
                "Tu perfil puede consultar odontólogos, pero no modificarlos."
            );

            return;
        }

        OdontologoDetalle selected =
            service.find(current, parseId(edit))
                .orElseThrow(
                    () -> new IllegalArgumentException(
                        "El odontólogo solicitado no existe."
                    )
                );

        req.setAttribute(
            "editDentist",
            selected
        );
    }

    private void loadFlashMessages(
        HttpServletRequest req
    ) {

        HttpSession session =
            req.getSession(false);

        if (session == null) {
            return;
        }

        req.setAttribute(
            "success",
            session.getAttribute(ATTR_SUCCESS)
        );

        req.setAttribute(
            "error",
            session.getAttribute(ATTR_ERROR)
        );

        session.removeAttribute(ATTR_SUCCESS);
        session.removeAttribute(ATTR_ERROR);
    }

    private User currentUser(
        HttpServletRequest req,
        HttpServletResponse res
    ) {

        HttpSession session =
            req.getSession(false);

        User user =
            session == null
                ? null
                : (User) session.getAttribute("user");

        if (user == null) {

            redirect(
                req,
                res,
                "/login"
            );

            return null;
        }

        if (!user.role().allows(MODULE_ODONTOLOGOS)) {

            sendError(
                res,
                HttpServletResponse.SC_FORBIDDEN,
                "No tienes permiso para acceder a odontólogos."
            );

            return null;
        }

        return user;
    }

    private List<Long> specialtyIds(
        HttpServletRequest req
    ) {

        String[] raw =
            req.getParameterValues("especialidades");

        if (raw == null) {
            return List.of();
        }

        List<Long> result =
            new ArrayList<>();

        for (String value : raw) {

            try {

                long id =
                    Long.parseLong(value);

                if (id <= 0) {
                    throw new NumberFormatException();
                }

                result.add(id);

            } catch (NumberFormatException ex) {

                throw new IllegalArgumentException(
                    "Una especialidad seleccionada no es válida.",
                    ex
                );
            }
        }

        return result;
    }

    private boolean validCsrf(
        HttpServletRequest req
    ) {

        HttpSession session =
            req.getSession(false);

        String expected =
            session == null
                ? null
                : (String) session.getAttribute("csrf");

        String supplied =
            req.getParameter("csrf");

        return expected != null
            && expected.equals(supplied);
    }

    private void flash(
        HttpServletRequest req,
        String key,
        String message
    ) {

        req.getSession()
            .setAttribute(key, message);
    }

    private String value(
        HttpServletRequest req,
        String name
    ) {

        return StringUtils.trimToEmpty(
            req.getParameter(name)
        );
    }

    private long parseId(String raw) {

        try {

            long id =
                Long.parseLong(raw);

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                "Identificador de odontólogo no válido.",
                ex
            );
        }
    }

    private boolean configureEncoding(
        HttpServletRequest req,
        HttpServletResponse res
    ) {

        try {

            req.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
            );

            return true;

        } catch (UnsupportedEncodingException ex) {

            AppLog.error(
                "No se pudo configurar UTF-8",
                ex
            );

            sendError(
                res,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "No se pudo procesar la solicitud."
            );

            return false;
        }
    }

    private String sqlErrorMessage(
        SQLException ex
    ) {

        if ("23000".equals(ex.getSQLState())) {

            return "No se pudo guardar: el correo o la colegiatura ya están registrados.";
        }

        String mysqlMessage =
            StringUtils.defaultString(
                ex.getMessage()
            ).toLowerCase(Locale.ROOT);

        if (mysqlMessage.contains("denied")) {

            return "MySQL rechazó los permisos necesarios para esta operación.";
        }

        return "No se pudo guardar la información. Revisa el registro de errores del sistema.";
    }

    private void redirect(
        HttpServletRequest req,
        HttpServletResponse res,
        String path
    ) {

        if (res.isCommitted()) {
            return;
        }

        try {

            res.sendRedirect(
                req.getContextPath() + path
            );

        } catch (IOException ex) {

            AppLog.error(
                "No se pudo realizar la redirección a " + path,
                ex
            );
        }
    }

    private void sendError(
        HttpServletResponse res,
        int status,
        String message
    ) {

        if (res.isCommitted()) {
            return;
        }

        try {

            res.sendError(
                status,
                message
            );

        } catch (IOException ex) {

            AppLog.error(
                "No se pudo enviar la respuesta de error HTTP " + status,
                ex
            );
        }
    }
}