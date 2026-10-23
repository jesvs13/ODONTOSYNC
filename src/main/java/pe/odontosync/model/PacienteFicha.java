package pe.odontosync.model;

import java.util.List;
import java.util.Optional;

/** Agregado de lectura para mostrar la ficha longitudinal de un paciente. */
public record PacienteFicha(
    PacienteDetalle paciente,
    List<PacienteCitaDetalle> citas,
    Optional<PacienteHistoriaDetalle> historia,
    List<PacienteRecetaDetalle> recetas,
    List<PacientePlacaDetalle> placas,
    List<PacientePagoDetalle> pagos
) { }
