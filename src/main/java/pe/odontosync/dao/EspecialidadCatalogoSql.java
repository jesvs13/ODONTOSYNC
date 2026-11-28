package pe.odontosync.dao;

/** Fragmentos SQL compartidos del catálogo. Los alias fijos son e y ec. */
final class EspecialidadCatalogoSql {
    private EspecialidadCatalogoSql() { }

    static final String ACTIVE = """
        CASE WHEN e.id_clinica IS NULL
             THEN COALESCE(ec.activa, e.estado = 'ACTIVA')
             ELSE e.estado = 'ACTIVA' END
        """.strip();

    // Los argumentos son exclusivamente expresiones SQL internas, nunca datos del usuario.
    static String availabilityJoin(String clinicExpression) {
        return "LEFT JOIN especialidad_clinica ec ON ec.id_especialidad = e.id_especialidad"
            + " AND ec.id_clinica = " + clinicExpression;
    }

    static String visibleTo(String clinicExpression) {
        return "(e.id_clinica IS NULL OR e.id_clinica = " + clinicExpression + ")";
    }
}
