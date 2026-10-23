package pe.odontosync.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centraliza la configuración y apertura de conexiones JDBC.
 * Las credenciales se leen desde variables de entorno y no se guardan en el código.
 */
public final class DatabaseConnection {
    private static final String URL = System.getenv().getOrDefault(
        "ODONTOSYNC_DB_URL",
        "jdbc:mysql://localhost:3306/odontosync?useUnicode=true&characterEncoding=UTF-8"
    );
    private static final String USER = System.getenv().getOrDefault("ODONTOSYNC_DB_USER", "odontosync_app");
    private static final String PASSWORD = System.getenv("ODONTOSYNC_DB_PASSWORD");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new ExceptionInInitializerError("No se encontró el driver JDBC de MySQL: " + ex.getMessage());
        }
    }

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException("Configura ODONTOSYNC_DB_PASSWORD antes de iniciar OdontoSync.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
