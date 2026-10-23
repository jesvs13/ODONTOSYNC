package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.ClinicaAtencionDAO;
import pe.odontosync.model.*;

class HistoriaClinicaServiceTest {
    @Test
    void odontologoConsultaSoloSuHistoriaVisible() throws Exception {
        FakeDao dao = new FakeDao();
        HistoriaClinicaService service = new HistoriaClinicaService(dao);
        HistoriaPacienteVista view = service.load(dentist(), 10);
        assertEquals("Ana García", view.paciente().nombreCompleto());
        assertEquals(44L, dao.lastUser);
    }

    @Test
    void recepcionNoAccedeAHistoriaClinica() {
        HistoriaClinicaService service = new HistoriaClinicaService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () -> service.list(reception(), ""));
    }

    @Test
    void registraAntecedenteValidado() throws Exception {
        FakeDao dao = new FakeDao();
        HistoriaClinicaService service = new HistoriaClinicaService(dao);
        service.addMedicalHistory(dentist(),10,"Médico","Hipertensión controlada","Usa medicación");
        assertEquals("Hipertensión controlada", dao.description);
    }

    @Test
    void alergenoEsObligatorio() {
        HistoriaClinicaService service = new HistoriaClinicaService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () -> service.addAllergy(dentist(),10,"","", ""));
    }

    private static User dentist() { return new User(44,1,"Dentista","Clínica",Role.ODONTOLOGO); }
    private static User reception() { return new User(3,1,"Recepción","Clínica",Role.RECEPCIONISTA); }

    private static final class FakeDao implements ClinicaAtencionDAO {
        long lastUser;
        String description;
        public Optional<AtencionCitaDetalle> findAppointmentForDentist(long c,long u,long a) { return Optional.empty(); }
        public long saveAttention(long c,long u,long a,String m,String d,String t,String o,String r) { return 1; }
        public List<HistoriaPacienteResumen> searchHistories(long c,long u,String q) { lastUser=u; return List.of(); }
        public Optional<HistoriaPacienteVista> loadHistory(long c,long u,long p) {
            lastUser=u;
            PacienteDetalle patient = new PacienteDetalle(10,"DNI","70123456","Ana","García",LocalDate.of(1990,1,1),"F",null,null,null,null,null,null,"ACTIVO");
            return Optional.of(new HistoriaPacienteVista(patient,Optional.empty(),List.of(),List.of(),List.of()));
        }
        public void updateGeneralNotes(long c,long u,long p,String n) { lastUser=u; }
        public long addMedicalHistory(long c,long u,long p,String t,String d,String o) { lastUser=u; description=d; return 1; }
        public long addAllergy(long c,long u,long p,String a,String r,String o) { lastUser=u; return 1; }
    }
}
