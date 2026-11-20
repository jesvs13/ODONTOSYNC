package pe.odontosync.service;

import pe.odontosync.model.PasswordResetAccount;

/** Puerto de salida para entregar el enlace sin acoplar el Service a SMTP. */
public interface PasswordResetMailer {
    void sendResetLink(PasswordResetAccount account, String resetLink)
        throws PasswordResetDeliveryException;
}
