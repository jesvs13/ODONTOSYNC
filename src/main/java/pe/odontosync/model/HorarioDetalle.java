package pe.odontosync.model;

import java.time.LocalTime;

/** Bloque horario configurable para un odontólogo. */
public record HorarioDetalle(
    long idHorario,
    long idOdontologo,
    String odontologo,
    int diaSemana,
    LocalTime horaInicio,
    LocalTime horaFin,
    String estado
) {
    public String diaNombre() {
        return switch (diaSemana) {
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miércoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            case 6 -> "Sábado";
            case 7 -> "Domingo";
            default -> "Día " + diaSemana;
        };
    }
}
