package pe.odontosync.model;

import java.time.LocalDate;
import java.util.List;

/** Perfil profesional y cuenta vinculada de un odontólogo. */
public record OdontologoDetalle(
    long idOdontologo,
    long idUsuario,
    String nombres,
    String apellidos,
    String numeroColegiatura,
    String telefono,
    String correo,
    LocalDate fechaIngreso,
    String estado,
    List<Long> especialidadIds,
    List<String> especialidades
) {
    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    public String especialidadesTexto() {
        return especialidades == null || especialidades.isEmpty()
            ? "Sin especialidad asignada"
            : String.join(", ", especialidades);
    }
}
