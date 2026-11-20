package pe.odontosync.model;

import java.math.BigDecimal;

public record DashboardPoint(
    String label,
    BigDecimal value
) { }
