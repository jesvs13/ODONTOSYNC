package pe.odontosync.security;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

/** Cabeceras HTTP estáticas de endurecimiento aplicadas a toda respuesta. */
public final class SecurityHeaders {
    private static final Map<String, String> VALUES = ImmutableMap.<String, String>builder()
        .put("X-Content-Type-Options", "nosniff")
        .put("X-Frame-Options", "DENY")
        .put("Referrer-Policy", "no-referrer")
        .put("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
        .put("Content-Security-Policy",
            "default-src 'none'; script-src 'self'; style-src 'self'; img-src 'self'; "
            + "form-action 'self'; base-uri 'none'; frame-ancestors 'none'; object-src 'none'")
        .build();

    private SecurityHeaders() { }

    public static Map<String, String> values() {
        return VALUES;
    }
}
