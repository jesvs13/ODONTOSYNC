package pe.odontosync.service;

import java.sql.SQLException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.RecetaCrudDAO;
import pe.odontosync.model.PacienteOpcion;
import pe.odontosync.model.RecetaResumen;

/** Reglas de negocio para emitir y consultar recetas médicas. */
public final class RecetaService {
    private static final int MAX_MEDICATIONS = 5000;
    private static final int MAX_INDICATIONS = 5000;

    private final RecetaCrudDAO dao;

    public RecetaService(RecetaCrudDAO dao) {
        this.dao = dao;
    }

    public boolean canCreate(Role role) {
        return role == Role.ODONTOLOGO;
    }

    public List<RecetaResumen> history(User current) throws SQLException {
        requireDentist(current);
        return dao.findByDentist(current.clinicId(), current.id());
    }

    public RecetaResumen detail(User current, long recipeId) throws SQLException {
        requireDentist(current);
        if (recipeId <= 0) {
            throw new IllegalArgumentException("Selecciona una receta válida.");
        }

        RecetaResumen recipe = dao.findById(current.clinicId(), current.id(), recipeId);
        if (recipe == null) {
            throw new IllegalArgumentException(
                "La receta no existe o no está disponible para este odontólogo."
            );
        }
        return recipe;
    }

    public List<PacienteOpcion> patients(User current) throws SQLException {
        requireDentist(current);
        return dao.findPatientsByDentist(current.clinicId(), current.id());
    }

    public List<RecetaResumen> historyForPatient(User current, long patientId) throws SQLException {
        requireDentist(current);
        if (patientId <= 0) {
            throw new IllegalArgumentException("Selecciona un paciente válido.");
        }
        return dao.findByPatient(current.clinicId(), current.id(), patientId);
    }

    public long create(
        User current,
        long patientId,
        String medicationsRaw,
        String indicationsRaw
    ) throws SQLException {
        requireDentist(current);
        if (patientId <= 0) {
            throw new IllegalArgumentException("Selecciona un paciente válido.");
        }

        String medications = required(
            medicationsRaw, MAX_MEDICATIONS, "Los medicamentos"
        );
        String indications = required(
            indicationsRaw, MAX_INDICATIONS, "Las indicaciones"
        );

        try {
            return dao.create(
                current.clinicId(),
                current.id(),
                patientId,
                medications,
                indications
            );
        } catch (IllegalStateException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private void requireDentist(User current) {
        if (current == null || current.role() != Role.ODONTOLOGO
            || !current.role().allows("recetas")) {
            throw new IllegalArgumentException(
                "Solo un odontólogo autorizado puede acceder a Recetas."
            );
        }
    }

    private String required(String raw, int max, String label) {
        String value = StringUtils.trimToEmpty(raw);
        if (value.length() < 3) {
            throw new IllegalArgumentException(label + " deben contener al menos 3 caracteres.");
        }
        if (value.length() > max) {
            throw new IllegalArgumentException(label + " superan el máximo de " + max + " caracteres.");
        }
        return value;
    }
}
