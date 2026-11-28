package pe.odontosync.service;

import java.util.Arrays;

/** Resultado de alta para entrega inmediata. Nunca almacenar en sesión ni registrar en logs. */
public final class CreatedAccount implements AutoCloseable {
    private final long id;
    private final String correo;
    private final char[] temporaryPassword;

    CreatedAccount(long id, String correo, char[] temporaryPassword) {
        this.id = id;
        this.correo = correo;
        this.temporaryPassword = temporaryPassword.clone();
    }

    public long id() { return id; }
    public String correo() { return correo; }
    /** Usar solo durante la respuesta de alta, antes de cerrar el resultado. */
    public char[] temporaryPassword() { return temporaryPassword; }
    @Override public void close() { Arrays.fill(temporaryPassword, '\0'); }
}
