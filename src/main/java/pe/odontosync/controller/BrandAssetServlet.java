package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Sirve únicamente los activos oficiales de marca incluidos en el classpath.
 * Esto evita depender de rutas estáticas del contenedor para los logos e iconos.
 */
@WebServlet("/brand/*")
public final class BrandAssetServlet extends HttpServlet {
    private static final Map<String, String> CONTENT_TYPES = Map.of(
        "logo-login.svg", "image/svg+xml;charset=UTF-8",
        "logo-sidebar.svg", "image/svg+xml;charset=UTF-8",
        "favicon.svg", "image/svg+xml;charset=UTF-8",
        "icon-16.png", "image/png",
        "icon-32.png", "image/png",
        "icon-48.png", "image/png"
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        String fileName = pathInfo == null ? "" : pathInfo.replaceFirst("^/", "");
        String contentType = CONTENT_TYPES.get(fileName);
        if (contentType == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("brand/" + fileName)) {
            if (in == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            resp.setContentType(contentType);
            resp.setHeader("Cache-Control", "public, max-age=86400");
            in.transferTo(resp.getOutputStream());
        }
    }
}
