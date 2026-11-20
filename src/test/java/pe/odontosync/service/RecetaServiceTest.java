package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.RecetaCrudDAO;
import pe.odontosync.model.PacienteOpcion;
import pe.odontosync.model.RecetaResumen;

class RecetaServiceTest {
    @Test
    void odontologoEmiteRecetaValida() throws Exception {
        FakeRecetaDAO dao = new FakeRecetaDAO();
        RecetaService service = new RecetaService(dao);

        long id = service.create(
            dentist(), 31, "Ibuprofeno 400 mg", "Cada 8 horas por 3 días"
        );

        assertEquals(801, id);
        assertEquals(5, dao.clinicId);
        assertEquals(44, dao.dentistUserId);
        assertEquals(31, dao.patientId);
        assertEquals("Ibuprofeno 400 mg", dao.medications);
        assertEquals("Cada 8 horas por 3 días", dao.indications);
    }

    @Test
    void odontologoConsultaRecetasDePacienteVisible() throws Exception {
        FakeRecetaDAO dao = new FakeRecetaDAO();
        RecetaService service = new RecetaService(dao);

        List<RecetaResumen> recipes = service.historyForPatient(dentist(), 31);

        assertEquals(1, recipes.size());
        assertEquals(5, dao.clinicId);
        assertEquals(44, dao.dentistUserId);
        assertEquals(31, dao.viewedPatientId);
        assertEquals(801, recipes.get(0).idReceta());
        assertEquals(31, recipes.get(0).idPaciente());
        assertEquals("Ibuprofeno 500 mg", recipes.get(0).medicamentos());
    }

    @Test
    void odontologoConsultaRecetaIndividualVisible() throws Exception {
        FakeRecetaDAO dao = new FakeRecetaDAO();
        RecetaService service = new RecetaService(dao);

        RecetaResumen recipe = service.detail(dentist(), 801);

        assertEquals(5, dao.clinicId);
        assertEquals(44, dao.dentistUserId);
        assertEquals(801, dao.viewedRecipeId);
        assertEquals(31, recipe.idPaciente());
        assertEquals("Ibuprofeno 500 mg", recipe.medicamentos());
        assertEquals("Después de los alimentos", recipe.indicaciones());
    }

    @Test
    void rechazaMedicamentosVacios() {
        RecetaService service = new RecetaService(new FakeRecetaDAO());

        assertThrows(
            IllegalArgumentException.class,
            () -> service.create(dentist(), 31, " ", "Cada 8 horas")
        );
    }

    @Test
    void rechazaIndicacionesVacias() {
        RecetaService service = new RecetaService(new FakeRecetaDAO());

        assertThrows(
            IllegalArgumentException.class,
            () -> service.create(dentist(), 31, "Ibuprofeno 400 mg", " ")
        );
    }

    @Test
    void administradorNoPuedeEmitirReceta() {
        RecetaService service = new RecetaService(new FakeRecetaDAO());

        assertThrows(
            IllegalArgumentException.class,
            () -> service.create(admin(), 31, "Ibuprofeno 400 mg", "Cada 8 horas")
        );
    }

    private static User dentist() {
        return new User(44, 5, "Carlos", "Clínica", Role.ODONTOLOGO);
    }

    private static User admin() {
        return new User(1, 5, "Admin", "Clínica", Role.ADMINISTRADOR);
    }

    private static final class FakeRecetaDAO implements RecetaCrudDAO {
        long clinicId;
        long dentistUserId;
        long patientId;
        long viewedPatientId;
        long viewedRecipeId;
        String medications;
        String indications;

        @Override
        public List<RecetaResumen> findByDentist(long clinicId, long dentistUserId) {
            return List.of();
        }

        @Override
        public List<RecetaResumen> findByPatient(
            long clinicId,
            long dentistUserId,
            long patientId
        ) {
            this.clinicId = clinicId;
            this.dentistUserId = dentistUserId;
            this.viewedPatientId = patientId;
            return List.of(new RecetaResumen(
                801,
                31,
                "2026-11-19 20:00",
                "Ana García",
                "Ibuprofeno 500 mg",
                "Después de los alimentos"
            ));
        }

        @Override
        public RecetaResumen findById(
            long clinicId,
            long dentistUserId,
            long recipeId
        ) {
            this.clinicId = clinicId;
            this.dentistUserId = dentistUserId;
            this.viewedRecipeId = recipeId;
            return new RecetaResumen(
                801,
                31,
                "2026-11-19 20:00",
                "Ana García",
                "Ibuprofeno 500 mg",
                "Después de los alimentos"
            );
        }

        @Override
        public List<PacienteOpcion> findPatientsByDentist(long clinicId, long dentistUserId) {
            return List.of(new PacienteOpcion(31, "Paciente Demo", "12345678", "ACTIVO"));
        }

        @Override
        public long create(
            long clinicId,
            long dentistUserId,
            long patientId,
            String medications,
            String indications
        ) {
            this.clinicId = clinicId;
            this.dentistUserId = dentistUserId;
            this.patientId = patientId;
            this.medications = medications;
            this.indications = indications;
            return 801;
        }
    }
}
