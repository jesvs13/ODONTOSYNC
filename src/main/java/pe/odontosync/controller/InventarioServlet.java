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
import pe.odontosync.User;
import pe.odontosync.dao.JdbcInventarioDAO;
import pe.odontosync.security.AppLog;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.InventarioService;

/** Gestión de insumos, stock y movimientos de inventario. */
@WebServlet("/modulo/inventario")
public final class InventarioServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String SUCCESS_KEY = "inventorySuccess";
    private static final String ERROR_KEY = "inventoryError";

    private transient InventarioService service;

    @Override
    public void init() {
        service = new InventarioService(new JdbcInventarioDAO());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User current = currentUser(req, res);
        if (current == null) return;

        try {
            req.setAttribute("user", current);
            req.setAttribute("active", "inventario");
            req.setAttribute("canManageInventory", service.canManage(current.role()));
            req.setAttribute("inventoryItems", service.list(current));
            req.setAttribute("inventoryCategories", service.categories(current));
            req.setAttribute("inventoryHistory", service.history(current));

            long editId = optionalId(req.getParameter("editar"));
            if (editId > 0) {
                req.setAttribute("editInventoryItem", service.detail(current, editId));
            }

            long movementId = optionalId(req.getParameter("movimiento"));
            if (movementId > 0) {
                req.setAttribute("movementInventoryItem", service.detail(current, movementId));
            }

            loadFlashMessages(req);
            req.getRequestDispatcher("/WEB-INF/views/inventario.jsp").forward(req, res);
        } catch (IllegalArgumentException ex) {
            flash(req, ERROR_KEY, ex.getMessage());
            res.sendRedirect(req.getContextPath() + "/panel");
        } catch (SQLException ex) {
            AppLog.error("Fallo consultando inventario", ex);
            res.sendError(503, "No se pudo consultar la información de inventario.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User current = currentUser(req, res);
        if (current == null) return;

        req.setCharacterEncoding("UTF-8");
        if (!validCsrf(req)) {
            res.sendError(403, "Solicitud rechazada.");
            return;
        }
        if (!service.canManage(current.role())) {
            res.sendError(403, "Tu perfil no puede gestionar Inventario.");
            return;
        }

        try {
            String action = value(req, "accion");
            switch (action) {
                case "crear" -> create(req, current);
                case "actualizar" -> update(req, current);
                case "estado" -> changeStatus(req, current);
                case "movimiento" -> registerMovement(req, current);
                default -> throw new IllegalArgumentException("Acción de inventario no válida.");
            }
        } catch (IllegalArgumentException ex) {
            flash(req, ERROR_KEY, ex.getMessage());
        } catch (SQLException ex) {
            AppLog.error("Fallo modificando inventario", ex);
            String state = ex.getSQLState();
            String message = state != null && state.startsWith("42")
                ? "MySQL rechazó la operación. Revisa los permisos del módulo Inventario."
                : "No se pudo guardar el cambio de inventario en MySQL.";
            flash(req, ERROR_KEY, message);
        }

        res.sendRedirect(req.getContextPath() + "/modulo/inventario");
    }

    private void create(HttpServletRequest req, User current) throws SQLException {
        long itemId = service.create(
            current,
            parseId(value(req, "id_categoria"), "Selecciona una categoría válida."),
            value(req, "nombre"),
            value(req, "descripcion"),
            value(req, "unidad_medida"),
            value(req, "stock_minimo"),
            value(req, "precio_referencial"),
            value(req, "estado")
        );
        AuditLog.businessChange(current, "CREATE", "INSUMO", itemId);
        flash(req, SUCCESS_KEY, "Insumo registrado correctamente. Registra una entrada o ajuste para establecer su stock.");
    }

    private void update(HttpServletRequest req, User current) throws SQLException {
        long itemId = parseId(value(req, "id_insumo"), "Selecciona un insumo válido.");
        service.update(
            current,
            itemId,
            parseId(value(req, "id_categoria"), "Selecciona una categoría válida."),
            value(req, "nombre"),
            value(req, "descripcion"),
            value(req, "unidad_medida"),
            value(req, "stock_minimo"),
            value(req, "precio_referencial"),
            value(req, "estado")
        );
        AuditLog.businessChange(current, "UPDATE", "INSUMO", itemId);
        flash(req, SUCCESS_KEY, "Insumo actualizado correctamente.");
    }

    private void changeStatus(HttpServletRequest req, User current) throws SQLException {
        long itemId = parseId(value(req, "id_insumo"), "Selecciona un insumo válido.");
        service.changeStatus(current, itemId, value(req, "estado"));
        AuditLog.businessChange(current, "STATUS", "INSUMO", itemId);
        flash(req, SUCCESS_KEY, "Estado del insumo actualizado.");
    }

    private void registerMovement(HttpServletRequest req, User current) throws SQLException {
        long itemId = parseId(value(req, "id_insumo"), "Selecciona un insumo válido.");
        long movementId = service.registerMovement(
            current,
            itemId,
            value(req, "tipo"),
            value(req, "cantidad"),
            value(req, "motivo")
        );
        AuditLog.businessChange(current, "CREATE", "MOVIMIENTO_INVENTARIO", movementId);
        flash(req, SUCCESS_KEY, "Movimiento registrado y stock actualizado correctamente.");
    }

    private User currentUser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return null;
        }
        if (!user.role().allows("inventario")) {
            AuditLog.accessDenied(user, "/modulo/inventario");
            res.sendError(403, "No tienes permiso para acceder a Inventario.");
            return null;
        }
        return user;
    }

    private void loadFlashMessages(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;
        req.setAttribute("success", session.getAttribute(SUCCESS_KEY));
        req.setAttribute("error", session.getAttribute(ERROR_KEY));
        session.removeAttribute(SUCCESS_KEY);
        session.removeAttribute(ERROR_KEY);
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

    private long parseId(String raw, String message) {
        try {
            long id = Long.parseLong(raw);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message, ex);
        }
    }

    private long optionalId(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        try {
            long id = Long.parseLong(raw);
            return id > 0 ? id : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
