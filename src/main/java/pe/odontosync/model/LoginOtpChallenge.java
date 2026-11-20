package pe.odontosync.model;

import java.io.Serializable;
import java.time.Instant;
import pe.odontosync.User;

public record LoginOtpChallenge(
    User user,
    String email,
    String salt,
    String codeHash,
    Instant issuedAt,
    Instant expiresAt,
    Instant resendAvailableAt,
    int failedAttempts
) implements Serializable {
    public LoginOtpChallenge withFailedAttempts(int attempts) {
        return new LoginOtpChallenge(
            user, email, salt, codeHash, issuedAt,
            expiresAt, resendAvailableAt, attempts
        );
    }
}
