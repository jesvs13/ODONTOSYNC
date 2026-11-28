package pe.odontosync.service;

import java.sql.SQLException;
import java.util.Arrays;
import pe.odontosync.security.Passwords;

/** Centraliza la generación, el hash y la limpieza de credenciales temporales. */
final class TemporaryAccountCredentials {
    private final SecureTemporaryPasswordGenerator passwords = new SecureTemporaryPasswordGenerator();

    @FunctionalInterface
    interface AccountWrite {
        long create(String email, String hash) throws SQLException;
    }

    @FunctionalInterface
    interface ResetWrite {
        void reset(String hash) throws SQLException;
    }

    CreatedAccount create(CorreoInstitucionalService emails, String nombres, String primerApellido,
        String segundoApellido, AccountWrite write) throws SQLException {
        return issue(hash -> emails.createWithAvailableEmail(nombres, primerApellido, segundoApellido,
            email -> new AccountIdentity(write.create(email, hash), email)));
    }

    CreatedAccount reset(long id, String email, ResetWrite write) throws SQLException {
        return issue(hash -> {
            write.reset(hash);
            return new AccountIdentity(id, email);
        });
    }

    private CreatedAccount issue(CredentialWrite write) throws SQLException {
        char[] password = passwords.generate();
        try {
            AccountIdentity account = write.persist(Passwords.hash(password));
            return new CreatedAccount(account.id(), account.email(), password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    @FunctionalInterface
    private interface CredentialWrite {
        AccountIdentity persist(String hash) throws SQLException;
    }

    private record AccountIdentity(long id, String email) { }
}
