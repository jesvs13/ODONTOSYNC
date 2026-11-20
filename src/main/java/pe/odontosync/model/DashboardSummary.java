package pe.odontosync.model;

import java.math.BigDecimal;

public record DashboardSummary(
    int patients,
    int appointments,
    BigDecimal income,
    double attendanceRate
) { }
