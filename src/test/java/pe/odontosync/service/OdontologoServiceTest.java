package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.security.Passwords;
import pe.odontosync.dao.OdontologoCrudDAO;
import pe.odontosync.model.EspecialidadOpcion;
import pe.odontosync.model.OdontologoDetalle;

class OdontologoServiceTest {
    @Test
    void administradorCreaOdontologoConCuentaYEspecialidad() throws Exception {
        FakeDao dao = new FakeDao();
        OdontologoService service = new OdontologoService(dao);
        CreatedAccount created = service.create(
            admin(), " Carlos ", " Ramirez ", "Vega", "999111222",
            "01234567", "cop-12345", "2026-01-10", "ACTIVO", List.of(1L, 2L)
        );
        assertEquals(50L, created.id());
        assertEquals("Carlos", dao.createdNames);
        assertEquals("cramirez@odontosync.pe", dao.createdEmail);
        assertEquals("Ramirez Vega", dao.createdSurnames);
        assertEquals("COP-12345", dao.createdLicense);
        assertEquals(List.of(1L, 2L), dao.createdSpecialties);
        assertNotNull(dao.createdHash);
        assertEquals("01234567", dao.createdDni);
        assertEquals("cramirez@odontosync.pe", created.correo());
        assertTrue(Passwords.matches(created.temporaryPassword(), dao.createdHash));
        created.close();
    }

    @Test
    void recepcionSoloConsultaYNoPuedeCrear() {
        OdontologoService service = new OdontologoService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () -> service.create(
            reception(), "Carlos", "Ramirez", "Vega", "999111222", "01234567",
            "COP-1", "2026-01-10", "ACTIVO", List.of(1L)
        ));
    }

    @Test
    void exigeEspecialidadActiva() {
        FakeDao dao = new FakeDao();
        OdontologoService service = new OdontologoService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.create(
            admin(), "Carlos", "Ramirez", "Vega", "999111222", "01234567",
            "COP-2", "2026-01-10", "ACTIVO", List.of(99L)
        ));
    }

    @Test
    void bajaEsLogica() throws Exception {
        FakeDao dao = new FakeDao();
        dao.dentists.add(sample());
        OdontologoService service = new OdontologoService(dao);
        service.changeStatus(admin(), 10, "INACTIVO");
        assertEquals("INACTIVO", dao.newStatus);
    }

    @Test
    void correoOdontologoConsideraCoincidenciasDeTodasLasClinicas() throws Exception {
        FakeDao dao = new FakeDao();
        dao.occupied.add("cramirez@odontosync.pe");
        dao.occupied.add("cramirezv@odontosync.pe");
        new OdontologoService(dao).create(admin(), "Carlos", "Ramirez", "Vega", "999111222",
            "01234567", "COP-12345", "2026-01-10", "ACTIVO", List.of(1L));
        assertEquals("cramirezv2@odontosync.pe", dao.createdEmail);
        assertEquals(1L, dao.createdClinic);
    }

    @Test
    void reintentaAltaSiOtroUsuarioOcupaElCorreo() throws Exception {
        FakeDao dao = new FakeDao();
        dao.race = true;
        new OdontologoService(dao).create(admin(), "Carlos", "Ramirez", "Vega", "999111222",
            "01234567", "COP-12345", "2026-01-10", "ACTIVO", List.of(1L));
        assertEquals("cramirezv@odontosync.pe", dao.createdEmail);
        assertEquals(2, dao.createAttempts);
    }

    @Test
    void edicionProfesionalConservaCorreoDeAcceso() throws Exception {
        FakeDao dao = new FakeDao();
        dao.dentists.add(sample());
        new OdontologoService(dao).update(admin(), 10, "Carlos Alberto", "Ramirez", "alterado@x.pe",
            "999111222", new char[0], "COP-1", "2026-01-10", "ACTIVO", List.of(1L));
        assertEquals("carlos@x.pe", dao.updatedEmail);
        assertNull(dao.updatedHash);
    }

    @Test
    void cambioDePasswordNoSeRealizaDesdePerfilProfesional() {
        FakeDao dao = new FakeDao();
        dao.dentists.add(sample());
        assertThrows(IllegalArgumentException.class, () -> new OdontologoService(dao).update(
            admin(), 10, "Carlos", "Ramirez", "carlos@x.pe", "999111222", "OtraClave2026".toCharArray(),
            "COP-1", "2026-01-10", "ACTIVO", List.of(1L)));
        assertNull(dao.updatedEmail);
    }

    @Test
    void noCreaOdontologoConDniInvalido() {
        FakeDao dao = new FakeDao();
        assertThrows(IllegalArgumentException.class, () -> new OdontologoService(dao).create(
            admin(), "Carlos", "Ramirez", "Vega", "999111222", "123", "COP-2", "2026-01-10", "ACTIVO", List.of(1L)));
        assertEquals(0, dao.createAttempts);
    }

    @Test
    void altaConsultaSoloEspecialidadesActivasDeLaClinicaActual() throws Exception {
        FakeDao dao = new FakeDao();
        OdontologoService service = new OdontologoService(dao);
        User other = new User(2, 2, "Admin 2", "Otra clínica", Role.ADMINISTRADOR);
        assertEquals(List.of(1L, 2L), service.specialties(admin(), null).stream().map(EspecialidadOpcion::id).toList());
        assertEquals(List.of(1L, 3L), service.specialties(other, null).stream().map(EspecialidadOpcion::id).toList());
        assertEquals(2L, dao.catalogClinic);
        assertNull(dao.catalogDentist);
    }

    @Test
    void altaRechazaEspecialidadPropiaDeOtraClinica() {
        FakeDao dao = new FakeDao();
        assertThrows(IllegalArgumentException.class, () -> new OdontologoService(dao).create(
            admin(), "Carlos", "Ramirez", "Vega", "999111222", "01234567",
            "COP-2", "2026-01-10", "ACTIVO", List.of(3L)));
        assertEquals(0, dao.createAttempts);
    }

    @Test
    void edicionConservaEspecialidadInactivaYaAsignada() throws Exception {
        FakeDao dao = new FakeDao();
        dao.dentists.add(sample());
        dao.inactive = Set.of(1L);
        OdontologoService service = new OdontologoService(dao);
        assertEquals("INACTIVA", service.specialties(admin(), 10L).stream().filter(sp -> sp.id() == 1).findFirst().orElseThrow().estado());
        service.update(admin(), 10, "Carlos", "Ramirez", "carlos@x.pe", "999111222", null,
            "COP-1", "2026-01-10", "ACTIVO", List.of(1L));
        assertEquals(List.of(1L), dao.updatedSpecialties);
        assertEquals(Long.valueOf(10), dao.catalogDentist);
    }

    @Test
    void edicionRechazaNuevaAsignacionInactiva() {
        FakeDao dao = new FakeDao();
        dao.dentists.add(sample());
        dao.inactive = Set.of(2L);
        assertThrows(IllegalArgumentException.class, () -> new OdontologoService(dao).update(
            admin(), 10, "Carlos", "Ramirez", "carlos@x.pe", "999111222", null,
            "COP-1", "2026-01-10", "ACTIVO", List.of(1L, 2L)));
        assertNull(dao.updatedSpecialties);
    }

    @Test
    void edicionPermiteRetirarAsignacionInactivaVoluntariamente() throws Exception {
        FakeDao dao = new FakeDao();
        dao.dentists.add(sample());
        dao.inactive = Set.of(1L);
        new OdontologoService(dao).update(admin(), 10, "Carlos", "Ramirez", "carlos@x.pe",
            "999111222", null, "COP-1", "2026-01-10", "ACTIVO", List.of(2L));
        assertEquals(List.of(2L), dao.updatedSpecialties);
    }

    private static User admin() { return new User(1, 1, "Admin", "Clínica", Role.ADMINISTRADOR); }
    private static User reception() { return new User(3, 1, "Recepción", "Clínica", Role.RECEPCIONISTA); }

    private static OdontologoDetalle sample() {
        return new OdontologoDetalle(10, 20, "Carlos", "Ramirez", "COP-1", "999111222",
            "carlos@x.pe", LocalDate.of(2026,1,10), "ACTIVO", List.of(1L), List.of("Odontología General"));
    }

    private static final class FakeDao implements OdontologoCrudDAO {
        final List<OdontologoDetalle> dentists = new ArrayList<>();
        String createdNames, createdSurnames, createdEmail, createdLicense, createdHash, newStatus;
        String updatedEmail, updatedHash, createdDni;
        long createdClinic;
        int createAttempts;
        boolean race;
        final Set<String> occupied = new HashSet<>();
        List<Long> createdSpecialties, updatedSpecialties;
        long catalogClinic;
        Long catalogDentist;
        Set<Long> inactive = Set.of();

        public List<OdontologoDetalle> searchByClinic(long c, String q) { return dentists; }
        public Optional<OdontologoDetalle> findDetailedById(long c, long id) {
            return dentists.stream().filter(d -> d.idOdontologo() == id).findFirst();
        }
        public List<EspecialidadOpcion> findAvailableSpecialties(long clinicId, Long dentistId) {
            catalogClinic = clinicId;
            catalogDentist = dentistId;
            List<Long> assigned = dentistId == null || clinicId != 1 ? List.of()
                : dentists.stream().filter(d -> d.idOdontologo() == dentistId).findFirst()
                    .map(OdontologoDetalle::especialidadIds).orElse(List.of());
            List<EspecialidadOpcion> catalog = List.of(new EspecialidadOpcion(1,"Odontología General","ACTIVA"),
                new EspecialidadOpcion(clinicId == 1 ? 2 : 3, "Especialidad propia", "ACTIVA"));
            return catalog.stream().filter(sp -> !inactive.contains(sp.id()) || assigned.contains(sp.id()))
                .map(sp -> inactive.contains(sp.id()) ? new EspecialidadOpcion(sp.id(), sp.nombre(), "INACTIVA") : sp).toList();
        }
        public boolean emailExists(long c,String e,Long x) { return false; }
        // El contrato global se incorporará al DAO en GREEN.
        public boolean emailExistsGlobally(String e) { return occupied.contains(e); }
        public boolean licenseExists(long c,String e,Long x) { return false; }
        public long create(long c,String n,String a,String e,String t,String h,String co,LocalDate f,String st,List<Long> sp,String dni) throws SQLException {
            createAttempts++;
            if (race) { race = false; occupied.add(e); throw new SQLException("duplicate email", "23000", 1062); }
            createdDni=dni; createdClinic=c; createdSurnames=a; createdNames=n; createdEmail=e; createdLicense=co; createdHash=h; createdSpecialties=sp; return 50;
        }
        public void update(long c,long id,String n,String a,String e,String t,String h,String co,LocalDate f,String st,List<Long> sp) { updatedEmail=e; updatedHash=h; updatedSpecialties=sp; }
        public void updateStatus(long c,long id,String st) { newStatus=st; }
    }
}
