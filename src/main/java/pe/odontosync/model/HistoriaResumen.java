package pe.odontosync.model;

/** Resumen de una historia clínica visible para el odontólogo. */
public record HistoriaResumen(String paciente, String documento, String apertura, String estado) { }
