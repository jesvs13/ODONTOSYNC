package pe.odontosync.security;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.odontosync.User;

/**
 * Registro de eventos importantes de seguridad y negocio.
 * Nunca registra contraseñas, hashes, tokens CSRF ni contenido clínico sensible.
 */
public final class AuditLog {
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private AuditLog() { }

    public static void loginSuccess(User user, String remoteAddress) {
        if (user == null) return;
        AUDIT.info("event=LOGIN_SUCCESS userId={} clinicId={} role={} ip={}",
            user.id(), user.clinicId(), user.role(), safe(remoteAddress));
    }

    public static void loginFailure(String email, String remoteAddress) {
        AUDIT.warn("event=LOGIN_FAILURE email={} ip={}", safeEmail(email), safe(remoteAddress));
    }

    public static void loginBlocked(String email, String remoteAddress) {
        AUDIT.warn("event=LOGIN_RATE_LIMITED email={} ip={}", safeEmail(email), safe(remoteAddress));
    }

    public static void sessionRequired(String path, String remoteAddress) {
        AUDIT.warn("event=ACCESS_WITHOUT_SESSION path={} ip={}", safe(path), safe(remoteAddress));
    }

    public static void clinicalAttentionSaved(User user, long appointmentId, long consultationId) {
        if (user == null) return;
        AUDIT.info("event=CLINICAL_ATTENTION_SAVED userId={} clinicId={} appointmentId={} consultationId={}",
            user.id(), user.clinicId(), appointmentId, consultationId);
    }

    public static void reportExported(User user, String reportName) {
        if (user == null) return;
        AUDIT.info("event=REPORT_EXPORTED userId={} clinicId={} report={}",
            user.id(), user.clinicId(), safe(reportName));
    }

    public static void businessChange(User user, String action, String entity, long entityId) {
        if (user == null) return;
        AUDIT.info("event=BUSINESS_CHANGE userId={} clinicId={} role={} action={} entity={} entityId={}",
            user.id(), user.clinicId(), user.role(), safe(action), safe(entity), entityId);
    }

    public static void accessDenied(User user, String path) {
        if (user == null) return;
        AUDIT.warn("event=ACCESS_DENIED userId={} clinicId={} role={} path={}",
            user.id(), user.clinicId(), user.role(), safe(path));
    }

    private static String safe(String value) {
        return StringUtils.abbreviate(StringUtils.defaultString(value).replaceAll("[\\r\\n\\t]", "_"), 120);
    }

    private static String safeEmail(String email) {
        String value = StringUtils.lowerCase(StringUtils.trimToEmpty(email))
            .replaceAll("[\\r\\n\\t]", "_");
        int at = value.indexOf('@');
        if (at <= 0) return "***";
        return value.substring(0, 1) + "***" + value.substring(at);
    }
}
