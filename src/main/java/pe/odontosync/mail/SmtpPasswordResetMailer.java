package pe.odontosync.mail;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import pe.odontosync.model.PasswordResetAccount;
import pe.odontosync.service.PasswordResetDeliveryException;
import pe.odontosync.service.PasswordResetMailer;

/** Entrega enlaces de recuperacion por SMTP. Admite SMTP local sin autenticacion. */
public final class SmtpPasswordResetMailer implements PasswordResetMailer {
    @Override
    public void sendResetLink(PasswordResetAccount account, String resetLink)
        throws PasswordResetDeliveryException {
        String host = required("ODONTOSYNC_SMTP_HOST");
        int port = intEnv("ODONTOSYNC_SMTP_PORT", 1025);
        boolean auth = booleanEnv("ODONTOSYNC_SMTP_AUTH", false);
        boolean startTls = booleanEnv("ODONTOSYNC_SMTP_STARTTLS", false);
        String user = optional("ODONTOSYNC_SMTP_USER", "");
        String password = optional("ODONTOSYNC_SMTP_PASSWORD", "");
        String from = optional("ODONTOSYNC_SMTP_FROM", "no-reply@odontosync.test");

        if (auth && (user.isBlank() || password.isBlank())) {
            throw new PasswordResetDeliveryException(
                "SMTP requiere ODONTOSYNC_SMTP_USER y ODONTOSYNC_SMTP_PASSWORD."
            );
        }

        Properties props = new Properties();
        props.setProperty("mail.smtp.host", host);
        props.setProperty("mail.smtp.port", Integer.toString(port));
        props.setProperty("mail.smtp.auth", Boolean.toString(auth));
        props.setProperty("mail.smtp.starttls.enable", Boolean.toString(startTls));
        props.setProperty("mail.smtp.starttls.required", Boolean.toString(startTls));
        props.setProperty("mail.smtp.connectiontimeout", "8000");
        props.setProperty("mail.smtp.timeout", "8000");
        props.setProperty("mail.smtp.writetimeout", "8000");

        Session session = auth
            ? Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, password);
                }
            })
            : Session.getInstance(props);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from, "OdontoSync", StandardCharsets.UTF_8.name()));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(account.email()));
            message.setSubject("Restablece tu contrasena de OdontoSync", StandardCharsets.UTF_8.name());
            message.setText(body(account, resetLink), StandardCharsets.UTF_8.name());
            Transport.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
            throw new PasswordResetDeliveryException(
                "No se pudo entregar el correo de recuperacion.", ex
            );
        }
    }

    private String body(PasswordResetAccount account, String resetLink) {
        return "Hola " + safeName(account.firstName()) + ",\n\n"
            + "Se solicito restablecer la contrasena de tu cuenta de OdontoSync"
            + " en " + safeName(account.clinicName()) + ".\n\n"
            + "Abre este enlace dentro de los proximos 15 minutos:\n"
            + resetLink + "\n\n"
            + "El enlace solo puede utilizarse una vez.\n"
            + "Si no solicitaste este cambio, ignora este mensaje.\n\n"
            + "OdontoSync";
    }

    private String safeName(String value) {
        if (value == null || value.isBlank()) return "usuario";
        return value.replaceAll("[\\r\\n\\t]", " ").trim();
    }

    private String required(String name) throws PasswordResetDeliveryException {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new PasswordResetDeliveryException("Falta configurar " + name + ".");
        }
        return value.trim();
    }

    private String optional(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int intEnv(String name, int fallback) throws PasswordResetDeliveryException {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) return fallback;
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 1 || parsed > 65535) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new PasswordResetDeliveryException(name + " no contiene un puerto valido.");
        }
    }

    private boolean booleanEnv(String name, boolean fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value.trim());
    }
}
