package pe.odontosync.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Cita completa para la agenda, edición y control de estado. */
public record CitaDetalle(
    long idCita,
    long idPaciente,
    String paciente,
    String documento,
    long idOdontologo,
    String odontologo,
    Long idEspecialidad,
    String especialidad,
    long idEstadoCita,
    String estado,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    String motivo,
    String observaciones
) {
    public String horaRango() {
        return horaInicio + " - " + horaFin;
    }
}
