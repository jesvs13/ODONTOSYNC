package pe.odontosync.dao;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import pe.odontosync.model.DashboardCategory;
import pe.odontosync.model.DashboardPoint;
import pe.odontosync.model.DashboardSummary;

public interface DashboardAnalyticsDAO {

    DashboardSummary summary(
        long clinicId,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) throws SQLException;

    List<DashboardPoint> appointmentsTrend(
        long clinicId,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) throws SQLException;

    List<DashboardCategory> appointmentStatus(
        long clinicId,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) throws SQLException;

    List<DashboardPoint> incomeTrend(
        long clinicId,
        LocalDate from,
        LocalDate to,
        Long dentistUserId
    ) throws SQLException;

    List<DashboardPoint> newPatientsTrend(
        long clinicId,
        LocalDate from,
        LocalDate to
    ) throws SQLException;

    List<DashboardCategory> attentionsByDentist(
        long clinicId,
        LocalDate from,
        LocalDate to
    ) throws SQLException;

    List<DashboardCategory> inventoryStatus(long clinicId)
        throws SQLException;

    List<DashboardCategory> paymentMethods(
        long clinicId,
        LocalDate from,
        LocalDate to
    ) throws SQLException;
}
