package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.JdbcUsuarioDAO;
import pe.odontosync.model.UsuarioDetalle;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.UsuarioCrudService;

/** CRUD administrativo de cuentas de OdontoSync. */
@WebServlet("/usuarios")
public final class UsuarioServlet extends HttpServlet {
    private final UsuarioCrudService service = new UsuarioCrudService(new JdbcUsuarioDAO());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentAdmin(req, res);
        if (current == null) return;

        try {
            req.setAttribute("user", current);
            req.setAttribute("active", "usuarios");
            req.setAttribute("usuarios", service.list(current.clinicId()));

            String edit = req.getParameter("editar");
            if (edit != null && !edit.isBlank()) {
                long id = parseId(edit);
                UsuarioDetalle selected = service.find(current.clinicId(), id)
                    .orElseThrow(() -> new IllegalArgumentException("El usuario solicitado no existe."));
                req.setAttribute("editUser", selected);
            }

            HttpSession session = req.getSession(false);
            if (session != null) {
                req.setAttribute("success", session.getAttribute("crudSuccess"));
                req.setAttribute("error", session.getAttribute("crudError"));
                session.removeAttribute("crudSuccess");
                session.removeAttribute("crudError");
            }
            req.getRequestDispatcher("/WEB-INF/views/usuarios.jsp").forward(req, res);
        } catch (IllegalArgumentException ex) {
            flash(req, "crudError", ex.getMessage());
            res.sendRedirect(req.getContextPath() + "/usuarios");
        } catch (SQLException ex) {
            AppLog.error("Fallo consultando CRUD de usuarios", ex);
            res.sendError(503, "No se pudo consultar MySQL.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentAdmin(req, res);
        if (current == null) return;
        req.setCharacterEncoding("UTF-8");

        if (!validCsrf(req)) {
            res.sendError(403, "Solicitud rechazada.");
            return;
        }

        String action = value(req, "accion");
        char[] password = value(req, "clave").toCharArray();
        try {
            switch (action) {
                case "crear" -> {
                    long id = service.create(
                        current.clinicId(), value(req, "nombres"), value(req, "apellidos"),
                        value(req, "correo"), value(req, "telefono"), value(req, "rol"),
                        value(req, "estado"), password, value(req, "colegiatura")
                    );
                    AuditLog.businessChange(current, "CREATE", "USUARIO", id);
                    flash(req, "crudSuccess", "Usuario creado correctamente. Ya puede iniciar sesión si su estado es ACTIVO.");
                }
                case "actualizar" -> {
                    long id = parseId(value(req, "id_usuario"));
                    service.update(
                        current.clinicId(), current.id(), id,
                        value(req, "nombres"), value(req, "apellidos"), value(req, "correo"),
                        value(req, "telefono"), value(req, "rol"), value(req, "estado"),
                        password, value(req, "colegiatura")
                    );
                    AuditLog.businessChange(current, "UPDATE", "USUARIO", id);
                    flash(req, "crudSuccess", "Usuario actualizado correctamente.");
                }
                case "eliminar" -> {
                    long id = parseId(value(req, "id_usuario"));
                    service.delete(current.clinicId(), current.id(), id);
                    AuditLog.businessChange(current, "DELETE", "USUARIO", id);
                    flash(req, "crudSuccess", "Cuenta de usuario eliminada correctamente.");
                }
                default -> throw new IllegalArgumentException("Acción no válida.");
            }
        } catch (IllegalArgumentException ex) {
            flash(req, "crudError", ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo guardando CRUD de usuarios", ex);
            String message = "23000".equals(ex.getSQLState())
                ? "No se pudo guardar: el correo o la colegiatura ya están registrados."
                : "No se pudo guardar en MySQL. Revisa los permisos del usuario odontosync_app.";
            flash(req, "crudError", message);
        } finally {
            Arrays.fill(password, '\0');
        }
        res.sendRedirect(req.getContextPath() + "/usuarios");
    }

    private User currentAdmin(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        if (user.role() != Role.ADMINISTRADOR) {
            res.sendError(403, "Solo el administrador puede gestionar usuarios.");
            return null;
        }
        return user;
    }

    private boolean validCsrf(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute("csrf");
        String supplied = req.getParameter("csrf");
        return expected != null && expected.equals(supplied);
    }

    private void flash(HttpServletRequest req, String key, String message) {
        req.getSession().setAttribute(key, message);
    }

    private String value(HttpServletRequest req, String name) {
        return StringUtils.trimToEmpty(req.getParameter(name));
    }

    private long parseId(String raw) {
        try {
            long id = Long.parseLong(raw);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identificador de usuario no válido.");
        }
    }
}
