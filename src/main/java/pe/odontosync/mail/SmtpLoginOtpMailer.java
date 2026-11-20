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
import java.time.Duration;
import java.util.Properties;
import pe.odontosync.User;
import pe.odontosync.service.LoginOtpDeliveryException;
import pe.odontosync.service.LoginOtpMailer;

/** Entrega el segundo factor de inicio de sesión usando el SMTP ya configurado. */
public final class SmtpLoginOtpMailer implements LoginOtpMailer {
    @Override
    public void sendCode(User userAccount, String email, String code, Duration validity)
        throws LoginOtpDeliveryException {
        String host = required("ODONTOSYNC_SMTP_HOST");
        int port = intEnv("ODONTOSYNC_SMTP_PORT", 1025);
        boolean auth = booleanEnv("ODONTOSYNC_SMTP_AUTH", false);
        boolean startTls = booleanEnv("ODONTOSYNC_SMTP_STARTTLS", false);
        String smtpUser = optional("ODONTOSYNC_SMTP_USER", "");
        String password = optional("ODONTOSYNC_SMTP_PASSWORD", "");
        String from = optional("ODONTOSYNC_SMTP_FROM", "no-reply@odontosync.test");

        if (auth && (smtpUser.isBlank() || password.isBlank())) {
            throw new LoginOtpDeliveryException(
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
                    return new PasswordAuthentication(smtpUser, password);
                }
            })
            : Session.getInstance(props);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(
                new InternetAddress(from, "OdontoSync", StandardCharsets.UTF_8.name())
            );
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
            message.setSubject(
                "Código de seguridad de OdontoSync",
                StandardCharsets.UTF_8.name()
            );
            message.setText(body(userAccount, code, validity), StandardCharsets.UTF_8.name());
            Transport.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
            throw new LoginOtpDeliveryException(
                "No se pudo entregar el código de seguridad.", ex
            );
        }
    }

    private String body(User user, String code, Duration validity) {
        long minutes = Math.max(1L, validity.toMinutes());
        return "Hola " + safe(user.name()) + ",\n\n"
            + "Se está iniciando sesión en tu cuenta de OdontoSync"
            + " para " + safe(user.clinicName()) + ".\n\n"
            + "Tu código de seguridad es:\n\n"
            + code + "\n\n"
            + "El código vence en " + minutes + " minutos y solo debe ser usado por ti.\n"
            + "Si no intentaste iniciar sesión, ignora este mensaje y no compartas el código.\n";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "usuario" : value.trim();
    }

    private String required(String name) throws LoginOtpDeliveryException {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new LoginOtpDeliveryException("Configura " + name + ".");
        }
        return value.trim();
    }

    private String optional(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int intEnv(String name, int fallback) throws LoginOtpDeliveryException {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) return fallback;
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 1 || parsed > 65535) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new LoginOtpDeliveryException(name + " no contiene un puerto válido.");
        }
    }

    private boolean booleanEnv(String name, boolean fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank()
            ? fallback
            : Boolean.parseBoolean(value.trim());
    }
}
