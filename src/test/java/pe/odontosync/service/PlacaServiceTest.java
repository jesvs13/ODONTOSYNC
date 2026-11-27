package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.PlacaCrudDAO;
import pe.odontosync.model.PlacaDetalle;

class PlacaServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 26);
    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-11-26T15:00:00Z"), ZoneOffset.UTC);
    private static final int MAX_BYTES = 10 * 1024 * 1024;

    @TempDir Path storage;
    private final FakeDao dao = new FakeDao();

    private PlacaService service() {
        return new PlacaService(dao, storage, CLOCK);
    }

    @Test
    void registraImagenPrivadaConClinicaYUsuarioDeSesion() throws Exception {
        byte[] bytes = image("png");
        long id = register(dentist(), 31, "panoramica.png", "image/png", bytes);
        assertEquals(801, id);
        assertEquals(5, dao.clinicId);
        assertEquals(44, dao.userId);
        assertEquals(31, dao.saved.idPaciente());
        assertEquals(44, dao.saved.idUsuario());
        assertEquals("PANORAMICA", dao.saved.tipoRadiografia());
        assertEquals(DATE, dao.saved.fechaRadiografia());
        assertEquals("Pieza 36", dao.saved.regionDental());
        assertEquals("Control", dao.saved.observaciones());
        assertEquals("panoramica.png", dao.saved.nombreArchivo());
        assertEquals("image/png", dao.saved.tipoMime());
        assertEquals(bytes.length, dao.saved.tamanoBytes());
        assertEquals("ACTIVA", dao.saved.estado());
        Path key = Path.of(dao.saved.claveArchivo());
        assertFalse(key.isAbsolute());
        Path stored = storage.resolve(key).normalize();
        assertTrue(stored.startsWith(storage));
        assertNotEquals("panoramica.png", stored.getFileName().toString());
        assertArrayEquals(bytes, Files.readAllBytes(stored));
        assertArrayEquals(bytes, service().download(dentist(), id));
    }

    @Test
    void admiteJpegYPdfValidos() throws Exception {
        byte[] jpg = image("jpg");
        register(dentist(), 31, "placa.jpeg", "image/jpeg", jpg);
        assertEquals("image/jpeg", dao.saved.tipoMime());
        byte[] pdf = pdf();
        register(dentist(), 31, "placa.pdf", "application/pdf", pdf);
        assertEquals("application/pdf", dao.saved.tipoMime());
        assertArrayEquals(pdf, service().download(dentist(), 801));
    }

    @Test
    void rechazaArchivoVacioExcesivoOFalsoSinGuardar() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> register(dentist(), 31,
            "placa.png", "image/png", new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> register(dentist(), 31,
            "placa.png", "image/png", new byte[MAX_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () -> register(dentist(), 31,
            "placa.png", "image/png", "texto".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> register(dentist(), 31,
            "placa.png", "image/png", new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10}));
        assertThrows(IllegalArgumentException.class, () -> register(dentist(), 31,
            "placa.pdf", "application/pdf", "%PDF-1.4\ntexto".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> register(dentist(), 31,
            "placa.png", "image/png", image("jpg")));
        assertThrows(IllegalArgumentException.class, () -> register(dentist(), 31,
            "placa.exe", "image/png", image("png")));
        assertThrows(IllegalArgumentException.class, () -> register(dentist(), 31,
            "../placa.png", "image/png", image("png")));
        assertNoWrites();
    }

    @Test
    void rechazaTipoFechaYPacienteInvalidos() throws Exception {
        byte[] bytes = image("png");
        assertThrows(IllegalArgumentException.class, () -> service().create(dentist(), 31,
            "INVALIDA", DATE, "", "", "placa.png", "image/png", bytes));
        assertThrows(IllegalArgumentException.class, () -> service().create(dentist(), 31,
            "PANORAMICA", null, "", "", "placa.png", "image/png", bytes));
        assertThrows(IllegalArgumentException.class, () -> register(dentist(), 0,
            "placa.png", "image/png", bytes));
        assertNoWrites();
    }

    @Test
    void rechazaRolesNoClinicosYUsuariosSinSesion() throws Exception {
        byte[] bytes = image("png");
        for (User user : new User[]{new User(1, 5, "Admin", "Clinica", Role.ADMINISTRADOR),
            new User(2, 5, "Recepcion", "Clinica", Role.RECEPCIONISTA), null}) {
            assertThrows(IllegalArgumentException.class, () -> register(user, 31,
                "placa.png", "image/png", bytes));
            assertThrows(IllegalArgumentException.class, () -> service().historyForPatient(user, 31));
            assertThrows(IllegalArgumentException.class, () -> service().detail(user, 801));
            assertThrows(IllegalArgumentException.class, () -> service().download(user, 801));
            assertThrows(IllegalArgumentException.class, () -> service().annul(user, 801, "Duplicado"));
        }
        assertEquals(0, dao.calls);
        assertNoWrites();
    }

    @Test
    void rechazaPacienteNoVinculadoYOtraClinica() throws Exception {
        byte[] bytes = image("png");
        assertThrows(IllegalArgumentException.class, () -> register(dentist(), 99,
            "placa.png", "image/png", bytes));
        assertThrows(IllegalArgumentException.class, () -> register(otherClinic(), 31,
            "placa.png", "image/png", bytes));
        assertThrows(IllegalArgumentException.class, () -> service().historyForPatient(dentist(), 99));
        assertThrows(IllegalArgumentException.class, () -> service().historyForPatient(otherClinic(), 31));
        assertNoWrites();
    }

    @Test
    void consultaYDescargaSoloDentroDelAlcanceAutorizado() throws Exception {
        register(dentist(), 31, "placa.png", "image/png", image("png"));
        assertEquals(801, service().historyForPatient(dentist(), 31).get(0).idPlaca());
        assertEquals(801, service().detail(dentist(), 801).idPlaca());
        assertEquals(801, service().detail(linkedDentist(), 801).idPlaca());
        for (User user : List.of(otherClinic(), unlinkedDentist())) {
            assertThrows(IllegalArgumentException.class, () -> service().detail(user, 801));
            assertThrows(IllegalArgumentException.class, () -> service().download(user, 801));
            assertThrows(IllegalArgumentException.class, () -> service().annul(user, 801, "Duplicado"));
        }
        assertThrows(IllegalArgumentException.class, () -> service().detail(dentist(), 999));
        assertEquals(0, dao.annuls);
    }

    @Test
    void anulaSoloRegistroPropioConMotivoYConservaArchivo() throws Exception {
        byte[] bytes = image("png");
        register(dentist(), 31, "placa.png", "image/png", bytes);
        Path stored = storage.resolve(dao.saved.claveArchivo());
        assertThrows(IllegalArgumentException.class, () -> service().annul(dentist(), 801, " "));
        assertThrows(IllegalArgumentException.class, () -> service().annul(linkedDentist(), 801, "Duplicado"));
        assertEquals(0, dao.annuls);
        service().annul(dentist(), 801, "Duplicado");
        assertEquals(1, dao.annuls);
        assertEquals("Duplicado", dao.reason);
        assertEquals(5, dao.clinicId);
        assertEquals(44, dao.userId);
        assertEquals("ANULADA", service().historyForPatient(dentist(), 31).get(0).estado());
        assertArrayEquals(bytes, Files.readAllBytes(stored));
        assertThrows(IllegalArgumentException.class, () -> service().download(dentist(), 801));
        assertThrows(IllegalArgumentException.class, () -> service().annul(dentist(), 801, "Otra vez"));
        assertEquals(1, dao.annuls);
    }

    @Test
    void siFallaRegistroEnBaseDeDatosNoQuedaArchivoHuerfano() throws Exception {
        dao.failInsert = true;
        assertThrows(SQLException.class, () -> register(dentist(), 31,
            "placa.png", "image/png", image("png")));
        assertNoWrites();
    }

    private long register(User user, long patient, String name, String mime, byte[] bytes)
        throws Exception {
        return service().create(user, patient, "PANORAMICA", DATE,
            "Pieza 36", "Control", name, mime, bytes);
    }

    private void assertNoWrites() throws IOException {
        assertNull(dao.saved);
        try (var paths = Files.walk(storage)) {
            assertFalse(paths.anyMatch(Files::isRegularFile));
        }
    }

    private static byte[] image(String format) throws IOException {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, format, out));
        return out.toByteArray();
    }

    private static byte[] pdf() {
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (String obj : List.of("<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 100 100] /Resources << >> >>")) {
            offsets.add(pdf.length());
            pdf.append(offsets.size()).append(" 0 obj\n").append(obj).append("\nendobj\n");
        }
        int xref = pdf.length();
        pdf.append("xref\n0 4\n0000000000 65535 f \n");
        for (int offset : offsets) pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        pdf.append("trailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static User dentist() { return new User(44, 5, "Carlos", "Clinica", Role.ODONTOLOGO); }
    private static User linkedDentist() { return new User(45, 5, "Ana", "Clinica", Role.ODONTOLOGO); }
    private static User unlinkedDentist() { return new User(46, 5, "Luis", "Clinica", Role.ODONTOLOGO); }
    private static User otherClinic() { return new User(44, 6, "Carlos", "Otra", Role.ODONTOLOGO); }

    private static final class FakeDao implements PlacaCrudDAO {
        long clinicId, userId;
        int calls, annuls;
        boolean failInsert;
        String reason;
        PlacaDetalle saved;

        private boolean allowed(long clinic, long user, long patient) {
            return clinic == 5 && (user == 44 || user == 45) && patient == 31;
        }
        private void capture(long clinic, long user) {
            calls++; clinicId = clinic; userId = user;
        }
        @Override public boolean patientVisible(long clinic, long user, long patient) {
            capture(clinic, user); return allowed(clinic, user, patient);
        }
        @Override public long create(long clinic, long user, PlacaDetalle data) throws SQLException {
            capture(clinic, user);
            if (failInsert) throw new SQLException("Fallo simulado antes de insertar");
            if (!allowed(clinic, user, data.idPaciente())) throw new IllegalArgumentException("No autorizado");
            saved = copy(data, "ACTIVA"); return 801;
        }
        @Override public List<PlacaDetalle> findByPatient(long clinic, long user, long patient) {
            capture(clinic, user);
            return allowed(clinic, user, patient) && saved != null ? List.of(saved) : List.of();
        }
        @Override public PlacaDetalle findById(long clinic, long user, long id) {
            capture(clinic, user);
            return id == 801 && saved != null && allowed(clinic, user, saved.idPaciente()) ? saved : null;
        }
        @Override public boolean annul(long clinic, long user, long id, String reason) {
            capture(clinic, user);
            if (saved == null || id != 801 || !allowed(clinic, user, saved.idPaciente())
                || saved.idUsuario() != user || !"ACTIVA".equals(saved.estado())) return false;
            this.reason = reason; annuls++; saved = copy(saved, "ANULADA"); return true;
        }
        private PlacaDetalle copy(PlacaDetalle x, String state) {
            return new PlacaDetalle(801, x.idPaciente(), x.idUsuario(), x.tipoRadiografia(),
                x.fechaRadiografia(), x.regionDental(), x.observaciones(), x.nombreArchivo(),
                x.claveArchivo(), x.tipoMime(), x.tamanoBytes(), state);
        }
    }
}
