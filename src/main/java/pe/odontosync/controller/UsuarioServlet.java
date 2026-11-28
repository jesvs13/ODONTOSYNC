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
import pe.odontosync.security.SecurityHeaders;
import pe.odontosync.service.UsuarioCrudService;
import pe.odontosync.service.CreatedAccount;

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
            AccountCreationView.prepare(req, "usuarios");

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
            // Permitir solo la consulta de disponibilidad al mismo origen en esta vista.
            res.setHeader("Content-Security-Policy", SecurityHeaders.values().get("Content-Security-Policy")
                + "; connect-src 'self'");
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
        if ("sugerir-correo".equals(action)) {
            suggestEmail(req, res);
            return;
        }
        char[] password = value(req, "clave").toCharArray();
        try {
            switch (action) {
                case "crear" -> {
                    AccountCreationView.consume(req, "usuarios");
                    if (password.length != 0) throw new IllegalArgumentException("La contraseña inicial es automática.");
                    try (CreatedAccount created = service.create(
                        current.clinicId(), value(req, "nombres"), value(req, "primer_apellido"),
                        value(req, "segundo_apellido"), value(req, "telefono"), value(req, "rol"),
                        value(req, "estado"), value(req, "dni")
                    )) {
                        AuditLog.businessChange(current, "CREATE", "USUARIO", created.id());
                        AccountCreationView.show(req, res, current, "usuarios", created);
                    }
                    return;
                }
                case "actualizar" -> {
                    long id = parseId(value(req, "id_usuario"));
                    service.update(
                        current.clinicId(), current.id(), id,
                        value(req, "nombres"), value(req, "apellidos"),
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

    private void suggestEmail(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("text/plain; charset=UTF-8");
        try {
            res.getWriter().write(service.suggestEmail(value(req, "nombres"),
                value(req, "primer_apellido"), value(req, "segundo_apellido")));
        } catch (IllegalArgumentException ex) {
            res.setStatus(400);
            res.getWriter().write(ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo consultando disponibilidad de correo", ex);
            res.setStatus(503);
            res.getWriter().write("No se pudo consultar la disponibilidad del correo.");
        }
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
