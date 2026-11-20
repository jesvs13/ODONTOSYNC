package pe.odontosync.service;

import java.time.Duration;
import pe.odontosync.User;

@FunctionalInterface
public interface LoginOtpMailer {
    void sendCode(User user, String email, String code, Duration validity)
        throws LoginOtpDeliveryException;
}
