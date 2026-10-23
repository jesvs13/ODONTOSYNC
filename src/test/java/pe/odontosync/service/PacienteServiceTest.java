package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.PacienteCrudDAO;
import pe.odontosync.model.*;

class PacienteServiceTest {
    @Test
    void recepcionRegistraPacienteConDatosNormalizados() throws Exception {
        FakeDao dao = new FakeDao();
        PacienteService service = new PacienteService(dao);
        long id = service.create(
            reception(), "dni", "70123456", " Ana ", " García ", "2001-05-12", "F",
            "Jr. Uno 123", "999111222", "ANA@CORREO.PE", "Estudiante",
            "Rosa García", "999333444", "ACTIVO"
        );
        assertEquals(20L, id);
        assertEquals("DNI", dao.createdType);
        assertEquals("Ana", dao.createdNames);
        assertEquals("ana@correo.pe", dao.createdEmail);
    }

    @Test
    void dniDebeTenerOchoDigitos() {
        PacienteService service = new PacienteService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () -> service.create(
            reception(), "DNI", "123", "Ana", "García", "2001-05-12", "F",
            "", "", "", "", "", "", "ACTIVO"
        ));
    }

    @Test
    void noPermiteDocumentoDuplicado() {
        FakeDao dao = new FakeDao(); dao.duplicate = true;
        PacienteService service = new PacienteService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.create(
            admin(), "DNI", "70123456", "Ana", "García", "2001-05-12", "F",
            "", "", "", "", "", "", "ACTIVO"
        ));
    }

    @Test
    void odontologoSoloConsultaAsignadosYNoModifica() throws Exception {
        FakeDao dao = new FakeDao();
        PacienteService service = new PacienteService(dao);
        service.list(dentist(), "ana");
        assertTrue(dao.assignedSearch);
        assertEquals(44L, dao.dentistUser);
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(dentist(), 5, "INACTIVO"));
    }

    @Test
    void odontologoNoPuedeAbrirPacienteNoAsignado() {
        FakeDao dao = new FakeDao(); dao.assignedVisible = false;
        dao.patients.add(sample());
        PacienteService service = new PacienteService(dao);
        assertThrows(IllegalArgumentException.class, () -> service.ficha(dentist(), 5));
    }


    @Test
    void fichaRespetaPrivacidadPorRol() throws Exception {
        FakeDao dao = new FakeDao(); dao.patients.add(sample());
        PacienteService service = new PacienteService(dao);

        service.ficha(dentist(), 5);
        assertTrue(dao.lastClinical);
        assertFalse(dao.lastPayments);

        service.ficha(reception(), 5);
        assertFalse(dao.lastClinical);
        assertTrue(dao.lastPayments);
    }

    @Test
    void bajaEsLogicaYConservaPaciente() throws Exception {
        FakeDao dao = new FakeDao(); dao.patients.add(sample());
        PacienteService service = new PacienteService(dao);
        service.changeStatus(admin(), 5, "INACTIVO");
        assertEquals("INACTIVO", dao.newStatus);
        assertFalse(dao.deleted);
    }

    private static User admin() { return new User(1, 1, "Admin", "Clínica", Role.ADMINISTRADOR); }
    private static User reception() { return new User(3, 1, "Recepción", "Clínica", Role.RECEPCIONISTA); }
    private static User dentist() { return new User(44, 1, "Dentista", "Clínica", Role.ODONTOLOGO); }

    private static PacienteDetalle sample() {
        return new PacienteDetalle(
            5, "DNI", "70123456", "Ana", "García", LocalDate.of(2001,5,12), "F",
            null, "999111222", "ana@correo.pe", null, null, null, "ACTIVO"
        );
    }

    private static final class FakeDao implements PacienteCrudDAO {
        final List<PacienteDetalle> patients = new ArrayList<>();
        boolean duplicate;
        boolean assignedSearch;
        boolean assignedVisible = true;
        boolean deleted;
        long dentistUser;
        String createdType, createdNames, createdEmail, newStatus;
        boolean lastClinical, lastPayments;

        public List<PacienteDetalle> searchByClinic(long c, String q) { return patients; }
        public List<PacienteDetalle> searchAssignedToDentist(long c, long u, String q) {
            assignedSearch = true; dentistUser = u; return patients;
        }
        public Optional<PacienteDetalle> findDetailedById(long c, long id) {
            return patients.stream().filter(p -> p.id() == id).findFirst();
        }
        public boolean isAssignedToDentist(long c, long u, long id) { return assignedVisible; }
        public boolean documentExists(long c, String t, String n, Long e) { return duplicate; }
        public long create(long c,String t,String n,String no,String ap,LocalDate f,String s,String d,String te,String co,String o,String ce,String tee,String es) {
            createdType=t; createdNames=no; createdEmail=co; return 20;
        }
        public void update(long c,long id,String t,String n,String no,String ap,LocalDate f,String s,String d,String te,String co,String o,String ce,String tee,String es) { }
        public void updateStatus(long c,long id,String e) { newStatus=e; }
        public PacienteFicha loadFicha(long c,long id,boolean clinical,boolean payments) {
            lastClinical = clinical; lastPayments = payments;
            PacienteDetalle p = findDetailedById(c,id).orElseThrow();
            return new PacienteFicha(p, List.of(), Optional.empty(), List.of(), List.of(), List.of());
        }
    }
}
