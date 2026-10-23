package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.TableView;
import pe.odontosync.User;
import pe.odontosync.dao.*;
import pe.odontosync.model.*;

class ModuleServiceTest {
    @Test void administradorConsultaPacientesDeSuClinica() throws Exception {
        Fixture f = new Fixture(); User u = admin();
        TableView t = f.service().load(u, "pacientes");
        assertEquals("Pacientes", t.title()); assertEquals(7, f.pacientes.clinic); assertFalse(f.pacientes.assigned);
    }

    @Test void odontologoConsultaSoloPacientesAsignados() throws Exception {
        Fixture f = new Fixture(); TableView t = f.service().load(dentist(), "pacientes");
        assertEquals("Mis pacientes", t.title()); assertTrue(f.pacientes.assigned); assertEquals(44, f.pacientes.user);
    }

    @Test void recepcionPuedeConsultarOdontologosYHorarios() throws Exception {
        Fixture f = new Fixture(); User u = reception();
        assertEquals("Odontólogos", f.service().load(u, "odontologos").title());
        assertEquals("Horarios", f.service().load(u, "horarios").title());
    }

    @Test void odontologoConsultaAgendaHistoriaRecetasYPlacas() throws Exception {
        Fixture f = new Fixture(); User u = dentist();
        assertEquals("Mis citas", f.service().load(u, "agenda").title());
        assertEquals("Historia clínica", f.service().load(u, "historias").title());
        assertEquals("Recetas emitidas", f.service().load(u, "recetas").title());
        assertEquals("Placas radiográficas", f.service().load(u, "placas").title());
        assertEquals(44, f.historias.user); assertEquals(44, f.recetas.user); assertEquals(44, f.placas.user);
    }

    @Test void administradorConsultaUsuariosInventarioReportesYClinica() throws Exception {
        Fixture f = new Fixture(); User u = admin();
        assertEquals("Usuarios y roles", f.service().load(u, "usuarios").title());
        assertEquals("Inventario", f.service().load(u, "inventario").title());
        assertEquals("Reportes", f.service().load(u, "reportes").title());
        assertEquals("Datos clínica", f.service().load(u, "clinica").title());
        assertEquals(7, f.usuarios.clinic); assertEquals(7, f.inventario.clinic); assertEquals(7, f.reportes.clinic); assertEquals(7, f.clinica.clinic);
    }

    @Test void recepcionConsultaPagos() throws Exception {
        Fixture f = new Fixture(); TableView t = f.service().load(reception(), "pagos");
        assertEquals("Pagos", t.title()); assertEquals("S/ 125.50", t.rows().get(0).get(2)); assertEquals(5, f.pagos.clinic);
    }

    @Test void odontologoNoPuedeAbrirUsuariosNiPagos() {
        Fixture f = new Fixture(); User u = dentist();
        assertThrows(IllegalArgumentException.class, () -> f.service().load(u, "usuarios"));
        assertThrows(IllegalArgumentException.class, () -> f.service().load(u, "pagos"));
    }

    @Test void moduloDesconocidoSeRechaza() {
        Fixture f = new Fixture();
        assertFalse(f.service().supports("inexistente"));
        assertThrows(IllegalArgumentException.class, () -> f.service().load(admin(), "inexistente"));
    }

    private static User admin() { return new User(1, 7, "Admin", "Clínica", Role.ADMINISTRADOR); }
    private static User reception() { return new User(2, 5, "Recepción", "Clínica", Role.RECEPCIONISTA); }
    private static User dentist() { return new User(44, 3, "Carlos", "Clínica", Role.ODONTOLOGO); }

    private static final class Fixture {
        final FPaciente pacientes = new FPaciente(); final FOdontologo odontologos = new FOdontologo();
        final FHorario horarios = new FHorario(); final FAgenda agenda = new FAgenda();
        final FUsuario usuarios = new FUsuario(); final FHistoria historias = new FHistoria();
        final FReceta recetas = new FReceta(); final FPlaca placas = new FPlaca(); final FPago pagos = new FPago();
        final FInventario inventario = new FInventario(); final FReporte reportes = new FReporte(); final FClinica clinica = new FClinica();
        ModuleService service() { return new ModuleService(pacientes, odontologos, horarios, agenda, usuarios, historias, recetas, placas, pagos, inventario, reportes, clinica); }
    }

    private static final class FPaciente implements PacienteDAO {
        long clinic,user; boolean assigned;
        public List<PacienteResumen> findByClinic(long c){clinic=c; return List.of(new PacienteResumen("Ana","1","999","ACTIVO"));}
        public List<PacienteResumen> findAssignedToDentist(long c,long u){clinic=c;user=u;assigned=true;return List.of(new PacienteResumen("Luis","2",null,"ACTIVO"));}
    }
    private static final class FOdontologo implements OdontologoDAO { public List<OdontologoResumen> findByClinic(long c){return List.of(new OdontologoResumen("Carla","COP-1","c@o.pe","ACTIVO"));} }
    private static final class FHorario implements HorarioDAO {
        public List<HorarioResumen> findByClinic(long c){return List.of(new HorarioResumen("Carla","1","08:00","12:00","ACTIVO"));}
        public List<HorarioResumen> findByDentist(long c,long u){return List.of(new HorarioResumen("Carlos","1","09:00","13:00","ACTIVO"));}
    }
    private static final class FAgenda implements AgendaDAO {
        public List<CitaResumen> findByClinic(long c){return List.of(new CitaResumen("2026-10-10","10:00","Ana","Carla","CONFIRMADA","Control"));}
        public List<CitaResumen> findByDentist(long c,long u){return List.of(new CitaResumen("2026-10-10","10:00","Ana","Carlos","CONFIRMADA","Control"));}
    }
    private static final class FUsuario implements UsuarioDAO {
        long clinic;
        public List<LoginAccount> findLoginAccountsByEmail(String e){return List.of();} public void updateLastAccess(long id){}
        public List<UsuarioResumen> findByClinic(long c){clinic=c;return List.of(new UsuarioResumen("Admin","a@o.pe","ADMINISTRADOR","ACTIVO"));}
    }
    private static final class FHistoria implements HistoriaDAO { long user; public List<HistoriaResumen> findAssignedToDentist(long c,long u){user=u;return List.of(new HistoriaResumen("Ana","1","2026-01-01","ACTIVA"));} }
    private static final class FReceta implements RecetaDAO { long user; public List<RecetaResumen> findByDentist(long c,long u){user=u;return List.of(new RecetaResumen("2026-10-09","Ana","Rx","Cada 8h"));} }
    private static final class FPlaca implements PlacaDAO { long user; public List<PlacaResumen> findAssignedToDentist(long c,long u){user=u;return List.of(new PlacaResumen("2026-10-09","Ana","placa.png"));} }
    private static final class FPago implements PagoDAO { long clinic; public List<PagoResumen> findByClinic(long c){clinic=c;return List.of(new PagoResumen("2026-10-09","Ana","125.50","YAPE"));} }
    private static final class FInventario implements InventarioDAO { long clinic; public List<InventarioResumen> findByClinic(long c){clinic=c;return List.of(new InventarioResumen("Guantes","Descartables","10","5","Normal"));} }
    private static final class FReporte implements ReporteDAO { long clinic; public List<ReporteEstadoResumen> countAppointmentsByStatus(long c){clinic=c;return List.of(new ReporteEstadoResumen("ATENDIDA","3"));} }
    private static final class FClinica implements ClinicaDAO { long clinic; public Optional<ClinicaResumen> findById(long c){clinic=c;return Optional.of(new ClinicaResumen("OdontoSync","20123456789","Lima","999","ACTIVA"));} }
}
