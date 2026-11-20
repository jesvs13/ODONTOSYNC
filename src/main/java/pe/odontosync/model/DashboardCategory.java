package pe.odontosync.model;

import java.math.BigDecimal;

public record DashboardCategory(
    String label,
    BigDecimal value
) { }
