package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import pe.odontosync.User;
import pe.odontosync.security.AppLog;
import pe.odontosync.service.ModuleService;

/** Controlador de navegación de módulos. No contiene SQL. */
@WebServlet("/modulo/*")
public final class ModuleServlet extends HttpServlet {
    private final ModuleService moduleService = ModuleService.production();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User user = (User) req.getSession(false).getAttribute("user");
        String path = req.getPathInfo();
        String key = path == null || path.length() <= 1 ? "" : path.substring(1);
        if (!moduleService.supports(key)) {
            res.sendError(404, "Módulo desconocido.");
            return;
        }
        if ("usuarios".equals(key)) {
            res.sendRedirect(req.getContextPath() + "/usuarios");
            return;
        }
        if ("pacientes".equals(key)) {
            res.sendRedirect(req.getContextPath() + "/pacientes");
            return;
        }
        if ("odontologos".equals(key)) {
            res.sendRedirect(req.getContextPath() + "/odontologos");
            return;
        }
        if ("horarios".equals(key)) {
            res.sendRedirect(req.getContextPath() + "/horarios");
            return;
        }
        if ("agenda".equals(key)) {
            res.sendRedirect(req.getContextPath() + "/agenda");
            return;
        }
        if ("historias".equals(key)) {
            res.sendRedirect(req.getContextPath() + "/historias");
            return;
        }
        if (!user.role().allows(key)) {
            res.sendError(403, "No tienes permiso para acceder a esta sección.");
            return;
        }
        try {
            req.setAttribute("user", user);
            req.setAttribute("active", key);
            req.setAttribute("table", moduleService.load(user, key));
            req.getRequestDispatcher("/WEB-INF/views/modulo.jsp").forward(req, res);
        } catch (Exception ex) {
            AppLog.error("Fallo en módulo de OdontoSync", ex);
            res.sendError(503, "No se pudo consultar MySQL.");
        }
    }
}
