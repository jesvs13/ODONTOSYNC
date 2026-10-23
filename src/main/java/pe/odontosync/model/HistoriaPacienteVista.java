package pe.odontosync.model;

import java.util.List;
import java.util.Optional;

/** Agregado de lectura para la historia clínica longitudinal. */
public record HistoriaPacienteVista(
    PacienteDetalle paciente,
    Optional<HistoriaClinicaDetalle> historia,
    List<AntecedenteClinicoDetalle> antecedentes,
    List<AlergiaClinicaDetalle> alergias,
    List<ConsultaHistorialDetalle> consultas
) { }
