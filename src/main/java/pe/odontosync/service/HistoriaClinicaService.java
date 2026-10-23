package pe.odontosync.service;

import java.sql.SQLException;
import java.util.List;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.ClinicaAtencionDAO;
import pe.odontosync.model.HistoriaPacienteResumen;
import pe.odontosync.model.HistoriaPacienteVista;

/** Reglas de acceso y validación de la historia clínica longitudinal. */
public final class HistoriaClinicaService {
    private final ClinicaAtencionDAO dao;

    public HistoriaClinicaService(ClinicaAtencionDAO dao) {
        this.dao = dao;
    }

    public List<HistoriaPacienteResumen> list(User current, String query) throws SQLException {
        requireDentist(current);
        String q = clean(query);
        if (q.length() > 80) throw new IllegalArgumentException("La búsqueda es demasiado larga.");
        return dao.searchHistories(current.clinicId(), current.id(), q);
    }

    public HistoriaPacienteVista load(User current, long patientId) throws SQLException {
        requireDentist(current);
        return dao.loadHistory(current.clinicId(), current.id(), patientId)
            .orElseThrow(() -> new IllegalArgumentException("El paciente no está vinculado a tus citas o ya no está disponible."));
    }

    public void updateGeneralNotes(User current, long patientId, String notes) throws SQLException {
        requireDentist(current);
        String value = limited(notes, 5000, "Las observaciones generales");
        dao.updateGeneralNotes(current.clinicId(), current.id(), patientId, value);
    }

    public long addMedicalHistory(
        User current, long patientId, String type, String description, String observations
    ) throws SQLException {
        requireDentist(current);
        String cleanType = required(type, 100, "tipo de antecedente");
        String cleanDescription = required(description, 255, "descripción del antecedente");
        String cleanObservations = limited(observations, 2000, "Las observaciones del antecedente");
        return dao.addMedicalHistory(
            current.clinicId(), current.id(), patientId, cleanType, cleanDescription, cleanObservations
        );
    }

    public long addAllergy(
        User current, long patientId, String allergen, String reaction, String observations
    ) throws SQLException {
        requireDentist(current);
        String cleanAllergen = required(allergen, 150, "alérgeno");
        String cleanReaction = limited(reaction, 255, "La reacción");
        String cleanObservations = limited(observations, 2000, "Las observaciones de la alergia");
        return dao.addAllergy(
            current.clinicId(), current.id(), patientId, cleanAllergen, cleanReaction, cleanObservations
        );
    }

    private void requireDentist(User current) {
        if (current == null || current.role() != Role.ODONTOLOGO) {
            throw new IllegalArgumentException("La historia clínica solo está disponible para el perfil Odontólogo.");
        }
    }

    private String required(String value, int max, String label) {
        String v = limited(value, max, "El " + label);
        if (v.length() < 2) throw new IllegalArgumentException("El " + label + " es obligatorio.");
        return v;
    }

    private String limited(String value, int max, String label) {
        String v = clean(value);
        if (v.length() > max) throw new IllegalArgumentException(label + " supera el máximo permitido.");
        return v;
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
}
