package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.ClinicaAtencionDAO;
import pe.odontosync.model.*;

class AtencionClinicaServiceTest {
    @Test
    void odontologoRegistraAtencionDeSuCitaConfirmada() throws Exception {
        FakeDao dao = new FakeDao();
        dao.appointment = sample("CONFIRMADA", Optional.empty());
        AtencionClinicaService service = new AtencionClinicaService(dao);
        long id = service.save(dentist(), 50, "Control", "Caries dental", "Restauración", "", "Control en 6 meses");
        assertEquals(91L, id);
        assertEquals("Caries dental", dao.diagnosis);
        assertEquals(44L, dao.dentistUserId);
    }

    @Test
    void exigeDiagnosticoYTratamiento() {
        FakeDao dao = new FakeDao();
        dao.appointment = sample("CONFIRMADA", Optional.empty());
        AtencionClinicaService service = new AtencionClinicaService(dao);
        assertThrows(IllegalArgumentException.class, () ->
            service.save(dentist(), 50, "Control", "", "Restauración", "", ""));
    }

    @Test
    void recepcionNoPuedeRegistrarInformacionClinica() {
        AtencionClinicaService service = new AtencionClinicaService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () -> service.load(reception(), 50));
    }

    @Test
    void citaPendienteNoPuedeAtenderse() {
        FakeDao dao = new FakeDao();
        dao.appointment = sample("PENDIENTE", Optional.empty());
        AtencionClinicaService service = new AtencionClinicaService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.load(dentist(), 50));
    }

    private static User dentist() { return new User(44,1,"Dentista","Clínica",Role.ODONTOLOGO); }
    private static User reception() { return new User(3,1,"Recepción","Clínica",Role.RECEPCIONISTA); }

    private static AtencionCitaDetalle sample(String status, Optional<ConsultaClinicaDetalle> consultation) {
        return new AtencionCitaDetalle(
            50,10,"Ana García","DNI 70123456",20,"Carlos Ramírez","Odontología General",status,
            LocalDate.now(),LocalTime.of(9,0),LocalTime.of(9,45),"Control dental","",consultation
        );
    }

    private static final class FakeDao implements ClinicaAtencionDAO {
        AtencionCitaDetalle appointment;
        String diagnosis;
        long dentistUserId;
        public Optional<AtencionCitaDetalle> findAppointmentForDentist(long c,long u,long a) {
            dentistUserId=u; return Optional.ofNullable(appointment);
        }
        public long saveAttention(long c,long u,long a,String m,String d,String t,String o,String r) {
            dentistUserId=u; diagnosis=d; return 91;
        }
        public List<HistoriaPacienteResumen> searchHistories(long c,long u,String q) { return List.of(); }
        public Optional<HistoriaPacienteVista> loadHistory(long c,long u,long p) { return Optional.empty(); }
        public void updateGeneralNotes(long c,long u,long p,String n) { }
        public long addMedicalHistory(long c,long u,long p,String t,String d,String o) { return 1; }
        public long addAllergy(long c,long u,long p,String a,String r,String o) { return 1; }
    }
}
