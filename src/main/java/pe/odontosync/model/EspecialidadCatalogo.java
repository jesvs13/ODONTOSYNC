package pe.odontosync.model;

/** Especialidad visible en una clínica y su disponibilidad efectiva. */
public record EspecialidadCatalogo(long id, Long clinicId, String nombre, boolean active) {
    // clinicId nulo identifica una especialidad del catálogo global.
}
