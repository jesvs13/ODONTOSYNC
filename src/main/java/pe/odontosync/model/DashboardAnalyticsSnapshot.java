package pe.odontosync.model;

import java.util.List;

public record DashboardAnalyticsSnapshot(
    DashboardSummary summary,
    List<DashboardPoint> appointmentsTrend,
    List<DashboardCategory> appointmentStatus,
    List<DashboardPoint> incomeTrend,
    List<DashboardPoint> newPatientsTrend,
    List<DashboardCategory> attentionsByDentist,
    List<DashboardCategory> inventoryStatus,
    List<DashboardCategory> paymentMethods
) { }
