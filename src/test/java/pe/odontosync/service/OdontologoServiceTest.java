package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.OdontologoCrudDAO;
import pe.odontosync.model.EspecialidadOpcion;
import pe.odontosync.model.OdontologoDetalle;

class OdontologoServiceTest {
    @Test
    void administradorCreaOdontologoConCuentaYEspecialidad() throws Exception {
        FakeDao dao = new FakeDao();
        OdontologoService service = new OdontologoService(dao);
        long id = service.create(
            admin(), " Carlos ", " Ramirez ", "CARLOS@CLINICA.PE", "999111222",
            "Clave2026".toCharArray(), "cop-12345", "2026-01-10", "ACTIVO", List.of(1L, 2L)
        );
        assertEquals(50L, id);
        assertEquals("Carlos", dao.createdNames);
        assertEquals("carlos@clinica.pe", dao.createdEmail);
        assertEquals("COP-12345", dao.createdLicense);
        assertEquals(List.of(1L, 2L), dao.createdSpecialties);
        assertNotNull(dao.createdHash);
        assertFalse(dao.createdHash.contains("Clave2026"));
    }

    @Test
    void recepcionSoloConsultaYNoPuedeCrear() {
        OdontologoService service = new OdontologoService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () -> service.create(
            reception(), "Carlos", "Ramirez", "c@x.pe", "999111222", "Clave2026".toCharArray(),
            "COP-1", "2026-01-10", "ACTIVO", List.of(1L)
        ));
    }

    @Test
    void exigeEspecialidadActiva() {
        FakeDao dao = new FakeDao();
        OdontologoService service = new OdontologoService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.create(
            admin(), "Carlos", "Ramirez", "carlos@x.pe", "999111222", "Clave2026".toCharArray(),
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

    private static User admin() { return new User(1, 1, "Admin", "Clínica", Role.ADMINISTRADOR); }
    private static User reception() { return new User(3, 1, "Recepción", "Clínica", Role.RECEPCIONISTA); }

    private static OdontologoDetalle sample() {
        return new OdontologoDetalle(10, 20, "Carlos", "Ramirez", "COP-1", "999111222",
            "carlos@x.pe", LocalDate.of(2026,1,10), "ACTIVO", List.of(1L), List.of("Odontología General"));
    }

    private static final class FakeDao implements OdontologoCrudDAO {
        final List<OdontologoDetalle> dentists = new ArrayList<>();
        String createdNames, createdEmail, createdLicense, createdHash, newStatus;
        List<Long> createdSpecialties;

        public List<OdontologoDetalle> searchByClinic(long c, String q) { return dentists; }
        public Optional<OdontologoDetalle> findDetailedById(long c, long id) {
            return dentists.stream().filter(d -> d.idOdontologo() == id).findFirst();
        }
        public List<EspecialidadOpcion> findActiveSpecialties() {
            return List.of(new EspecialidadOpcion(1,"Odontología General","ACTIVA"), new EspecialidadOpcion(2,"Ortodoncia","ACTIVA"));
        }
        public boolean emailExists(long c,String e,Long x) { return false; }
        public boolean licenseExists(long c,String e,Long x) { return false; }
        public long create(long c,String n,String a,String e,String t,String h,String co,LocalDate f,String st,List<Long> sp) {
            createdNames=n; createdEmail=e; createdLicense=co; createdHash=h; createdSpecialties=sp; return 50;
        }
        public void update(long c,long id,String n,String a,String e,String t,String h,String co,LocalDate f,String st,List<Long> sp) { }
        public void updateStatus(long c,long id,String st) { newStatus=st; }
    }
}
