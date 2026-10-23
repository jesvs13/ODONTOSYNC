package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.AgendaCrudDAO;
import pe.odontosync.model.CitaDetalle;
import pe.odontosync.model.CitaEstadoOpcion;
import pe.odontosync.model.CitaResumen;
import pe.odontosync.model.EspecialidadOpcion;
import pe.odontosync.model.OdontologoOpcion;
import pe.odontosync.model.PacienteOpcion;

class AgendaServiceTest {
    @Test
    void recepcionRegistraCitaPendienteEnHorarioDisponible() throws Exception {
        FakeDao dao = new FakeDao();
        AgendaService service = new AgendaService(dao);
        long id = service.create(
            reception(), 10, 20, "1", LocalDate.now().plusDays(1).toString(),
            "09:00", "09:45", "Consulta general", "Primera evaluación"
        );
        assertEquals(90L, id);
        assertEquals(1L, dao.createdStateId);
        assertEquals(70L, dao.createdScheduleId);
        assertEquals("Consulta general", dao.createdReason);
    }

    @Test
    void impideHorarioOcupadoDelOdontologo() {
        FakeDao dao = new FakeDao();
        dao.dentistOverlap = true;
        AgendaService service = new AgendaService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.create(
            admin(), 10, 20, "1", LocalDate.now().plusDays(1).toString(),
            "09:00", "09:45", "Control dental", ""
        ));
    }

    @Test
    void impideCitaFueraDelHorarioDelOdontologo() {
        FakeDao dao = new FakeDao();
        dao.scheduleId = null;
        AgendaService service = new AgendaService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.create(
            admin(), 10, 20, "", LocalDate.now().plusDays(1).toString(),
            "20:00", "20:45", "Control dental", ""
        ));
    }

    @Test
    void odontologoSoloCargaSuAgenda() throws Exception {
        FakeDao dao = new FakeDao();
        AgendaService service = new AgendaService(dao);
        service.list(dentist(), "", "", "", "");
        assertEquals(44L, dao.loadedDentistUserId);
    }

    @Test
    void odontologoNoPuedeProgramarCitas() {
        AgendaService service = new AgendaService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () -> service.create(
            dentist(), 10, 20, "", LocalDate.now().plusDays(1).toString(),
            "09:00", "09:45", "Consulta", ""
        ));
    }

    @Test
    void recepcionConfirmaPeroNoMarcaAtendida() throws Exception {
        FakeDao dao = new FakeDao();
        dao.details.add(sample("PENDIENTE"));
        AgendaService service = new AgendaService(dao);
        service.changeStatus(reception(), 50, "CONFIRMADA");
        assertEquals(2L, dao.updatedStateId);

        dao.details.clear();
        dao.details.add(sample("CONFIRMADA"));
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(reception(), 50, "ATENDIDA"));
    }

    @Test
    void odontologoNoMarcaAtendidaSinRegistrarAtencionClinica() {
        FakeDao dao = new FakeDao();
        dao.details.add(sample("CONFIRMADA"));
        AgendaService service = new AgendaService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(dentist(), 50, "ATENDIDA"));
    }

    @Test
    void citaCerradaNoSeReprograma() {
        FakeDao dao = new FakeDao();
        dao.details.add(sample("ATENDIDA"));
        AgendaService service = new AgendaService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.update(
            admin(), 50, 10, 20, "1", LocalDate.now().plusDays(2).toString(),
            "10:00", "10:45", "Control", ""
        ));
    }

    private static User admin() { return new User(1,1,"Admin","Clínica",Role.ADMINISTRADOR); }
    private static User reception() { return new User(3,1,"Recepción","Clínica",Role.RECEPCIONISTA); }
    private static User dentist() { return new User(44,1,"Dentista","Clínica",Role.ODONTOLOGO); }

    private static CitaDetalle sample(String status) {
        long state = switch (status) {
            case "PENDIENTE" -> 1; case "CONFIRMADA" -> 2; case "ATENDIDA" -> 3;
            case "CANCELADA" -> 4; default -> 5;
        };
        return new CitaDetalle(
            50,10,"Ana García","DNI 70123456",20,"Carlos Ramírez",1L,"Odontología General",
            state,status,LocalDate.now().plusDays(1),LocalTime.of(9,0),LocalTime.of(9,45),"Consulta",""
        );
    }

    private static final class FakeDao implements AgendaCrudDAO {
        final List<CitaDetalle> details = new ArrayList<>();
        Long loadedDentistUserId;
        Long scheduleId = 70L;
        boolean blocked;
        boolean dentistOverlap;
        boolean patientOverlap;
        long createdStateId;
        Long createdScheduleId;
        String createdReason;
        long updatedStateId;

        public List<CitaResumen> findByClinic(long c) { return List.of(); }
        public List<CitaResumen> findByDentist(long c,long u) { return List.of(); }
        public List<CitaDetalle> searchDetailed(long c,Long u,LocalDate f,LocalDate t,String st,String q) {
            loadedDentistUserId=u; return details;
        }
        public Optional<CitaDetalle> findDetailedById(long c,long id) {
            return details.stream().filter(x -> x.idCita()==id).findFirst();
        }
        public List<PacienteOpcion> findActivePatients(long c) {
            return List.of(new PacienteOpcion(10,"Ana García","DNI 70123456","ACTIVO"));
        }
        public List<OdontologoOpcion> findActiveDentists(long c) {
            return List.of(new OdontologoOpcion(20,"Carlos Ramírez","ACTIVO"));
        }
        public List<EspecialidadOpcion> findActiveSpecialties() {
            return List.of(new EspecialidadOpcion(1,"Odontología General","ACTIVA"));
        }
        public Map<Long, List<Long>> findActiveDentistSpecialtyIds(long c) {
            return Map.of(20L, List.of(1L));
        }
        public List<CitaEstadoOpcion> findStates() {
            return List.of(
                new CitaEstadoOpcion(1,"PENDIENTE"),new CitaEstadoOpcion(2,"CONFIRMADA"),
                new CitaEstadoOpcion(3,"ATENDIDA"),new CitaEstadoOpcion(4,"CANCELADA"),
                new CitaEstadoOpcion(5,"NO_ASISTIO")
            );
        }
        public boolean patientIsActive(long c,long p) { return p==10; }
        public boolean dentistIsActive(long c,long d) { return d==20; }
        public boolean dentistBelongsToUser(long c,long d,long u) { return d==20 && u==44; }
        public boolean specialtyAssignedToDentist(long d,long s) { return d==20 && s==1; }
        public Optional<Long> findCoveringSchedule(long c,long d,int day,LocalTime s,LocalTime e) { return Optional.ofNullable(scheduleId); }
        public boolean isBlocked(long c,long d,LocalDate date,LocalTime s,LocalTime e) { return blocked; }
        public boolean dentistOverlaps(long c,long d,LocalDate date,LocalTime s,LocalTime e,Long x) { return dentistOverlap; }
        public boolean patientOverlaps(long c,long p,LocalDate date,LocalTime s,LocalTime e,Long x) { return patientOverlap; }
        public long create(long c,long p,long d,Long sp,Long sch,long state,LocalDate date,LocalTime s,LocalTime e,String r,String n,long by) {
            createdStateId=state; createdScheduleId=sch; createdReason=r; return 90;
        }
        public void update(long c,long id,long p,long d,Long sp,Long sch,LocalDate date,LocalTime s,LocalTime e,String r,String n) { }
        public void updateStatus(long c,long id,long state) { updatedStateId=state; }
    }
}
