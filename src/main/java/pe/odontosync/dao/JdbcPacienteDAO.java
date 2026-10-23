package pe.odontosync.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.model.PacienteCitaDetalle;
import pe.odontosync.model.PacienteDetalle;
import pe.odontosync.model.PacienteFicha;
import pe.odontosync.model.PacienteHistoriaDetalle;
import pe.odontosync.model.PacientePagoDetalle;
import pe.odontosync.model.PacientePlacaDetalle;
import pe.odontosync.model.PacienteRecetaDetalle;
import pe.odontosync.model.PacienteResumen;

/** Implementación JDBC del acceso a pacientes. Contiene únicamente persistencia. */
public final class JdbcPacienteDAO implements PacienteDAO, PacienteCrudDAO {
    private static final String FIND_BY_CLINIC = """
        SELECT CONCAT(nombres,' ',apellidos) AS paciente,
               numero_documento, telefono, estado
        FROM pacientes
        WHERE id_clinica = ?
        ORDER BY apellidos, nombres
        LIMIT 50
        """;

    private static final String FIND_ASSIGNED_TO_DENTIST = """
        SELECT DISTINCT CONCAT(p.nombres,' ',p.apellidos) AS paciente,
               p.numero_documento, p.telefono, p.estado
        FROM pacientes p
        JOIN citas c ON c.id_paciente = p.id_paciente
        JOIN odontologos o ON o.id_odontologo = c.id_odontologo
        WHERE p.id_clinica = ?
          AND c.id_clinica = ?
          AND o.id_clinica = ?
          AND o.id_usuario = ?
        ORDER BY paciente
        LIMIT 50
        """;

    private static final String DETAIL_COLUMNS = """
        p.id_paciente, p.tipo_documento, p.numero_documento, p.nombres, p.apellidos,
        p.fecha_nacimiento, p.sexo, p.direccion, p.telefono, p.correo, p.ocupacion,
        p.contacto_emergencia, p.telefono_emergencia, p.estado
        """;

    @Override
    public List<PacienteResumen> findByClinic(long clinicId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_BY_CLINIC)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            try (ResultSet rs = ps.executeQuery()) {
                return readSummary(rs);
            }
        }
    }

    @Override
    public List<PacienteResumen> findAssignedToDentist(long clinicId, long dentistUserId) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(FIND_ASSIGNED_TO_DENTIST)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            ps.setLong(2, clinicId);
            ps.setLong(3, clinicId);
            ps.setLong(4, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return readSummary(rs);
            }
        }
    }

    @Override
    public List<PacienteDetalle> searchByClinic(long clinicId, String query) throws SQLException {
        String sql = "SELECT " + DETAIL_COLUMNS + " FROM pacientes p "
            + "WHERE p.id_clinica=? AND (?='' OR p.numero_documento LIKE ? OR p.nombres LIKE ? "
            + "OR p.apellidos LIKE ? OR COALESCE(p.telefono,'') LIKE ? OR COALESCE(p.correo,'') LIKE ?) "
            + "ORDER BY p.estado='ACTIVO' DESC, p.apellidos, p.nombres LIMIT 100";
        String q = query == null ? "" : query.trim();
        String like = "%" + q + "%";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            ps.setString(2, q);
            for (int i = 3; i <= 7; i++) ps.setString(i, like);
            try (ResultSet rs = ps.executeQuery()) { return readDetails(rs); }
        }
    }

    @Override
    public List<PacienteDetalle> searchAssignedToDentist(long clinicId, long dentistUserId, String query) throws SQLException {
        String sql = "SELECT " + DETAIL_COLUMNS + " FROM pacientes p "
            + "WHERE p.id_clinica=? "
            + "AND EXISTS (SELECT 1 FROM citas c JOIN odontologos o ON o.id_odontologo=c.id_odontologo "
            + "            WHERE c.id_paciente=p.id_paciente AND c.id_clinica=p.id_clinica "
            + "              AND o.id_clinica=p.id_clinica AND o.id_usuario=?) "
            + "AND (?='' OR p.numero_documento LIKE ? OR p.nombres LIKE ? OR p.apellidos LIKE ? "
            + "OR COALESCE(p.telefono,'') LIKE ? OR COALESCE(p.correo,'') LIKE ?) "
            + "ORDER BY p.estado='ACTIVO' DESC, p.apellidos, p.nombres LIMIT 100";
        String q = query == null ? "" : query.trim();
        String like = "%" + q + "%";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setQueryTimeout(8);
            ps.setLong(1, clinicId);
            ps.setLong(2, dentistUserId);
            ps.setString(3, q);
            for (int i = 4; i <= 8; i++) ps.setString(i, like);
            try (ResultSet rs = ps.executeQuery()) { return readDetails(rs); }
        }
    }

    @Override
    public Optional<PacienteDetalle> findDetailedById(long clinicId, long patientId) throws SQLException {
        String sql = "SELECT " + DETAIL_COLUMNS + " FROM pacientes p WHERE p.id_clinica=? AND p.id_paciente=?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(readDetail(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public boolean isAssignedToDentist(long clinicId, long dentistUserId, long patientId) throws SQLException {
        String sql = """
            SELECT EXISTS(
              SELECT 1
              FROM citas c
              JOIN odontologos o ON o.id_odontologo=c.id_odontologo
              WHERE c.id_clinica=? AND c.id_paciente=? AND o.id_clinica=? AND o.id_usuario=?
              LIMIT 1
            ) AS permitido
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setLong(2, patientId);
            ps.setLong(3, clinicId);
            ps.setLong(4, dentistUserId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBoolean(1); }
        }
    }

    @Override
    public boolean documentExists(long clinicId, String documentType, String documentNumber, Long excludePatientId)
        throws SQLException {
        String sql = excludePatientId == null
            ? "SELECT 1 FROM pacientes WHERE id_clinica=? AND tipo_documento=? AND numero_documento=? LIMIT 1"
            : "SELECT 1 FROM pacientes WHERE id_clinica=? AND tipo_documento=? AND numero_documento=? AND id_paciente<>? LIMIT 1";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId);
            ps.setString(2, documentType);
            ps.setString(3, documentNumber);
            if (excludePatientId != null) ps.setLong(4, excludePatientId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    @Override
    public long create(
        long clinicId, String documentType, String documentNumber, String nombres, String apellidos,
        LocalDate birthDate, String sexo, String direccion, String telefono, String correo,
        String ocupacion, String emergencyContact, String emergencyPhone, String estado
    ) throws SQLException {
        String sql = """
            INSERT INTO pacientes
              (id_usuario,id_clinica,tipo_documento,numero_documento,nombres,apellidos,fecha_nacimiento,
               sexo,direccion,telefono,correo,ocupacion,contacto_emergencia,telefono_emergencia,estado)
            VALUES (NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setLong(i++, clinicId);
            ps.setString(i++, documentType);
            ps.setString(i++, documentNumber);
            ps.setString(i++, nombres);
            ps.setString(i++, apellidos);
            ps.setDate(i++, Date.valueOf(birthDate));
            ps.setString(i++, sexo);
            ps.setString(i++, direccion);
            ps.setString(i++, telefono);
            ps.setString(i++, correo);
            ps.setString(i++, ocupacion);
            ps.setString(i++, emergencyContact);
            ps.setString(i++, emergencyPhone);
            ps.setString(i, estado);
            if (ps.executeUpdate() != 1) throw new SQLException("No se pudo crear el paciente.");
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("MySQL no devolvió el id del paciente creado.");
                return keys.getLong(1);
            }
        }
    }

    @Override
    public void update(
        long clinicId, long patientId, String documentType, String documentNumber,
        String nombres, String apellidos, LocalDate birthDate, String sexo, String direccion,
        String telefono, String correo, String ocupacion, String emergencyContact,
        String emergencyPhone, String estado
    ) throws SQLException {
        String sql = """
            UPDATE pacientes
            SET tipo_documento=?, numero_documento=?, nombres=?, apellidos=?, fecha_nacimiento=?, sexo=?,
                direccion=?, telefono=?, correo=?, ocupacion=?, contacto_emergencia=?, telefono_emergencia=?, estado=?
            WHERE id_clinica=? AND id_paciente=?
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, documentType);
            ps.setString(i++, documentNumber);
            ps.setString(i++, nombres);
            ps.setString(i++, apellidos);
            ps.setDate(i++, Date.valueOf(birthDate));
            ps.setString(i++, sexo);
            ps.setString(i++, direccion);
            ps.setString(i++, telefono);
            ps.setString(i++, correo);
            ps.setString(i++, ocupacion);
            ps.setString(i++, emergencyContact);
            ps.setString(i++, emergencyPhone);
            ps.setString(i++, estado);
            ps.setLong(i++, clinicId);
            ps.setLong(i, patientId);
            if (ps.executeUpdate() != 1) throw new SQLException("No se encontró el paciente a actualizar.");
        }
    }

    @Override
    public void updateStatus(long clinicId, long patientId, String estado) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                 "UPDATE pacientes SET estado=? WHERE id_clinica=? AND id_paciente=?")) {
            ps.setString(1, estado);
            ps.setLong(2, clinicId);
            ps.setLong(3, patientId);
            if (ps.executeUpdate() != 1) throw new SQLException("No se encontró el paciente.");
        }
    }

    @Override
    public PacienteFicha loadFicha(long clinicId, long patientId, boolean includeClinical, boolean includePayments) throws SQLException {
        PacienteDetalle patient = findDetailedById(clinicId, patientId)
            .orElseThrow(() -> new SQLException("El paciente solicitado no existe."));
        try (Connection cn = DatabaseConnection.getConnection()) {
            return new PacienteFicha(
                patient,
                readAppointments(cn, clinicId, patientId),
                includeClinical ? readHistory(cn, patientId) : Optional.empty(),
                includeClinical ? readPrescriptions(cn, patientId) : List.of(),
                includeClinical ? readXrays(cn, patientId) : List.of(),
                includePayments ? readPayments(cn, clinicId, patientId) : List.of()
            );
        }
    }

    private List<PacienteCitaDetalle> readAppointments(Connection cn, long clinicId, long patientId) throws SQLException {
        String sql = """
            SELECT DATE_FORMAT(c.fecha,'%Y-%m-%d') AS fecha,
                   TIME_FORMAT(c.hora_inicio,'%H:%i') AS hora,
                   CONCAT(o.nombres,' ',o.apellidos) AS odontologo,
                   e.nombre AS estado, COALESCE(c.motivo,'') AS motivo
            FROM citas c
            JOIN odontologos o ON o.id_odontologo=c.id_odontologo
            JOIN estados_cita e ON e.id_estado_cita=c.id_estado_cita
            WHERE c.id_clinica=? AND c.id_paciente=?
            ORDER BY c.fecha DESC, c.hora_inicio DESC LIMIT 8
            """;
        List<PacienteCitaDetalle> out = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId); ps.setLong(2, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new PacienteCitaDetalle(
                    rs.getString("fecha"), rs.getString("hora"), rs.getString("odontologo"),
                    rs.getString("estado"), rs.getString("motivo")
                ));
            }
        }
        return out;
    }

    private Optional<PacienteHistoriaDetalle> readHistory(Connection cn, long patientId) throws SQLException {
        String sql = """
            SELECT DATE_FORMAT(fecha_apertura,'%Y-%m-%d') AS fecha_apertura, estado,
                   COALESCE(observaciones_generales,'') AS observaciones
            FROM historias_clinicas WHERE id_paciente=? LIMIT 1
            """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new PacienteHistoriaDetalle(
                    rs.getString("fecha_apertura"), rs.getString("estado"), rs.getString("observaciones")
                ));
            }
        }
    }

    private List<PacienteRecetaDetalle> readPrescriptions(Connection cn, long patientId) throws SQLException {
        String sql = """
            SELECT DATE_FORMAT(r.fecha_emision,'%Y-%m-%d') AS fecha,
                   CONCAT(o.nombres,' ',o.apellidos) AS odontologo,
                   r.medicamentos, r.indicaciones
            FROM recetas r
            JOIN odontologos o ON o.id_odontologo=r.id_odontologo
            WHERE r.id_paciente=?
            ORDER BY r.fecha_emision DESC LIMIT 6
            """;
        List<PacienteRecetaDetalle> out = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new PacienteRecetaDetalle(
                    rs.getString("fecha"), rs.getString("odontologo"),
                    rs.getString("medicamentos"), rs.getString("indicaciones")
                ));
            }
        }
        return out;
    }

    private List<PacientePlacaDetalle> readXrays(Connection cn, long patientId) throws SQLException {
        String sql = """
            SELECT DATE_FORMAT(fecha_subida,'%Y-%m-%d') AS fecha, nombre_archivo, url_archivo
            FROM placas_radiograficas WHERE id_paciente=? ORDER BY fecha_subida DESC LIMIT 6
            """;
        List<PacientePlacaDetalle> out = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new PacientePlacaDetalle(
                    rs.getString("fecha"), rs.getString("nombre_archivo"), rs.getString("url_archivo")
                ));
            }
        }
        return out;
    }

    private List<PacientePagoDetalle> readPayments(Connection cn, long clinicId, long patientId) throws SQLException {
        String sql = """
            SELECT DATE_FORMAT(p.fecha_pago,'%Y-%m-%d') AS fecha,
                   CAST(p.monto AS CHAR) AS monto, p.metodo_pago,
                   CONCAT(DATE_FORMAT(c.fecha,'%Y-%m-%d'),' ',TIME_FORMAT(c.hora_inicio,'%H:%i')) AS cita
            FROM pagos p
            JOIN citas c ON c.id_cita=p.id_cita
            WHERE c.id_clinica=? AND c.id_paciente=?
            ORDER BY p.fecha_pago DESC LIMIT 8
            """;
        List<PacientePagoDetalle> out = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, clinicId); ps.setLong(2, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new PacientePagoDetalle(
                    rs.getString("fecha"), rs.getString("monto"), rs.getString("metodo_pago"), rs.getString("cita")
                ));
            }
        }
        return out;
    }

    private List<PacienteResumen> readSummary(ResultSet rs) throws SQLException {
        List<PacienteResumen> patients = new ArrayList<>();
        while (rs.next()) patients.add(new PacienteResumen(
            rs.getString("paciente"), rs.getString("numero_documento"),
            rs.getString("telefono"), rs.getString("estado")
        ));
        return patients;
    }

    private List<PacienteDetalle> readDetails(ResultSet rs) throws SQLException {
        List<PacienteDetalle> patients = new ArrayList<>();
        while (rs.next()) patients.add(readDetail(rs));
        return patients;
    }

    private PacienteDetalle readDetail(ResultSet rs) throws SQLException {
        Date birth = rs.getDate("fecha_nacimiento");
        return new PacienteDetalle(
            rs.getLong("id_paciente"), rs.getString("tipo_documento"), rs.getString("numero_documento"),
            rs.getString("nombres"), rs.getString("apellidos"), birth == null ? null : birth.toLocalDate(),
            rs.getString("sexo"), rs.getString("direccion"), rs.getString("telefono"), rs.getString("correo"),
            rs.getString("ocupacion"), rs.getString("contacto_emergencia"), rs.getString("telefono_emergencia"),
            rs.getString("estado")
        );
    }
}
