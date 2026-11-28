package pe.odontosync.dao;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.model.EspecialidadCatalogo;

/** Persistencia del catálogo; todas las operaciones están delimitadas por clínica. */
public interface EspecialidadCrudDAO {
    /** Devuelve globales y propias, activas e inactivas, con disponibilidad efectiva. */
    List<EspecialidadCatalogo> listByClinic(long clinicId) throws SQLException;

    /** Crea una especialidad propia activa. */
    long create(long clinicId, String nombre) throws SQLException;

    /** Cambia únicamente el nombre de una especialidad propia de la clínica. */
    void rename(long clinicId, long id, String nombre) throws SQLException;

    /** Cambia disponibilidad para la clínica, sin eliminar el registro ni sus vínculos. */
    void setActive(long clinicId, long id, boolean active) throws SQLException;
}
