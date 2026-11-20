package pe.odontosync.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import pe.odontosync.Role;
import pe.odontosync.User;
import pe.odontosync.model.LoginOtpChallenge;
import pe.odontosync.model.LoginOtpStatus;
import pe.odontosync.model.LoginOtpVerification;

class LoginOtpServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final User USER =
        new User(7L, 3L, "Ana Torres", "Clínica Sonrisa", Role.ADMINISTRADOR);

    @Test
    void emiteCodigoDeSeisDigitosSinGuardarloEnTextoPlano() throws Exception {
        FakeMailer mailer = new FakeMailer();
        LoginOtpService service = service(mailer, () -> "482731", CLOCK);

        LoginOtpChallenge challenge = service.issue(USER, "ana@clinica.pe");

        assertEquals("482731", mailer.code);
        assertEquals("ana@clinica.pe", mailer.email);
        assertEquals(Duration.ofMinutes(5), mailer.validity);
        assertNotEquals("482731", challenge.codeHash());
        assertFalse(challenge.codeHash().contains("482731"));
        assertEquals(NOW.plus(Duration.ofMinutes(5)), challenge.expiresAt());
        assertEquals(NOW.plus(Duration.ofSeconds(60)), challenge.resendAvailableAt());
        assertEquals(0, challenge.failedAttempts());
    }

    @Test
    void aceptaCodigoCorrecto() throws Exception {
        LoginOtpService service = service(new FakeMailer(), () -> "482731", CLOCK);
        LoginOtpChallenge challenge = service.issue(USER, "ana@clinica.pe");

        LoginOtpVerification result = service.verify(challenge, "482731");

        assertEquals(LoginOtpStatus.VERIFIED, result.status());
        assertSame(challenge, result.challenge());
    }

    @Test
    void incrementaIntentosYBloqueaEnElQuintoFallo() throws Exception {
        LoginOtpService service = service(new FakeMailer(), () -> "482731", CLOCK);
        LoginOtpChallenge challenge = service.issue(USER, "ana@clinica.pe");

        for (int i = 1; i <= 4; i++) {
            LoginOtpVerification result = service.verify(challenge, "111111");
            assertEquals(LoginOtpStatus.INVALID, result.status());
            challenge = result.challenge();
            assertEquals(i, challenge.failedAttempts());
        }

        LoginOtpVerification blocked = service.verify(challenge, "111111");
        assertEquals(LoginOtpStatus.LOCKED, blocked.status());
        assertEquals(5, blocked.challenge().failedAttempts());
    }

    @Test
    void rechazaCodigoVencido() throws Exception {
        FakeMailer mailer = new FakeMailer();
        LoginOtpService issueService = service(mailer, () -> "482731", CLOCK);
        LoginOtpChallenge challenge = issueService.issue(USER, "ana@clinica.pe");

        Clock later = Clock.fixed(NOW.plus(Duration.ofMinutes(6)), ZoneOffset.UTC);
        LoginOtpService verifyService = service(mailer, () -> "999999", later);

        LoginOtpVerification result = verifyService.verify(challenge, "482731");

        assertEquals(LoginOtpStatus.EXPIRED, result.status());
    }

    @Test
    void limitaReenvioAntesDeSesentaSegundos() throws Exception {
        FakeMailer mailer = new FakeMailer();
        LoginOtpService service = service(mailer, () -> "482731", CLOCK);
        LoginOtpChallenge challenge = service.issue(USER, "ana@clinica.pe");

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> service.resend(challenge)
        );

        assertTrue(error.getMessage().contains("60"));
        assertEquals(1, mailer.deliveries);
    }

    @Test
    void permiteReenvioDespuesDeSesentaSegundosEInvalidaCodigoAnterior() throws Exception {
        FakeMailer mailer = new FakeMailer();
        LoginOtpService first = service(mailer, () -> "482731", CLOCK);
        LoginOtpChallenge original = first.issue(USER, "ana@clinica.pe");

        Clock later = Clock.fixed(NOW.plus(Duration.ofSeconds(61)), ZoneOffset.UTC);
        LoginOtpService resend = service(mailer, () -> "937204", later);

        LoginOtpChallenge replacement = resend.resend(original);

        assertEquals(2, mailer.deliveries);
        assertEquals("937204", mailer.code);
        assertEquals(
            LoginOtpStatus.INVALID,
            resend.verify(replacement, "482731").status()
        );
        assertEquals(
            LoginOtpStatus.VERIFIED,
            resend.verify(replacement, "937204").status()
        );
    }

    private static LoginOtpService service(
        FakeMailer mailer,
        LoginOtpGenerator generator,
        Clock clock
    ) {
        return new LoginOtpService(
            mailer,
            generator,
            clock,
            Duration.ofMinutes(5),
            Duration.ofSeconds(60),
            5
        );
    }

    private static final class FakeMailer implements LoginOtpMailer {
        private String email;
        private String code;
        private Duration validity;
        private int deliveries;

        @Override
        public void sendCode(User user, String email, String code, Duration validity) {
            this.email = email;
            this.code = code;
            this.validity = validity;
            deliveries++;
        }
    }
}
