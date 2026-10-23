package pe.odontosync.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.JdbcReporteDAO;
import pe.odontosync.security.AuditLog;
import pe.odontosync.service.ReporteExcelService;

/** Descarga el resumen de citas en Excel utilizando Apache POI. */
@WebServlet("/reportes/exportar-citas.xlsx")
public final class ReporteExcelServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ReporteExcelServlet.class);
    private final ReporteExcelService service = new ReporteExcelService(new JdbcReporteDAO());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User current = session == null ? null : (User) session.getAttribute("user");
        if (current == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        if (current.role() != Role.ADMINISTRADOR) {
            res.sendError(403, "Solo Administración puede exportar reportes.");
            return;
        }

        try {
            byte[] xlsx = service.exportAppointmentsByStatus(current);
            res.setStatus(HttpServletResponse.SC_OK);
            res.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            res.setHeader("Content-Disposition",
                "attachment; filename=odontosync_citas_" + LocalDate.now() + ".xlsx");
            res.setHeader("Cache-Control", "no-store");
            res.setContentLength(xlsx.length);

            // Commons IO se usa para transferir el binario sin duplicar lógica de streams.
            try (ByteArrayInputStream input = new ByteArrayInputStream(xlsx)) {
                IOUtils.copy(input, res.getOutputStream());
            }
            res.getOutputStream().flush();
            AuditLog.reportExported(current, "citas-por-estado.xlsx");
        } catch (IllegalArgumentException ex) {
            res.sendError(403, ex.getMessage());
        } catch (SQLException ex) {
            LOG.error("No se pudo consultar MySQL para exportar el reporte", ex);
            res.sendError(503, "No se pudo generar el reporte desde MySQL.");
        }
    }
}
