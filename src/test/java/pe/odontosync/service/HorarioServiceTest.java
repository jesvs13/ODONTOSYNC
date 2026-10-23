package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.HorarioCrudDAO;
import pe.odontosync.model.HorarioDetalle;
import pe.odontosync.model.OdontologoOpcion;

class HorarioServiceTest {
    @Test
    void administradorCreaBloqueValido() throws Exception {
        FakeDao dao = new FakeDao();
        HorarioService service = new HorarioService(dao);
        long id = service.create(admin(), 5, "1", "08:00", "12:00", "ACTIVO");
        assertEquals(80L, id);
        assertEquals(1, dao.createdDay);
        assertEquals(LocalTime.of(8,0), dao.createdStart);
    }

    @Test
    void evitaCruceDeHorarios() {
        FakeDao dao = new FakeDao(); dao.overlap = true;
        HorarioService service = new HorarioService(dao);
        assertThrows(IllegalArgumentException.class, () ->
            service.create(admin(), 5, "1", "10:00", "13:00", "ACTIVO"));
    }

    @Test
    void validaOrdenDeHoras() {
        HorarioService service = new HorarioService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () ->
            service.create(admin(), 5, "1", "12:00", "08:00", "ACTIVO"));
    }

    @Test
    void odontologoSoloVeSusHorarios() throws Exception {
        FakeDao dao = new FakeDao();
        HorarioService service = new HorarioService(dao);
        service.list(dentist());
        assertTrue(dao.loadedDentist);
        assertEquals(44L, dao.dentistUserId);
    }

    @Test
    void recepcionNoPuedeModificar() {
        HorarioService service = new HorarioService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () ->
            service.create(reception(), 5, "1", "08:00", "12:00", "ACTIVO"));
    }

    private static User admin() { return new User(1,1,"Admin","Clínica",Role.ADMINISTRADOR); }
    private static User reception() { return new User(3,1,"Recepción","Clínica",Role.RECEPCIONISTA); }
    private static User dentist() { return new User(44,1,"Dentista","Clínica",Role.ODONTOLOGO); }

    private static final class FakeDao implements HorarioCrudDAO {
        boolean overlap;
        boolean loadedDentist;
        long dentistUserId;
        int createdDay;
        LocalTime createdStart;
        List<HorarioDetalle> all = new ArrayList<>();

        public List<HorarioDetalle> findDetailedByClinic(long c) { return all; }
        public List<HorarioDetalle> findDetailedByDentistUser(long c,long u) { loadedDentist=true; dentistUserId=u; return all; }
        public Optional<HorarioDetalle> findDetailedById(long c,long id) { return all.stream().filter(h -> h.idHorario()==id).findFirst(); }
        public List<OdontologoOpcion> findDentistsByClinic(long c) { return List.of(new OdontologoOpcion(5,"Carlos R.","ACTIVO")); }
        public boolean dentistBelongsToClinic(long c,long d) { return d == 5; }
        public boolean dentistIsActive(long c,long d) { return d == 5; }
        public boolean overlaps(long c,long d,int day,LocalTime s,LocalTime e,Long x) { return overlap; }
        public long create(long c,long d,int day,LocalTime s,LocalTime e,String st) { createdDay=day; createdStart=s; return 80; }
        public void update(long c,long id,long d,int day,LocalTime s,LocalTime e,String st) { }
        public void updateStatus(long c,long id,String st) { }
    }
}
