package pe.odontosync.service;

public final class LoginOtpDeliveryException extends Exception {
    public LoginOtpDeliveryException(String message) {
        super(message);
    }

    public LoginOtpDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
