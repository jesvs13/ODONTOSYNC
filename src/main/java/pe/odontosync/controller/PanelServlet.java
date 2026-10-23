package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import pe.odontosync.User;
import pe.odontosync.dao.JdbcDashboardDAO;
import pe.odontosync.security.AppLog;
import pe.odontosync.service.DashboardService;

/** Controlador del panel principal. */
@WebServlet("/panel")
public final class PanelServlet extends HttpServlet {
    private final DashboardService dashboardService = new DashboardService(new JdbcDashboardDAO());

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        User user = (User) req.getSession(false).getAttribute("user");
        try {
            req.setAttribute("user", user);
            req.setAttribute("active", "Inicio");
            req.setAttribute("cards", dashboardService.load(user));
            req.getRequestDispatcher("/WEB-INF/views/panel.jsp").forward(req, res);
        } catch (Exception ex) {
            AppLog.error("Fallo en panel de OdontoSync", ex);
            res.sendError(503, "No se pudo consultar MySQL.");
        }
    }
}
