package pe.odontosync.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/** Cita y datos clínicos necesarios para registrar o revisar una atención. */
public record AtencionCitaDetalle(
    long idCita,
    long idPaciente,
    String paciente,
    String documento,
    long idOdontologo,
    String odontologo,
    String especialidad,
    String estadoCita,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    String motivoCita,
    String observacionesCita,
    Optional<ConsultaClinicaDetalle> consulta
) { }
