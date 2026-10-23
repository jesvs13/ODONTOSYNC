package pe.odontosync.service;

import java.sql.SQLException;
import org.apache.commons.lang3.StringUtils;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.ClinicaAtencionDAO;
import pe.odontosync.model.AtencionCitaDetalle;

/** Reglas de negocio para convertir una cita confirmada en una atención clínica trazable. */
public final class AtencionClinicaService {
    private final ClinicaAtencionDAO dao;

    public AtencionClinicaService(ClinicaAtencionDAO dao) {
        this.dao = dao;
    }

    public AtencionCitaDetalle load(User current, long appointmentId) throws SQLException {
        requireDentist(current);
        AtencionCitaDetalle appointment = dao.findAppointmentForDentist(
            current.clinicId(), current.id(), appointmentId
        ).orElseThrow(() -> new IllegalArgumentException("La cita no existe o no está asignada a tu cuenta de odontólogo."));
        if (!("CONFIRMADA".equals(appointment.estadoCita()) || "ATENDIDA".equals(appointment.estadoCita()))) {
            throw new IllegalArgumentException("La atención clínica solo está disponible para citas CONFIRMADAS o ya ATENDIDAS.");
        }
        return appointment;
    }

    public long save(
        User current, long appointmentId, String motive, String diagnosis,
        String treatment, String observations, String recommendations
    ) throws SQLException {
        AtencionCitaDetalle appointment = load(current, appointmentId);
        String cleanMotive = text(motive, 2000, "El motivo de consulta");
        if (cleanMotive.isBlank()) cleanMotive = appointment.motivoCita();
        if (cleanMotive.isBlank()) throw new IllegalArgumentException("Ingresa el motivo de la consulta clínica.");

        String cleanDiagnosis = required(diagnosis, 5000, "diagnóstico");
        String cleanTreatment = required(treatment, 5000, "tratamiento");
        String cleanObservations = text(observations, 5000, "Las observaciones");
        String cleanRecommendations = text(recommendations, 5000, "Las recomendaciones");

        return dao.saveAttention(
            current.clinicId(), current.id(), appointmentId,
            cleanMotive, cleanDiagnosis, cleanTreatment, cleanObservations, cleanRecommendations
        );
    }

    private void requireDentist(User current) {
        if (current == null || current.role() != Role.ODONTOLOGO) {
            throw new IllegalArgumentException("Solo un odontólogo puede registrar información clínica de una atención.");
        }
    }

    private String required(String value, int max, String label) {
        String v = text(value, max, "El " + label);
        if (v.length() < 3) throw new IllegalArgumentException("El " + label + " debe contener al menos 3 caracteres.");
        return v;
    }

    private String text(String value, int max, String label) {
        String v = StringUtils.trimToEmpty(value);
        if (v.length() > max) throw new IllegalArgumentException(label + " supera el máximo de " + max + " caracteres.");
        return v;
    }
}
