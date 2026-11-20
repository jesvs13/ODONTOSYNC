package pe.odontosync.model;

public record LoginOtpVerification(
    LoginOtpStatus status,
    LoginOtpChallenge challenge
) { }
