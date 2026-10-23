# OdontoSync

Aplicación web académica 100% Java para gestión de una clínica odontológica.

## Stack actual

- Java 17
- Jakarta Servlets + JSP
- MVC + Service + DAO
- JDBC
- MySQL 8
- Maven
- Apache Tomcat 10.1
- JUnit 5

## Semana 10 integrada

- Google Guava aplicado a colecciones inmutables, Multimap y control de intentos de login con CacheBuilder.
- Apache POI para exportar reportes Excel.
- Apache Commons Lang e IO para validación/normalización y manejo de streams.
- SLF4J + Logback para errores, monitoreo y auditoría.
- OWASP Dependency-Check y configuración de SonarQube para revisión de seguridad/calidad.
- Controles de seguridad: validación de entradas, PreparedStatement, CSP, sesiones/roles, CSRF, mínimo privilegio, auditoría y HTTPS activable por entorno.

## Funcionalidad implementada

- Login real contra MySQL.
- Sesión y control de acceso por roles: Administrador, Recepción y Odontólogo.
- Dashboard por rol.
- CRUD funcional de Usuarios.
- CRUD funcional de Pacientes con baja lógica y ficha integral.
- CRUD funcional de Odontólogos con cuenta vinculada, especialidades y baja lógica.
- CRUD funcional de Horarios con prevención de cruces y vistas según rol.
- Agenda/Citas funcional: registro, confirmación, reprogramación, cancelación, estados y validación de disponibilidad.
- Atención clínica funcional: diagnóstico, tratamiento, observaciones y recomendaciones vinculadas a la cita.
- Historia clínica funcional: apertura automática, evolución, antecedentes médicos, alergias y observaciones generales.
- Reporte de citas por estado con exportación Excel generada por Apache POI.
- Registro de auditoría con Logback para eventos importantes.
- Consultas DAO de Recetas, Placas, Pagos, Inventario, Reportes y Clínica.
- Identidad visual OdontoSync integrada.

## Compilar y probar

```powershell
mvn clean test
mvn clean package
```

El WAR se genera en:

`target/odontosync.war`

## Revisión de dependencias

Con Internet disponible:

```powershell
mvn -Psecurity-audit org.owasp:dependency-check-maven:check
```

Reporte esperado:

`target/dependency-check-report.html`

## Logs

Logback genera, bajo la carpeta `logs` de Tomcat:

- `odontosync.log`
- `odontosync-audit.log`

No se registran contraseñas, hashes, tokens CSRF ni texto clínico sensible.

## MySQL

Base de datos demo:

`sql/odontosync_demo.sql`

Permisos del usuario técnico:

- `sql/permisos_crud_usuarios.sql`
- `sql/permisos_crud_pacientes.sql`
- `sql/permisos_crud_odontologos_horarios.sql`
- `sql/permisos_agenda_citas.sql`
- `sql/permisos_atencion_historia.sql`

## Carpeta local

Para el trabajo del equipo se recomienda mantener siempre el proyecto en:

`C:\Users\USER\Desktop\ODONTOSYNC`

Esto simplifica los comandos de compilación y despliegue usados durante el curso.
