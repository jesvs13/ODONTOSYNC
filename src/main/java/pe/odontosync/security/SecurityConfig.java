package pe.odontosync.security;

/**
 * Configuración de seguridad dependiente del entorno.
 * En desarrollo local HTTPS permanece desactivado; en producción puede activarse
 * con ODONTOSYNC_REQUIRE_HTTPS=true y ODONTOSYNC_HTTPS_PORT (por defecto 8443).
 */
public final class SecurityConfig {
    private SecurityConfig() { }

    public static boolean requireHttps() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("ODONTOSYNC_REQUIRE_HTTPS", "false"));
    }

    public static int httpsPort() {
        String raw = System.getenv().getOrDefault("ODONTOSYNC_HTTPS_PORT", "8443");
        try {
            int port = Integer.parseInt(raw);
            return port >= 1 && port <= 65535 ? port : 8443;
        } catch (NumberFormatException ex) {
            return 8443;
        }
    }
}
