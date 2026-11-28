package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.UUID;
import pe.odontosync.User;
import pe.odontosync.service.CreatedAccount;

/** Entrega la credencial solo en la respuesta del POST, sin persistirla en sesión. */
final class AccountCreationView {
    private AccountCreationView() { }

    static void prepare(HttpServletRequest req, String module) {
        String token = UUID.randomUUID().toString();
        req.getSession().setAttribute("altaNonce:" + module, token);
        req.setAttribute("altaNonce", token);
    }

    static void consume(HttpServletRequest req, String module) {
        HttpSession session = req.getSession(false);
        if (session == null) throw new IllegalArgumentException("La sesión ha expirado.");
        synchronized (session) {
            String key = "altaNonce:" + module;
            Object expected = session.getAttribute(key);
            if (!(expected instanceof String token) || !token.equals(req.getParameter("alta_nonce"))) {
                throw new IllegalArgumentException("El formulario ya fue utilizado o caducó. Abre uno nuevo.");
            }
            session.removeAttribute(key);
        }
    }

    static void show(HttpServletRequest req, HttpServletResponse res, User user, String module,
        CreatedAccount created) throws ServletException, IOException {
        res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        res.setHeader("Pragma", "no-cache");
        req.setAttribute("user", user);
        req.setAttribute("active", module);
        req.setAttribute("createdAccount", created);
        try {
            req.getRequestDispatcher("/WEB-INF/views/credenciales-alta.jsp").forward(req, res);
        } finally {
            req.removeAttribute("createdAccount");
        }
    }
}
