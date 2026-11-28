package pe.odontosync.service;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.EspecialidadCrudDAO;
import pe.odontosync.model.EspecialidadCatalogo;

/** Reglas de negocio del catálogo de especialidades por clínica. */
public final class EspecialidadService {
    private static final int MAX_NAME_LENGTH = 100;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private final EspecialidadCrudDAO dao;

    public EspecialidadService(EspecialidadCrudDAO dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    public List<EspecialidadCatalogo> list(User current) throws SQLException {
        requireAuthenticated(current);
        return dao.listByClinic(current.clinicId()).stream()
            .filter(e -> e.clinicId() == null || e.clinicId() == current.clinicId())
            .toList();
    }

    public List<EspecialidadCatalogo> active(User current) throws SQLException {
        return list(current).stream().filter(EspecialidadCatalogo::active).toList();
    }

    public long create(User current, String nombre) throws SQLException {
        requireAdmin(current);
        String name = validateName(nombre);
        ensureUnique(list(current), name, null);
        return dao.create(current.clinicId(), name);
    }

    public void rename(User current, long id, String nombre) throws SQLException {
        requireAdmin(current);
        String name = validateName(nombre);
        List<EspecialidadCatalogo> catalog = list(current);
        EspecialidadCatalogo existing = requireVisible(catalog, id);
        if (existing.clinicId() == null) {
            throw new IllegalArgumentException("No puedes modificar el nombre de una especialidad global.");
        }
        ensureUnique(catalog, name, id);
        dao.rename(current.clinicId(), id, name);
    }

    public void setActive(User current, long id, boolean active) throws SQLException {
        requireAdmin(current);
        requireVisible(list(current), id);
        dao.setActive(current.clinicId(), id, active);
    }

    private static EspecialidadCatalogo requireVisible(List<EspecialidadCatalogo> catalog, long id) {
        return catalog.stream().filter(e -> e.id() == id).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("La especialidad no existe en tu clínica."));
    }

    private static void ensureUnique(List<EspecialidadCatalogo> catalog, String name, Long excludeId) {
        String key = comparisonKey(name);
        boolean duplicate = catalog.stream().anyMatch(e ->
            (excludeId == null || e.id() != excludeId) && comparisonKey(e.nombre()).equals(key));
        if (duplicate) {
            throw new IllegalArgumentException("Ya existe una especialidad con ese nombre en tu clínica.");
        }
    }

    private static String validateName(String name) {
        String normalized = normalizeSpaces(name);
        if (normalized.isEmpty() || normalized.length() > MAX_NAME_LENGTH
            || normalized.codePoints().anyMatch(Character::isISOControl)
            || comparisonKey(normalized).isEmpty()) {
            throw new IllegalArgumentException("Ingresa un nombre de especialidad válido de hasta 100 caracteres.");
        }
        return normalized;
    }

    private static String normalizeSpaces(String name) {
        return name == null ? "" : WHITESPACE.matcher(name).replaceAll(" ").trim();
    }

    private static String comparisonKey(String name) {
        String normalized = Normalizer.normalize(normalizeSpaces(name), Normalizer.Form.NFD);
        return MARKS.matcher(normalized).replaceAll("").toLowerCase(Locale.ROOT);
    }

    private static void requireAuthenticated(User current) {
        if (current == null || current.id() <= 0 || current.clinicId() <= 0 || current.role() == null) {
            throw new IllegalArgumentException("Debes iniciar sesión para consultar las especialidades.");
        }
    }

    private static void requireAdmin(User current) {
        requireAuthenticated(current);
        if (current.role() != Role.ADMINISTRADOR) {
            throw new IllegalArgumentException("Solo administración puede gestionar especialidades.");
        }
    }
}
