package pe.odontosync.service;

public final class PasswordResetDeliveryException extends Exception {
    public PasswordResetDeliveryException(String message) {
        super(message);
    }

    public PasswordResetDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
