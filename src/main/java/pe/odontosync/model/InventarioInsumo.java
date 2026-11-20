package pe.odontosync.model;

import java.math.BigDecimal;

public record InventarioInsumo(
    long id,
    long categoriaId,
    String categoria,
    String nombre,
    String descripcion,
    String unidadMedida,
    BigDecimal stockActual,
    BigDecimal stockMinimo,
    BigDecimal precioReferencial,
    String estado
) {
    public boolean stockBajo() {
        return stockActual != null && stockMinimo != null
            && stockActual.compareTo(stockMinimo) <= 0;
    }

    public boolean sinStock() {
        return stockActual != null && stockActual.compareTo(BigDecimal.ZERO) == 0;
    }
}
