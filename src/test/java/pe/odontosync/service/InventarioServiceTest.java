package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.dao.InventarioCrudDAO;
import pe.odontosync.model.InventarioCategoria;
import pe.odontosync.model.InventarioInsumo;
import pe.odontosync.model.InventarioMovimiento;
import pe.odontosync.model.InventarioResumen;

class InventarioServiceTest {
    @Test
    void administradorRegistraInsumoNormalizado() throws Exception {
        FakeDao dao = new FakeDao();
        InventarioService service = new InventarioService(dao);

        long id = service.create(
            admin(), 3, "  Resina A2  ", "  Resina fotocurable  ", "unidad",
            "5.00", "28.50", "activo"
        );

        assertEquals(70L, id);
        assertEquals(8L, dao.clinicId);
        assertEquals(3L, dao.categoryId);
        assertEquals("Resina A2", dao.name);
        assertEquals("Resina fotocurable", dao.description);
        assertEquals("unidad", dao.unit);
        assertEquals(new BigDecimal("5.00"), dao.minimumStock);
        assertEquals(new BigDecimal("28.50"), dao.referencePrice);
        assertEquals("ACTIVO", dao.status);
    }

    @Test
    void editarInsumoNoRecibeStockDirecto() throws Exception {
        FakeDao dao = new FakeDao();
        dao.items.add(sampleItem());
        InventarioService service = new InventarioService(dao);

        service.update(
            admin(), 15, 3, "Resina A2", "Nueva descripción", "unidad",
            "6.00", "30.00", "ACTIVO"
        );

        assertEquals(15L, dao.itemId);
        assertEquals(new BigDecimal("6.00"), dao.minimumStock);
        assertEquals(new BigDecimal("30.00"), dao.referencePrice);
    }

    @Test
    void recepcionistaNoPuedeGestionarInventario() {
        InventarioService service = new InventarioService(new FakeDao());
        assertThrows(IllegalArgumentException.class, () -> service.list(reception()));
        assertThrows(IllegalArgumentException.class, () -> service.create(
            reception(), 1, "Guantes", "", "caja", "2", "", "ACTIVO"
        ));
    }

    @Test
    void noPermiteNombreDuplicadoEnLaMismaClinica() {
        FakeDao dao = new FakeDao();
        dao.duplicateName = true;
        InventarioService service = new InventarioService(dao);

        assertThrows(IllegalArgumentException.class, () -> service.create(
            admin(), 1, "Guantes", "", "caja", "2", "", "ACTIVO"
        ));
    }

    @Test
    void entradaRegistraMovimientoConUsuarioYClinica() throws Exception {
        FakeDao dao = new FakeDao();
        dao.items.add(sampleItem());
        InventarioService service = new InventarioService(dao);

        long id = service.registerMovement(admin(), 15, "entrada", "4.50", "Compra mensual");

        assertEquals(91L, id);
        assertEquals(8L, dao.clinicId);
        assertEquals(4L, dao.userId);
        assertEquals(15L, dao.itemId);
        assertEquals("ENTRADA", dao.movementType);
        assertEquals(new BigDecimal("4.50"), dao.movementQuantity);
        assertEquals("Compra mensual", dao.movementReason);
    }

    @Test
    void ajusteRepresentaElStockFinal() throws Exception {
        FakeDao dao = new FakeDao();
        dao.items.add(sampleItem());
        InventarioService service = new InventarioService(dao);

        service.registerMovement(admin(), 15, "AJUSTE", "12", "Conteo físico");

        assertEquals("AJUSTE", dao.movementType);
        assertEquals(new BigDecimal("12.00"), dao.movementQuantity);
    }

    @Test
    void movimientoRechazaCantidadCero() {
        InventarioService service = new InventarioService(new FakeDao());
        assertThrows(IllegalArgumentException.class,
            () -> service.registerMovement(admin(), 15, "SALIDA", "0", "Consumo"));
    }

    @Test
    void errorDeStockDelDaoSeExponeComoValidacionDeNegocio() {
        FakeDao dao = new FakeDao();
        dao.movementFailure = "Stock insuficiente para realizar la salida.";
        InventarioService service = new InventarioService(dao);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.registerMovement(admin(), 15, "SALIDA", "50", "Consumo clínico"));
        assertEquals("Stock insuficiente para realizar la salida.", ex.getMessage());
    }

    @Test
    void stockBajoSeCalculaDesdeStockActualYMinimo() {
        InventarioInsumo item = sampleItem();
        assertTrue(item.stockBajo());
        assertFalse(item.sinStock());
    }

    @Test
    void historialQuedaLimitadoALaClinicaDeLaSesion() throws Exception {
        FakeDao dao = new FakeDao();
        InventarioService service = new InventarioService(dao);
        service.history(admin());
        assertEquals(8L, dao.historyClinicId);
    }

    private static User admin() {
        return new User(4, 8, "Administrador", "Clínica", Role.ADMINISTRADOR);
    }

    private static User reception() {
        return new User(5, 8, "Recepción", "Clínica", Role.RECEPCIONISTA);
    }

    private static InventarioInsumo sampleItem() {
        return new InventarioInsumo(
            15, 3, "Restauración", "Resina A2", "Resina fotocurable", "unidad",
            new BigDecimal("3.00"), new BigDecimal("5.00"), new BigDecimal("28.50"), "ACTIVO"
        );
    }

    private static final class FakeDao implements InventarioCrudDAO {
        final List<InventarioInsumo> items = new ArrayList<>();
        boolean duplicateName;
        String movementFailure;
        long clinicId, categoryId, itemId, userId, historyClinicId;
        String name, description, unit, status, movementType, movementReason;
        BigDecimal minimumStock, referencePrice, movementQuantity;

        @Override
        public List<InventarioResumen> findByClinic(long clinicId) { return List.of(); }

        @Override
        public List<InventarioInsumo> findDetailedByClinic(long clinicId) {
            this.clinicId = clinicId;
            return items;
        }

        @Override
        public Optional<InventarioInsumo> findDetailedById(long clinicId, long itemId) {
            return items.stream().filter(x -> x.id() == itemId).findFirst();
        }

        @Override
        public List<InventarioCategoria> findActiveCategories() {
            return List.of(new InventarioCategoria(3, "Restauración"));
        }

        @Override
        public boolean nameExists(long clinicId, String name, Long excludeId) {
            return duplicateName;
        }

        @Override
        public long create(
            long clinicId, long categoryId, String name, String description, String unit,
            BigDecimal minimumStock, BigDecimal referencePrice, String status
        ) {
            this.clinicId = clinicId;
            this.categoryId = categoryId;
            this.name = name;
            this.description = description;
            this.unit = unit;
            this.minimumStock = minimumStock;
            this.referencePrice = referencePrice;
            this.status = status;
            return 70L;
        }

        @Override
        public void update(
            long clinicId, long itemId, long categoryId, String name, String description,
            String unit, BigDecimal minimumStock, BigDecimal referencePrice, String status
        ) {
            this.clinicId = clinicId;
            this.itemId = itemId;
            this.categoryId = categoryId;
            this.name = name;
            this.description = description;
            this.unit = unit;
            this.minimumStock = minimumStock;
            this.referencePrice = referencePrice;
            this.status = status;
        }

        @Override
        public void updateStatus(long clinicId, long itemId, String status) {
            this.clinicId = clinicId;
            this.itemId = itemId;
            this.status = status;
        }

        @Override
        public List<InventarioMovimiento> findMovementsByClinic(long clinicId) {
            historyClinicId = clinicId;
            return List.of();
        }

        @Override
        public long createMovement(
            long clinicId, long userId, long itemId, String type,
            BigDecimal quantity, String reason
        ) {
            if (movementFailure != null) throw new IllegalStateException(movementFailure);
            this.clinicId = clinicId;
            this.userId = userId;
            this.itemId = itemId;
            this.movementType = type;
            this.movementQuantity = quantity;
            this.movementReason = reason;
            return 91L;
        }
    }
}
