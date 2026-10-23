package pe.odontosync;

import pe.odontosync.config.DatabaseConnection;
import pe.odontosync.security.Passwords;
import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;

/** Asigna una clave a una cuenta existente sin imprimirla ni guardarla en el código. */
public final class PasswordAdmin {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Uso: PasswordAdmin <id_clinica> <correo>");
            System.exit(2);
        }
        Console terminal = System.console();
        if (terminal == null) {
            System.err.println("Ejecuta este comando desde una terminal interactiva.");
            System.exit(2);
        }
        char[] first = terminal.readPassword("Nueva contraseña: ");
        char[] second = terminal.readPassword("Repetir contraseña: ");
        try {
            if (first == null || first.length < 10 || !Arrays.equals(first, second)) {
                throw new IllegalArgumentException("Las claves no coinciden o tienen menos de 10 caracteres.");
            }
            String hash = Passwords.hash(first);
            try (Connection cn = DatabaseConnection.getConnection();
                 PreparedStatement ps = cn.prepareStatement("UPDATE usuarios SET password_hash=? WHERE id_clinica=? AND correo=?")) {
                ps.setString(1, hash);
                ps.setLong(2, Long.parseLong(args[0]));
                ps.setString(3, args[1].trim());
                int updated = ps.executeUpdate();
                if (updated != 1) throw new IllegalStateException("No se encontró exactamente una cuenta. Filas: " + updated);
                System.out.println("Contraseña actualizada para " + args[1]);
            }
        } finally {
            if (first != null) Arrays.fill(first, '\0');
            if (second != null) Arrays.fill(second, '\0');
        }
    }
}
