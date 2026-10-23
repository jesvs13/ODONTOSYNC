# OdontoSync

OdontoSync es un sistema web para la gestión de una clínica odontológica desarrollado como parte del curso de Integrador I.

El proyecto busca facilitar la administración de pacientes, odontólogos, horarios, citas y atenciones clínicas, centralizando la información en una base de datos MySQL.

## Tecnologías utilizadas

Para el desarrollo del proyecto utilizamos:

- Java 17
- JSP y Servlets
- JDBC
- MySQL
- Maven
- Apache Tomcat 10.1
- HTML y CSS
- JavaScript para algunas funciones de interfaz
- Visual Studio Code

El proyecto se ha desarrollado principalmente en Java, manteniendo la estructura solicitada para el curso.

## Arquitectura del proyecto

Organizamos el sistema utilizando una arquitectura basada en MVC y separando las responsabilidades principales.

La estructura general es:

```text
Vista (JSP)
     ↓
Controller (Servlet)
     ↓
Service
     ↓
DAO
     ↓
JDBC
     ↓
MySQL
```

### Vista

Las vistas están desarrolladas con JSP, HTML y CSS.

Son las pantallas que utiliza el usuario para interactuar con el sistema, por ejemplo:

- Login
- Panel principal
- Pacientes
- Odontólogos
- Horarios
- Agenda
- Atención clínica
- Historia clínica

### Controller

Los controladores están desarrollados utilizando Servlets.

Su función es recibir las solicitudes realizadas desde las vistas y comunicarse con la lógica del sistema.

Algunos ejemplos son:

- `LoginServlet`
- `PacienteServlet`
- `OdontologoServlet`
- `HorarioServlet`
- `AgendaServlet`
- `AtencionServlet`

### Service

La capa Service contiene las principales validaciones y reglas de negocio.

Por ejemplo, en el registro de una cita se verifica:

- que el paciente exista;
- que el odontólogo esté disponible;
- que la especialidad corresponda al odontólogo;
- que la cita esté dentro de su horario;
- que no exista cruce de horarios;
- que la agenda no se encuentre bloqueada.

### DAO

La capa DAO se encarga de realizar las consultas a MySQL utilizando JDBC y `PreparedStatement`.

De esta manera evitamos colocar consultas SQL directamente dentro de los Servlets.

## Módulos desarrollados

Actualmente nuestro proyecto cuenta con los siguientes módulos:

- Inicio de sesión
- Control de usuarios por roles
- Panel principal según el tipo de usuario
- Gestión de usuarios
- Gestión de pacientes
- Gestión de odontólogos
- Especialidades
- Gestión de horarios
- Agenda y citas
- Bloqueo de agenda
- Atención clínica
- Historia clínica
- Reportes en Excel

También continuamos trabajando en los módulos restantes del proyecto.

## Roles del sistema

El sistema maneja principalmente tres tipos de usuario:

### Administrador

Tiene acceso a las funciones administrativas del sistema, como usuarios, odontólogos, horarios y otros módulos de gestión.

### Recepción

Está orientado principalmente a la gestión de pacientes, citas y agenda.

### Odontólogo

Tiene acceso a la información necesaria para realizar la atención clínica de sus pacientes.

## Base de datos

Utilizamos MySQL como sistema de gestión de base de datos.

La aplicación se conecta mediante JDBC utilizando un usuario técnico llamado:

```text
odontosync_app
```

Este usuario tiene solamente los permisos necesarios para que el sistema pueda trabajar con las tablas correspondientes.

No utilizamos el usuario `root` para la conexión normal de la aplicación.

## Seguridad

Durante el desarrollo también hemos aplicado diferentes medidas de seguridad.

Entre ellas se encuentran:

- contraseñas protegidas mediante PBKDF2 y salt;
- consultas SQL mediante `PreparedStatement`;
- control de acceso según roles;
- manejo de sesiones;
- cierre de sesión por inactividad;
- protección CSRF;
- protección frente a XSS;
- usuario de base de datos con permisos limitados;
- registro de errores y eventos importantes.

## Librerías utilizadas

Durante el desarrollo hemos incorporado algunas librerías de Java de acuerdo con las necesidades del proyecto.

### Google Guava

La utilizamos principalmente para algunas utilidades y para controlar temporalmente los intentos fallidos de inicio de sesión mediante caché.

### Apache Commons Lang

La utilizamos para trabajar y validar cadenas de texto recibidas desde los formularios.

Por ejemplo:

```java
StringUtils.trimToEmpty(...)
StringUtils.isBlank(...)
```

### Apache Commons IO

La utilizamos para facilitar el manejo de archivos y flujos de datos.

### Apache POI

La utilizamos para generar archivos Excel desde Java.

Actualmente permite exportar información del sistema a archivos `.xlsx`.

### SLF4J y Logback

Los utilizamos para registrar errores y eventos importantes durante la ejecución del sistema.

Esto nos ayuda tanto para revisar problemas como para tener un registro de determinadas acciones realizadas dentro de la aplicación.

## Calidad del código

Como parte de la revisión del proyecto también utilizamos SonarQube para identificar posibles mejoras en el código.

Nos permite detectar aspectos como:

- código repetido;
- posibles errores;
- manejo de excepciones;
- problemas de mantenibilidad;
- recomendaciones de calidad.

Las observaciones encontradas se revisan antes de realizar cambios para evitar afectar funcionalidades que ya se encuentran operativas.

## Gestión de dependencias

Las dependencias del proyecto están definidas en:

```text
pom.xml
```

Maven se encarga de descargar las librerías necesarias, ejecutar las pruebas y generar el archivo WAR de la aplicación.

Los principales comandos utilizados son:

```bash
mvn clean test
mvn clean package
```

Al compilar el proyecto se genera:

```text
target/odontosync.war
```

Este archivo se despliega en Apache Tomcat.

## Ejecución del proyecto

Para ejecutar OdontoSync es necesario contar con:

- Java 17
- Maven
- MySQL Server
- Apache Tomcat 10.1

También se deben configurar las variables necesarias para la conexión con la base de datos.

La aplicación se ejecuta localmente mediante:

```text
http://localhost:8080/odontosync
```

## Trabajo colaborativo

El proyecto se encuentra almacenado en GitHub para facilitar el trabajo del equipo y mantener un historial de los cambios realizados.

La rama:

```text
main
```

se utiliza como versión principal y estable del proyecto.

Para nuevos cambios utilizaremos ramas independientes, por ejemplo:

```text
feature/recetas
feature/pagos
feature/inventario
feature/reportes
```

Cada integrante realizará sus respectivos commits para mantener identificado el trabajo realizado.

## Integrantes

Proyecto desarrollado por estudiantes del curso de Integrador I.


## Estado del proyecto

OdontoSync continúa en desarrollo como parte de las entregas del curso.

La versión actual permite demostrar el funcionamiento de los principales procesos administrativos y clínicos desarrollados hasta el momento.