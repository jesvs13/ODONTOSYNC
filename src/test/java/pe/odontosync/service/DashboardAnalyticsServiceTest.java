package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.DashboardAnalyticsDAO;
import pe.odontosync.model.DashboardAnalyticsSnapshot;
import pe.odontosync.model.DashboardCategory;
import pe.odontosync.model.DashboardPoint;
import pe.odontosync.model.DashboardSummary;

class DashboardAnalyticsServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 30);

    @Test
    void administradorConsultaSoloLaClinicaDeSuSesion() throws Exception {
        FakeDashboardAnalyticsDAO dao = new FakeDashboardAnalyticsDAO();
        DashboardAnalyticsService service = new DashboardAnalyticsService(dao);
        User admin = new User(
            10L,
            27L,
            "Administrador",
            "Clínica A",
            Role.ADMINISTRADOR
        );

        DashboardAnalyticsSnapshot dashboard =
            service.load(admin, FROM, TO, null);

        assertEquals(27L, dao.lastClinicId);
        assertEquals(FROM, dao.lastFrom);
        assertEquals(TO, dao.lastTo);
        assertNull(dao.lastDentistUserId);

        assertEquals(120, dashboard.summary().patients());
        assertEquals(84, dashboard.summary().appointments());
        assertEquals(new BigDecimal("8450.00"), dashboard.summary().income());
        assertEquals(91.0, dashboard.summary().attendanceRate());
    }

    @Test
    void filtroDeOdontologoSeTransmiteSinCambiarLaClinicaAutenticada()
        throws Exception {

        FakeDashboardAnalyticsDAO dao = new FakeDashboardAnalyticsDAO();
        DashboardAnalyticsService service = new DashboardAnalyticsService(dao);
        User admin = new User(
            25L,
            8L,
            "Administrador",
            "Clínica B",
            Role.ADMINISTRADOR
        );

        service.load(admin, FROM, TO, 44L);

        assertEquals(8L, dao.lastClinicId);
        assertEquals(44L, dao.lastDentistUserId);
    }

    @Test
    void odontologoNoPuedeConsultarDashboardGlobal() {
        FakeDashboardAnalyticsDAO dao = new FakeDashboardAnalyticsDAO();
        DashboardAnalyticsService service = new DashboardAnalyticsService(dao);
        User dentist = new User(
            44L,
            8L,
            "Odontólogo",
            "Clínica B",
            Role.ODONTOLOGO
        );

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> service.load(dentist, FROM, TO, null)
        );

        assertTrue(error.getMessage().toLowerCase().contains("administrador"));
        assertEquals(0L, dao.lastClinicId);
    }

    @Test
    void recepcionistaNoPuedeConsultarDashboardGlobal() {
        FakeDashboardAnalyticsDAO dao = new FakeDashboardAnalyticsDAO();
        DashboardAnalyticsService service = new DashboardAnalyticsService(dao);
        User receptionist = new User(
            33L,
            8L,
            "Recepción",
            "Clínica B",
            Role.RECEPCIONISTA
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.load(receptionist, FROM, TO, null)
        );

        assertEquals(0L, dao.lastClinicId);
    }

    @Test
    void rechazaRangoDeFechasInvalidoAntesDeConsultarElDao() {
        FakeDashboardAnalyticsDAO dao = new FakeDashboardAnalyticsDAO();
        DashboardAnalyticsService service = new DashboardAnalyticsService(dao);
        User admin = new User(
            10L,
            27L,
            "Administrador",
            "Clínica A",
            Role.ADMINISTRADOR
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.load(
                admin,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 11, 1),
                null
            )
        );

        assertEquals(0L, dao.lastClinicId);
    }

    @Test
    void cargaTodasLasSeriesNecesariasParaGraficos() throws Exception {
        FakeDashboardAnalyticsDAO dao = new FakeDashboardAnalyticsDAO();
        DashboardAnalyticsService service = new DashboardAnalyticsService(dao);
        User admin = new User(
            10L,
            27L,
            "Administrador",
            "Clínica A",
            Role.ADMINISTRADOR
        );

        DashboardAnalyticsSnapshot dashboard =
            service.load(admin, FROM, TO, null);

        assertFalse(dashboard.appointmentsTrend().isEmpty());
        assertFalse(dashboard.appointmentStatus().isEmpty());
        assertFalse(dashboard.incomeTrend().isEmpty());
        assertFalse(dashboard.newPatientsTrend().isEmpty());
        assertFalse(dashboard.attentionsByDentist().isEmpty());
        assertFalse(dashboard.inventoryStatus().isEmpty());
        assertFalse(dashboard.paymentMethods().isEmpty());
    }

    private static final class FakeDashboardAnalyticsDAO
        implements DashboardAnalyticsDAO {

        long lastClinicId;
        LocalDate lastFrom;
        LocalDate lastTo;
        Long lastDentistUserId;

        @Override
        public DashboardSummary summary(
            long clinicId,
            LocalDate from,
            LocalDate to,
            Long dentistUserId
        ) throws SQLException {
            capture(clinicId, from, to, dentistUserId);
            return new DashboardSummary(
                120,
                84,
                new BigDecimal("8450.00"),
                91.0
            );
        }

        @Override
        public List<DashboardPoint> appointmentsTrend(
            long clinicId,
            LocalDate from,
            LocalDate to,
            Long dentistUserId
        ) throws SQLException {
            capture(clinicId, from, to, dentistUserId);
            return List.of(
                new DashboardPoint("01 Nov", new BigDecimal("8")),
                new DashboardPoint("02 Nov", new BigDecimal("11"))
            );
        }

        @Override
        public List<DashboardCategory> appointmentStatus(
            long clinicId,
            LocalDate from,
            LocalDate to,
            Long dentistUserId
        ) throws SQLException {
            capture(clinicId, from, to, dentistUserId);
            return List.of(
                new DashboardCategory("Atendidas", new BigDecimal("60")),
                new DashboardCategory("Pendientes", new BigDecimal("24"))
            );
        }

        @Override
        public List<DashboardPoint> incomeTrend(
            long clinicId,
            LocalDate from,
            LocalDate to,
            Long dentistUserId
        ) throws SQLException {
            capture(clinicId, from, to, dentistUserId);
            return List.of(
                new DashboardPoint("Semana 1", new BigDecimal("2200.00"))
            );
        }

        @Override
        public List<DashboardPoint> newPatientsTrend(
            long clinicId,
            LocalDate from,
            LocalDate to
        ) throws SQLException {
            capture(clinicId, from, to, null);
            return List.of(
                new DashboardPoint("Semana 1", new BigDecimal("12"))
            );
        }

        @Override
        public List<DashboardCategory> attentionsByDentist(
            long clinicId,
            LocalDate from,
            LocalDate to
        ) throws SQLException {
            capture(clinicId, from, to, null);
            return List.of(
                new DashboardCategory("Dra. Ana", new BigDecimal("31"))
            );
        }

        @Override
        public List<DashboardCategory> inventoryStatus(long clinicId)
            throws SQLException {
            this.lastClinicId = clinicId;
            return List.of(
                new DashboardCategory("Normal", new BigDecimal("18")),
                new DashboardCategory("Stock bajo", new BigDecimal("2")),
                new DashboardCategory("Sin stock", BigDecimal.ONE)
            );
        }

        @Override
        public List<DashboardCategory> paymentMethods(
            long clinicId,
            LocalDate from,
            LocalDate to
        ) throws SQLException {
            capture(clinicId, from, to, null);
            return List.of(
                new DashboardCategory("Efectivo", new BigDecimal("4200.00")),
                new DashboardCategory("Tarjeta", new BigDecimal("4250.00"))
            );
        }

        private void capture(
            long clinicId,
            LocalDate from,
            LocalDate to,
            Long dentistUserId
        ) {
            this.lastClinicId = clinicId;
            this.lastFrom = from;
            this.lastTo = to;
            if (dentistUserId != null) {
                this.lastDentistUserId = dentistUserId;
            }
        }
    }
}
