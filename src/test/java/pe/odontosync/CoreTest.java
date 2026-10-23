package pe.odontosync;

import pe.odontosync.security.Passwords;
public final class CoreTest {
    public static void main(String[] args) {
        check(Role.fromDatabase("ADMINISTRADOR") == Role.ADMINISTRADOR, "Rol admin");
        check(Role.fromDatabase("RECEPCIONISTA") == Role.RECEPCIONISTA, "Rol recepción");
        check(Role.fromDatabase("ODONTOLOGO") == Role.ODONTOLOGO, "Rol odontólogo");
        check(Role.fromDatabase("PACIENTE") == null, "Rol no autorizado");
        check(Role.ADMINISTRADOR.allows("usuarios") && !Role.ODONTOLOGO.allows("usuarios"), "Usuarios");
        check(Role.RECEPCIONISTA.allows("pagos") && !Role.ODONTOLOGO.allows("pagos"), "Pagos");
        check(Role.ODONTOLOGO.allows("historias") && !Role.RECEPCIONISTA.allows("historias"), "Historias");
        check(Passwords.matches("OdontoAdmin26!".toCharArray(),
            "pbkdf2_sha256$310000$2WQz3TgkJq8dabYf5ylFxA==$ZVYDVwH1S8qZjNP8qI9Hv/HRJ7WXyNOOagpB2pDkS2c="), "Clave SQL");
        check(Passwords.matches("OdontoRecep26!".toCharArray(),
            "pbkdf2_sha256$310000$1FbWcWUn6+HdHN9IKb8P0g==$vbJ6Qz7fGSgvGcHhGmFTnrxbpdlN1p0EkvAUKx4Sfto="), "Clave recepción");
        check(Passwords.matches("OdontoDent26!".toCharArray(),
            "pbkdf2_sha256$310000$YrhP+AvVkz+W19oowM6GNw==$xU/RLCQ76xm0hBXtxPtelVucZ6f00+diwA3i04BFzIA="), "Clave odontólogo");
        check(!Passwords.matches("otra".toCharArray(),
            "pbkdf2_sha256$310000$2WQz3TgkJq8dabYf5ylFxA==$ZVYDVwH1S8qZjNP8qI9Hv/HRJ7WXyNOOagpB2pDkS2c="), "Clave errónea");
        check(Html.escape("<script>").equals("&lt;script&gt;"), "HTML escapado");
        check(Role.ODONTOLOGO.allows("placas") && !Role.RECEPCIONISTA.allows("placas"), "Placas por rol");
        System.out.println("OK: roles, permisos, hash SQL y HTML.");
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
