package pe.odontosync.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.PlacaCrudDAO;
import pe.odontosync.model.PlacaDetalle;

/** Reglas clínicas y almacenamiento privado de archivos radiográficos. */
public final class PlacaService {
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final Set<String> TYPES = Set.of(
        "PANORAMICA", "PERIAPICAL", "BITEWING", "OCLUSAL", "CEFALOMETRICA", "OTRA");
    private static final Pattern STORAGE_KEY = Pattern.compile("rx-[0-9]+\\.(png|jpg|pdf)");

    private final PlacaCrudDAO dao;
    private final Path storage;
    private final Clock clock;

    public PlacaService(PlacaCrudDAO dao, Path storage, Clock clock) {
        this.dao = Objects.requireNonNull(dao);
        this.storage = Objects.requireNonNull(storage).toAbsolutePath().normalize();
        this.clock = Objects.requireNonNull(clock);
    }

    public long create(User current, long patientId, String type, LocalDate date,
                       String region, String observations, String filename,
                       String mime, byte[] bytes) throws SQLException, IOException {
        requireDentist(current);
        requirePatient(current, patientId);
        String normalizedType = clean(type).toUpperCase(Locale.ROOT);
        if (!TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Selecciona un tipo de radiografía válido.");
        }
        if (date == null || date.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("La fecha de radiografía no es válida.");
        }
        String dentalRegion = bounded(region, 100, "La región dental");
        String notes = bounded(observations, 5000, "Las observaciones");
        String name = RadiografiaFileValidator.validateName(filename);
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo debe tener entre 1 byte y 10 MB.");
        }
        byte[] content = bytes.clone();
        String media = clean(mime).toLowerCase(Locale.ROOT);
        String suffix = RadiografiaFileValidator.validateContent(name, media, content);

        Files.createDirectories(storage);
        Path root = storage.toRealPath();
        Path file = Files.createTempFile(root, "rx-", suffix);
        try {
            Files.write(file, content, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING, LinkOption.NOFOLLOW_LINKS);
            PlacaDetalle data = new PlacaDetalle(0, patientId, current.id(), normalizedType,
                date, dentalRegion, notes, name, file.getFileName().toString(), media,
                content.length, "ACTIVA");
            long id = dao.create(current.clinicId(), current.id(), data);
            if (id <= 0) throw new SQLException("No se obtuvo el identificador de la radiografía.");
            return id;
        } catch (IOException | SQLException | RuntimeException ex) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException cleanup) {
                ex.addSuppressed(cleanup);
            }
            throw ex;
        }
    }

    public List<PlacaDetalle> historyForPatient(User current, long patientId) throws SQLException {
        requireDentist(current);
        requirePatient(current, patientId);
        return dao.findByPatient(current.clinicId(), current.id(), patientId);
    }

    public PlacaDetalle detail(User current, long plateId) throws SQLException {
        requireDentist(current);
        if (plateId <= 0) throw new IllegalArgumentException("Selecciona una radiografía válida.");
        PlacaDetalle plate = dao.findById(current.clinicId(), current.id(), plateId);
        if (plate == null || plate.idPlaca() != plateId) {
            throw new IllegalArgumentException("Radiografía no disponible para este odontólogo.");
        }
        requirePatient(current, plate.idPaciente());
        return plate;
    }

    public byte[] download(User current, long plateId) throws SQLException, IOException {
        PlacaDetalle plate = detail(current, plateId);
        requireActive(plate);
        String key = plate.claveArchivo();
        if (key == null || !STORAGE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("El archivo privado no está disponible.");
        }
        Path root = storage.toRealPath();
        Path file = root.resolve(key);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Archivo radiográfico no disponible.");
        }
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length == 0 || bytes.length > MAX_BYTES || bytes.length != plate.tamanoBytes()) {
                throw new IOException("El tamaño del archivo no coincide con el registro.");
            }
            return bytes;
        }
    }

    public void annul(User current, long plateId, String reason) throws SQLException {
        PlacaDetalle plate = detail(current, plateId);
        requireActive(plate);
        if (plate.idUsuario() != current.id()) {
            throw new IllegalArgumentException("Solo el odontólogo emisor puede anular la radiografía.");
        }
        String value = bounded(reason, 500, "El motivo de anulación");
        if (value.isBlank()) throw new IllegalArgumentException("Indica el motivo de anulación.");
        if (!dao.annul(current.clinicId(), current.id(), plateId, value)) {
            throw new IllegalArgumentException("No se pudo anular la radiografía.");
        }
    }

    private void requireDentist(User current) {
        if (current == null || current.id() <= 0 || current.clinicId() <= 0
            || current.role() != Role.ODONTOLOGO || !current.role().allows("placas")) {
            throw new IllegalArgumentException("Solo un odontólogo autorizado puede gestionar radiografías.");
        }
    }

    private void requirePatient(User current, long patientId) throws SQLException {
        if (patientId <= 0 || !dao.patientVisible(current.clinicId(), current.id(), patientId)) {
            throw new IllegalArgumentException("Paciente no disponible para este odontólogo.");
        }
    }

    private void requireActive(PlacaDetalle plate) {
        if (!"ACTIVA".equals(plate.estado())) {
            throw new IllegalArgumentException("La radiografía ya no está activa.");
        }
    }

    private String clean(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private String bounded(String raw, int max, String label) {
        String value = clean(raw);
        if (value.length() > max) throw new IllegalArgumentException(label + " supera el límite permitido.");
        return value;
    }
}
