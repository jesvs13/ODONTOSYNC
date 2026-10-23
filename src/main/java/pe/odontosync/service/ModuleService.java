package pe.odontosync.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import pe.odontosync.Role;
import pe.odontosync.TableView;
import pe.odontosync.User;
import pe.odontosync.dao.*;
import pe.odontosync.model.*;

/**
 * Coordina la regla del rol, llama al DAO correspondiente y prepara datos para la vista.
 * No contiene SQL ni conoce detalles de JDBC.
 */
public final class ModuleService {
    private final PacienteDAO pacienteDAO;
    private final OdontologoDAO odontologoDAO;
    private final HorarioDAO horarioDAO;
    private final AgendaDAO agendaDAO;
    private final UsuarioDAO usuarioDAO;
    private final HistoriaDAO historiaDAO;
    private final RecetaDAO recetaDAO;
    private final PlacaDAO placaDAO;
    private final PagoDAO pagoDAO;
    private final InventarioDAO inventarioDAO;
    private final ReporteDAO reporteDAO;
    private final ClinicaDAO clinicaDAO;

    public ModuleService(
        PacienteDAO pacienteDAO, OdontologoDAO odontologoDAO, HorarioDAO horarioDAO,
        AgendaDAO agendaDAO, UsuarioDAO usuarioDAO, HistoriaDAO historiaDAO,
        RecetaDAO recetaDAO, PlacaDAO placaDAO, PagoDAO pagoDAO,
        InventarioDAO inventarioDAO, ReporteDAO reporteDAO, ClinicaDAO clinicaDAO
    ) {
        this.pacienteDAO = pacienteDAO;
        this.odontologoDAO = odontologoDAO;
        this.horarioDAO = horarioDAO;
        this.agendaDAO = agendaDAO;
        this.usuarioDAO = usuarioDAO;
        this.historiaDAO = historiaDAO;
        this.recetaDAO = recetaDAO;
        this.placaDAO = placaDAO;
        this.pagoDAO = pagoDAO;
        this.inventarioDAO = inventarioDAO;
        this.reporteDAO = reporteDAO;
        this.clinicaDAO = clinicaDAO;
    }

    /** Ensamblaje de producción. Se mantiene separado para poder inyectar fakes en las pruebas. */
    public static ModuleService production() {
        return new ModuleService(
            new JdbcPacienteDAO(), new JdbcOdontologoDAO(), new JdbcHorarioDAO(), new JdbcAgendaDAO(),
            new JdbcUsuarioDAO(), new JdbcHistoriaDAO(), new JdbcRecetaDAO(), new JdbcPlacaDAO(),
            new JdbcPagoDAO(), new JdbcInventarioDAO(), new JdbcReporteDAO(), new JdbcClinicaDAO()
        );
    }

    public boolean supports(String module) {
        return List.of("pacientes", "odontologos", "horarios", "agenda", "usuarios", "historias",
            "recetas", "placas", "pagos", "inventario", "reportes", "clinica").contains(module);
    }

    public TableView load(User user, String module) throws SQLException {
        if (!user.role().allows(module)) throw new IllegalArgumentException("Módulo no autorizado");
        return switch (module) {
            case "pacientes" -> patients(user);
            case "odontologos" -> dentists(user);
            case "horarios" -> schedules(user);
            case "agenda" -> agenda(user);
            case "usuarios" -> users(user);
            case "historias" -> histories(user);
            case "recetas" -> prescriptions(user);
            case "placas" -> xRays(user);
            case "pagos" -> payments(user);
            case "inventario" -> inventory(user);
            case "reportes" -> reports(user);
            case "clinica" -> clinic(user);
            default -> throw new IllegalArgumentException("Módulo desconocido");
        };
    }

    private TableView patients(User user) throws SQLException {
        List<PacienteResumen> items = user.role() == Role.ODONTOLOGO
            ? pacienteDAO.findAssignedToDentist(user.clinicId(), user.id())
            : pacienteDAO.findByClinic(user.clinicId());
        List<List<String>> rows = new ArrayList<>();
        for (PacienteResumen p : items) rows.add(List.of(safe(p.nombre()), safe(p.documento()), safe(p.telefono()), safe(p.estado())));
        return new TableView(user.role() == Role.ODONTOLOGO ? "Mis pacientes" : "Pacientes",
            user.role() == Role.ODONTOLOGO ? "Pacientes vinculados a tus citas" : "Registro de pacientes de tu clínica",
            List.of("Paciente", "Documento", "Teléfono", "Estado"), rows);
    }

    private TableView dentists(User user) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        for (OdontologoResumen d : odontologoDAO.findByClinic(user.clinicId())) rows.add(List.of(safe(d.nombre()), safe(d.colegiatura()), safe(d.correo()), safe(d.estado())));
        return new TableView("Odontólogos", "Profesionales de tu clínica", List.of("Odontólogo", "Colegiatura", "Correo", "Estado"), rows);
    }

    private TableView schedules(User user) throws SQLException {
        boolean dentist = user.role() == Role.ODONTOLOGO;
        List<HorarioResumen> items = dentist ? horarioDAO.findByDentist(user.clinicId(), user.id()) : horarioDAO.findByClinic(user.clinicId());
        List<List<String>> rows = new ArrayList<>();
        for (HorarioResumen h : items) rows.add(dentist
            ? List.of(safe(h.dia()), safe(h.horaInicio()), safe(h.horaFin()), safe(h.estado()))
            : List.of(safe(h.odontologo()), safe(h.dia()), safe(h.horaInicio()), safe(h.horaFin()), safe(h.estado())));
        return dentist
            ? new TableView("Mis horarios", "Disponibilidad registrada", List.of("Día", "Inicio", "Fin", "Estado"), rows)
            : new TableView("Horarios", "Disponibilidad de los odontólogos", List.of("Odontólogo", "Día", "Inicio", "Fin", "Estado"), rows);
    }

    private TableView agenda(User user) throws SQLException {
        boolean dentist = user.role() == Role.ODONTOLOGO;
        List<CitaResumen> items = dentist ? agendaDAO.findByDentist(user.clinicId(), user.id()) : agendaDAO.findByClinic(user.clinicId());
        List<List<String>> rows = new ArrayList<>();
        for (CitaResumen c : items) rows.add(dentist
            ? List.of(safe(c.fecha()), safe(c.horaInicio()), safe(c.paciente()), safe(c.estado()), safe(c.motivo()))
            : List.of(safe(c.fecha()), safe(c.horaInicio()), safe(c.paciente()), safe(c.odontologo()), safe(c.estado())));
        return dentist
            ? new TableView("Mis citas", "Agenda de tus pacientes", List.of("Fecha", "Hora", "Paciente", "Estado", "Motivo"), rows)
            : new TableView("Agenda", "Citas de la clínica", List.of("Fecha", "Hora", "Paciente", "Odontólogo", "Estado"), rows);
    }

    private TableView users(User user) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        for (UsuarioResumen x : usuarioDAO.findByClinic(user.clinicId())) rows.add(List.of(safe(x.nombre()), safe(x.correo()), safe(x.rol()), safe(x.estado())));
        return new TableView("Usuarios y roles", "Cuentas de tu clínica", List.of("Nombre", "Correo", "Rol", "Estado"), rows);
    }

    private TableView histories(User user) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        for (HistoriaResumen x : historiaDAO.findAssignedToDentist(user.clinicId(), user.id())) rows.add(List.of(safe(x.paciente()), safe(x.documento()), safe(x.apertura()), safe(x.estado())));
        return new TableView("Historia clínica", "Pacientes a tu cargo con historia registrada", List.of("Paciente", "Documento", "Apertura", "Estado"), rows);
    }

    private TableView prescriptions(User user) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        for (RecetaResumen x : recetaDAO.findByDentist(user.clinicId(), user.id())) rows.add(List.of(safe(x.fecha()), safe(x.paciente()), safe(x.medicamentos()), safe(x.indicaciones())));
        return new TableView("Recetas emitidas", "Prescripciones registradas por ti", List.of("Fecha", "Paciente", "Medicamentos", "Indicaciones"), rows);
    }

    private TableView xRays(User user) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        for (PlacaResumen x : placaDAO.findAssignedToDentist(user.clinicId(), user.id())) rows.add(List.of(safe(x.fecha()), safe(x.paciente()), safe(x.archivo())));
        return new TableView("Placas radiográficas", "Archivos registrados de pacientes a tu cargo", List.of("Fecha", "Paciente", "Archivo"), rows);
    }

    private TableView payments(User user) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        for (PagoResumen x : pagoDAO.findByClinic(user.clinicId())) rows.add(List.of(safe(x.fecha()), safe(x.paciente()), money(x.monto()), safe(x.metodo())));
        return new TableView("Pagos", "Cobros de citas de la clínica", List.of("Fecha", "Paciente", "Monto", "Método"), rows);
    }

    private TableView inventory(User user) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        for (InventarioResumen x : inventarioDAO.findByClinic(user.clinicId())) rows.add(List.of(safe(x.insumo()), safe(x.categoria()), safe(x.stock()), safe(x.minimo()), safe(x.alerta())));
        return new TableView("Inventario", "Existencias y alertas de stock", List.of("Insumo", "Categoría", "Stock", "Mínimo", "Alerta"), rows);
    }

    private TableView reports(User user) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        for (ReporteEstadoResumen x : reporteDAO.countAppointmentsByStatus(user.clinicId())) rows.add(List.of(safe(x.estado()), safe(x.citas())));
        return new TableView("Reportes", "Resumen por estado de cita", List.of("Estado", "Citas"), rows);
    }

    private TableView clinic(User user) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        clinicaDAO.findById(user.clinicId()).ifPresent(x -> rows.add(List.of(safe(x.nombre()), safe(x.ruc()), safe(x.direccion()), safe(x.telefono()), safe(x.estado()))));
        return new TableView("Datos clínica", "Información de tu centro", List.of("Clínica", "RUC", "Dirección", "Teléfono", "Estado"), rows);
    }

    private String safe(String value) { return value == null || value.isBlank() ? "—" : value; }
    private String money(String value) { return "S/ " + (value == null || value.isBlank() ? "0.00" : value); }
}
