package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.EspecialidadCrudDAO;
import pe.odontosync.model.EspecialidadCatalogo;

/** Contrato RED: catalogo global y propio, con disponibilidad por clinica. */
class EspecialidadServiceTest {
    private final FakeDao dao = new FakeDao();
    private final EspecialidadService service = new EspecialidadService(dao);

    @Test
    void consultaCatalogoDeLaClinicaDelUsuario() throws Exception {
        assertEquals(List.of(1L, 2L, 10L), ids(service.list(user(1, Role.ADMINISTRADOR))));
        assertEquals(List.of(1L, 2L, 20L), ids(service.list(user(2, Role.ADMINISTRADOR))));
    }

    @Test
    void losTresRolesPuedenConsultarElCatalogo() throws Exception {
        for (Role role : Role.values()) {
            assertEquals(List.of(1L, 2L, 10L), ids(service.list(user(1, role))));
        }
    }

    @Test
    void noConsultaSinUsuarioAutenticado() {
        assertThrows(IllegalArgumentException.class, () -> service.list(null));
    }

    @Test
    void soloAdministradorPuedeGestionar() {
        for (Role role : List.of(Role.RECEPCIONISTA, Role.ODONTOLOGO)) {
            User actor = user(1, role);
            assertThrows(IllegalArgumentException.class, () -> service.create(actor, "Cirugia oral"));
            assertThrows(IllegalArgumentException.class, () -> service.rename(actor, 10, "Nuevo nombre"));
            assertThrows(IllegalArgumentException.class, () -> service.setActive(actor, 1, false));
        }
        assertEquals(0, dao.writes);
    }

    @Test
    void creaEspecialidadPropiaActivaNormalizandoEspacios() throws Exception {
        long id = service.create(user(1, Role.ADMINISTRADOR), "  Cirugía   oral  ");
        EspecialidadCatalogo created = byId(service.list(user(1, Role.ADMINISTRADOR)), id);
        assertEquals(Long.valueOf(1L), created.clinicId());
        assertEquals("Cirugía oral", created.nombre());
        assertTrue(created.active());
        assertFalse(ids(service.list(user(2, Role.ADMINISTRADOR))).contains(id));
    }

    @Test
    void rechazaNombreVacioOLongitudMayorAlCampo() {
        User admin = user(1, Role.ADMINISTRADOR);
        for (String name : new String[]{null, "", "   ", "a".repeat(101)}) {
            assertThrows(IllegalArgumentException.class, () -> service.create(admin, name));
            assertThrows(IllegalArgumentException.class, () -> service.rename(admin, 10, name));
        }
        assertEquals(0, dao.writes);
    }

    @Test
    void rechazaDuplicadoGlobalAunqueEsteInactivoEnLaClinica() throws Exception {
        User admin = user(1, Role.ADMINISTRADOR);
        service.setActive(admin, 1, false);
        assertThrows(IllegalArgumentException.class, () -> service.create(admin, "  ODONTOLOGIA  GENERAL "));
        assertEquals(1, dao.writes);
    }

    @Test
    void rechazaDuplicadoPropioAunqueEsteInactivo() throws Exception {
        User admin = user(1, Role.ADMINISTRADOR);
        service.setActive(admin, 10, false);
        assertThrows(IllegalArgumentException.class, () -> service.create(admin, "ESTETICA DENTAL"));
        assertEquals(1, dao.writes);
    }

    @Test
    void permiteMismoNombrePropioEnClinicasDiferentes() throws Exception {
        long id = service.create(user(2, Role.ADMINISTRADOR), "Estética dental");
        assertEquals(Long.valueOf(2L), byId(service.list(user(2, Role.ADMINISTRADOR)), id).clinicId());
        assertFalse(ids(service.list(user(1, Role.ADMINISTRADOR))).contains(id));
    }

    @Test
    void modificaNombrePropioConservandoIdentificadorYEstado() throws Exception {
        User admin = user(1, Role.ADMINISTRADOR);
        service.setActive(admin, 10, false);
        service.rename(admin, 10, "  Odontología  estética ");
        EspecialidadCatalogo updated = byId(service.list(admin), 10);
        assertEquals("Odontología estética", updated.nombre());
        assertEquals(Long.valueOf(1L), updated.clinicId());
        assertFalse(updated.active());
    }

    @Test
    void renombrarNoDuplicaOtraEspecialidadYPermiteSuPropioNombre() throws Exception {
        User admin = user(1, Role.ADMINISTRADOR);
        assertThrows(IllegalArgumentException.class, () -> service.rename(admin, 10, "ORTODONCIA"));
        assertEquals(0, dao.writes);
        service.rename(admin, 10, "Estética dental");
        assertEquals("Estética dental", byId(service.list(admin), 10).nombre());
    }

    @Test
    void noModificaNombreGlobalNiEspecialidadAjena() {
        User admin = user(1, Role.ADMINISTRADOR);
        assertThrows(IllegalArgumentException.class, () -> service.rename(admin, 1, "Otra"));
        assertThrows(IllegalArgumentException.class, () -> service.rename(admin, 20, "Otra"));
        assertThrows(IllegalArgumentException.class, () -> service.setActive(admin, 20, false));
        assertEquals(0, dao.writes);
    }

    @Test
    void desactivaYReactivaGlobalSoloEnClinicaActual() throws Exception {
        User admin1 = user(1, Role.ADMINISTRADOR);
        User admin2 = user(2, Role.ADMINISTRADOR);
        service.setActive(admin1, 1, false);
        assertFalse(byId(service.list(admin1), 1).active());
        assertTrue(byId(service.list(admin2), 1).active());
        service.setActive(admin1, 1, true);
        assertTrue(byId(service.list(admin1), 1).active());
        assertTrue(byId(service.list(admin2), 1).active());
    }

    @Test
    void inactivacionPropiaEsLogicaYConservaElRegistro() throws Exception {
        User admin = user(1, Role.ADMINISTRADOR);
        service.setActive(admin, 10, false);
        assertEquals("Estética dental", byId(service.list(admin), 10).nombre());
        assertFalse(ids(service.active(admin)).contains(10L));
        service.setActive(admin, 10, true);
        assertTrue(ids(service.active(admin)).contains(10L));
    }

    @Test
    void seleccionActivaRespetaDisponibilidadYAlcance() throws Exception {
        User admin = user(1, Role.ADMINISTRADOR);
        service.setActive(admin, 2, false);
        assertEquals(List.of(1L, 10L), ids(service.active(user(1, Role.RECEPCIONISTA))));
        assertEquals(List.of(1L, 2L, 20L), ids(service.active(user(2, Role.RECEPCIONISTA))));
    }

    @Test
    void rechazaIdentificadoresInexistentes() {
        User admin = user(1, Role.ADMINISTRADOR);
        assertThrows(IllegalArgumentException.class, () -> service.rename(admin, 999, "Nueva"));
        assertThrows(IllegalArgumentException.class, () -> service.setActive(admin, 999, false));
        assertEquals(0, dao.writes);
    }

    private static User user(long clinic, Role role) {
        return new User(100 + clinic, clinic, "Usuario", "Clinica " + clinic, role);
    }

    private static List<Long> ids(List<EspecialidadCatalogo> catalog) {
        return catalog.stream().map(EspecialidadCatalogo::id).toList();
    }

    private static EspecialidadCatalogo byId(List<EspecialidadCatalogo> catalog, long id) {
        return catalog.stream().filter(e -> e.id() == id).findFirst().orElseThrow();
    }

    /** Simula persistencia; las validaciones de negocio corresponden al servicio. */
    private static final class FakeDao implements EspecialidadCrudDAO {
        private final List<EspecialidadCatalogo> rows = new ArrayList<>(List.of(
            new EspecialidadCatalogo(1L, null, "Odontología general", true),
            new EspecialidadCatalogo(2L, null, "Ortodoncia", true),
            new EspecialidadCatalogo(10L, 1L, "Estética dental", true),
            new EspecialidadCatalogo(20L, 2L, "Implantología", true)
        ));
        private record Scope(long clinicId, long id) { }
        private final Map<Scope, Boolean> availability = new HashMap<>();
        private long nextId = 30;
        int writes;

        public List<EspecialidadCatalogo> listByClinic(long clinicId) {
            return rows.stream()
                .filter(e -> e.clinicId() == null || e.clinicId() == clinicId)
                .map(e -> new EspecialidadCatalogo(e.id(), e.clinicId(), e.nombre(),
                    availability.getOrDefault(new Scope(clinicId, e.id()), e.active())))
                .toList();
        }

        public long create(long clinicId, String nombre) {
            writes++;
            long id = nextId++;
            rows.add(new EspecialidadCatalogo(id, clinicId, nombre, true));
            return id;
        }

        public void rename(long clinicId, long id, String nombre) {
            writes++;
            for (int i = 0; i < rows.size(); i++) {
                EspecialidadCatalogo e = rows.get(i);
                if (e.id() == id) {
                    rows.set(i, new EspecialidadCatalogo(id, e.clinicId(), nombre, e.active()));
                    return;
                }
            }
        }

        public void setActive(long clinicId, long id, boolean active) {
            writes++;
            availability.put(new Scope(clinicId, id), active);
        }
    }
}
