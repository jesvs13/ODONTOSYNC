package pe.odontosync.model;

/** Resumen de existencias para la vista de inventario. */
public record InventarioResumen(String insumo, String categoria, String stock, String minimo, String alerta) { }
