package pe.odontosync.model;

import java.time.LocalDate;

/** Datos completos del paciente utilizados por el CRUD y la ficha. */
public record PacienteDetalle(
    long id,
    String tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidos,
    LocalDate fechaNacimiento,
    String sexo,
    String direccion,
    String telefono,
    String correo,
    String ocupacion,
    String contactoEmergencia,
    String telefonoEmergencia,
    String estado
) {
    public String nombreCompleto() {
        return (nombres + " " + apellidos).trim();
    }
}
