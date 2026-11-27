package pe.odontosync.config;

import java.nio.file.Path;
import java.time.Clock;
import pe.odontosync.dao.JdbcPlacaCrudDAO;
import pe.odontosync.service.PlacaService;

/** Almacenamiento persistente externo al WAR. */
public final class RadiografiaConfig {
    private RadiografiaConfig() { }

    public static PlacaService service() {
        String directory = System.getProperty("odontosync.radiografias.dir",
            System.getenv().getOrDefault("ODONTOSYNC_RADIOGRAFIAS_DIR",
                "C:\\OdontoSyncStorage\\radiografias"));
        Path path = Path.of(directory);
        if (!path.isAbsolute()) {
            throw new IllegalStateException("Configura una ruta absoluta de almacenamiento radiografico.");
        }
        return new PlacaService(new JdbcPlacaCrudDAO(), path, Clock.systemDefaultZone());
    }
}
