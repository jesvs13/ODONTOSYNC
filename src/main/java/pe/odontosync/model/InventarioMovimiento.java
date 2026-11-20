package pe.odontosync.model;

import java.math.BigDecimal;

public record InventarioMovimiento(
    long id,
    long insumoId,
    String insumo,
    String tipo,
    BigDecimal cantidad,
    String motivo,
    String fecha,
    String usuario
) { }
