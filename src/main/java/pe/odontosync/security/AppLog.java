package pe.odontosync.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registro técnico centralizado; Logback define destino, rotación y niveles. */
public final class AppLog {
    private static final Logger LOG = LoggerFactory.getLogger("pe.odontosync.application");

    private AppLog() { }

    public static void trace(String message, Object... args) {
        LOG.trace(message, args);
    }

    public static void debug(String message, Object... args) {
        LOG.debug(message, args);
    }

    public static void info(String message, Object... args) {
        LOG.info(message, args);
    }

    public static void warn(String message, Object... args) {
        LOG.warn(message, args);
    }

    public static void error(String message, Throwable error) {
        LOG.error(message, error);
    }
}
