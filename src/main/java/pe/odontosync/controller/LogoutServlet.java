package pe.odontosync.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/** Cierre de sesión protegido con token CSRF. */
@WebServlet("/logout")
public final class LogoutServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute("csrf");
        String supplied = req.getParameter("csrf");
        if (expected == null || !expected.equals(supplied)) {
            res.sendError(403, "Solicitud rechazada.");
            return;
        }
        session.invalidate();
        res.sendRedirect(req.getContextPath() + "/login");
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.sendError(405, "Usa el botón Salir.");
    }
}
