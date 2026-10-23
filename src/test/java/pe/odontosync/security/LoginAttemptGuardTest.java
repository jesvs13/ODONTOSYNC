package pe.odontosync.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class LoginAttemptGuardTest {
    @Test
    void blocksAfterRepeatedFailuresAndSuccessClearsState() {
        LoginAttemptGuard guard = new LoginAttemptGuard();
        String key = guard.key("usuario@odontosync.pe", "127.0.0.1");
        assertTrue(guard.isAllowed(key));
        for (int i = 0; i < 5; i++) guard.recordFailure(key);
        assertFalse(guard.isAllowed(key));
        guard.recordSuccess(key);
        assertTrue(guard.isAllowed(key));
    }
}
