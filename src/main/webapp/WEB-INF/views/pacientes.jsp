<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.time.LocalDate,java.time.Period,pe.odontosync.model.*" %>
<%@ include file="layout-top.jspf" %>
<link rel="stylesheet" href="<%= Html.escape(base) %>/css/pacientes-dialog.css?v=20261024">
<%
List<PacienteDetalle> patients = (List<PacienteDetalle>) request.getAttribute("pacientes");
PacienteDetalle edit = (PacienteDetalle) request.getAttribute("editPatient");
PacienteFicha file = (PacienteFicha) request.getAttribute("patientFile");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
String query = (String) request.getAttribute("query");
Boolean manageAttr = (Boolean) request.getAttribute("canManagePatients");
boolean canManage = Boolean.TRUE.equals(manageAttr);
boolean canViewClinical = Boolean.TRUE.equals((Boolean) request.getAttribute("canViewClinical"));
boolean canViewPayments = Boolean.TRUE.equals((Boolean) request.getAttribute("canViewPayments"));
boolean editing = edit != null;
%>
<div class="page-heading">
  <div>
    <h1><%= user.role() == pe.odontosync.Role.ODONTOLOGO ? "Mis pacientes" : "Pacientes" %></h1>
    <p><%= canManage ? "Registra, actualiza y consulta la ficha integral de cada paciente." : "Consulta los pacientes vinculados a tus atenciones." %></p>
  </div>
  <div class="patient-heading-actions">
    <% if (canManage) { %><button class="btn primary" type="button" data-open-patient-dialog>+ Nuevo paciente</button><% } %>
  </div>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<section class="patient-toolbar">
  <form class="patient-search" action="<%= Html.escape(base) %>/pacientes" method="get" role="search">
    <label for="patient-q">Buscar paciente</label>
    <div class="search-row">
      <input id="patient-q" name="q" maxlength="80" value="<%= Html.escape(query == null ? "" : query) %>" placeholder="Nombre, documento, correo o teléfono">
      <button class="btn primary" type="submit">Buscar</button>
      <% if (query != null && !query.isBlank()) { %><a class="btn secondary" href="<%= Html.escape(base) %>/pacientes">Limpiar</a><% } %>
    </div>
  </form>
  <div class="patient-toolbar-note">
    <strong><%= patients == null ? 0 : patients.size() %></strong>
    <span>resultado(s)</span>
  </div>
</section>

<% if (canManage) { %>
<dialog class="crud-card crud-dialog patient-dialog" data-patient-dialog data-auto-open="<%= editing %>">
  <div class="crud-head">
    <div>
      <h2><%= editing ? "Editar paciente" : "Registrar paciente" %></h2>
      <p><%= editing ? "Actualiza los datos administrativos sin afectar su historial clínico." : "Crea la ficha administrativa que luego se relacionará con citas y atención clínica." %></p>
    </div>
    <a class="btn secondary" href="<%= Html.escape(base) %>/pacientes">Cancelar</a>
  </div>

  <form class="crud-form patient-form" action="<%= Html.escape(base) %>/pacientes" method="post" autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="<%= editing ? "actualizar" : "crear" %>">
    <% if (editing) { %><input type="hidden" name="id_paciente" value="<%= edit.id() %>"><% } %>

    <label>Tipo de documento
      <select name="tipo_documento" required>
        <% String currentType = editing ? edit.tipoDocumento() : "DNI"; %>
        <% for (String type : List.of("DNI","CE","PASAPORTE","OTRO")) { %>
          <option value="<%= type %>" <%= type.equals(currentType) ? "selected" : "" %>><%= type %></option>
        <% } %>
      </select>
    </label>
    <label>Número de documento
      <input name="numero_documento" maxlength="20" required value="<%= Html.escape(editing ? edit.numeroDocumento() : "") %>" placeholder="Ej. 70123456">
    </label>
    <label>Nombres
      <input name="nombres" maxlength="100" required value="<%= Html.escape(editing ? edit.nombres() : "") %>">
    </label>
    <label>Apellidos
      <input name="apellidos" maxlength="100" required value="<%= Html.escape(editing ? edit.apellidos() : "") %>">
    </label>
    <label>Fecha de nacimiento
      <input type="date" name="fecha_nacimiento" required max="<%= LocalDate.now() %>" value="<%= editing && edit.fechaNacimiento() != null ? edit.fechaNacimiento() : "" %>">
    </label>
    <label>Sexo
      <% String currentSex = editing && edit.sexo() != null ? edit.sexo() : ""; %>
      <select name="sexo">
        <option value="" <%= currentSex.isEmpty() ? "selected" : "" %>>No especificado</option>
        <option value="F" <%= "F".equals(currentSex) ? "selected" : "" %>>Femenino</option>
        <option value="M" <%= "M".equals(currentSex) ? "selected" : "" %>>Masculino</option>
        <option value="OTRO" <%= "OTRO".equals(currentSex) ? "selected" : "" %>>Otro</option>
      </select>
    </label>
    <label>Teléfono
      <input name="telefono" maxlength="20" value="<%= Html.escape(editing && edit.telefono() != null ? edit.telefono() : "") %>">
    </label>
    <label>Correo electrónico
      <input type="email" name="correo" maxlength="150" value="<%= Html.escape(editing && edit.correo() != null ? edit.correo() : "") %>">
    </label>
    <label class="span-2">Dirección
      <input name="direccion" maxlength="255" value="<%= Html.escape(editing && edit.direccion() != null ? edit.direccion() : "") %>">
    </label>
    <label>Ocupación
      <input name="ocupacion" maxlength="100" value="<%= Html.escape(editing && edit.ocupacion() != null ? edit.ocupacion() : "") %>">
    </label>
    <label>Estado
      <% String currentStatus = editing ? edit.estado() : "ACTIVO"; %>
      <select name="estado" required>
        <option value="ACTIVO" <%= "ACTIVO".equals(currentStatus) ? "selected" : "" %>>ACTIVO</option>
        <option value="INACTIVO" <%= "INACTIVO".equals(currentStatus) ? "selected" : "" %>>INACTIVO</option>
      </select>
    </label>
    <label>Contacto de emergencia
      <input name="contacto_emergencia" maxlength="150" value="<%= Html.escape(editing && edit.contactoEmergencia() != null ? edit.contactoEmergencia() : "") %>">
    </label>
    <label>Teléfono de emergencia
      <input name="telefono_emergencia" maxlength="20" value="<%= Html.escape(editing && edit.telefonoEmergencia() != null ? edit.telefonoEmergencia() : "") %>">
    </label>

    <div class="form-actions">
      <button class="btn primary" type="submit"><%= editing ? "Guardar cambios" : "Registrar paciente" %></button>
    </div>
  </form>
</dialog>
<% } %>

<% if (file != null) {
     PacienteDetalle p = file.paciente();
     String sexLabel = "F".equals(p.sexo()) ? "Femenino" : "M".equals(p.sexo()) ? "Masculino" : "OTRO".equals(p.sexo()) ? "Otro" : "No especificado";
     String age = p.fechaNacimiento() == null ? "—" : String.valueOf(Period.between(p.fechaNacimiento(), LocalDate.now()).getYears()) + " años";
%>
<section class="patient-file">
  <div class="patient-file-head">
    <div class="patient-avatar"><%= Html.escape(p.nombres().substring(0,1).toUpperCase()) %><%= Html.escape(p.apellidos().substring(0,1).toUpperCase()) %></div>
    <div>
      <span class="eyebrow">FICHA DEL PACIENTE</span>
      <h2><%= Html.escape(p.nombreCompleto()) %></h2>
      <p><%= Html.escape(p.tipoDocumento() + " " + p.numeroDocumento()) %> · <%= Html.escape(age) %> · <%= Html.escape(sexLabel) %></p>
    </div>
    <div class="patient-file-actions">
      <span class="status-pill <%= "INACTIVO".equals(p.estado()) ? "status-inactive" : "" %>"><%= Html.escape(p.estado()) %></span>
      <% if (canManage && "ACTIVO".equals(p.estado())) { %><a class="btn tiny primary" href="<%= Html.escape(base) %>/agenda?paciente=<%= p.id() %>">Agendar cita</a><% } %>
    </div>
  </div>

  <div class="patient-data-grid">
    <div><span>Teléfono</span><strong><%= Html.escape(p.telefono() == null ? "—" : p.telefono()) %></strong></div>
    <div><span>Correo</span><strong><%= Html.escape(p.correo() == null ? "—" : p.correo()) %></strong></div>
    <div><span>Ocupación</span><strong><%= Html.escape(p.ocupacion() == null ? "—" : p.ocupacion()) %></strong></div>
    <div><span>Dirección</span><strong><%= Html.escape(p.direccion() == null ? "—" : p.direccion()) %></strong></div>
    <div><span>Contacto emergencia</span><strong><%= Html.escape(p.contactoEmergencia() == null ? "—" : p.contactoEmergencia()) %></strong></div>
    <div><span>Teléfono emergencia</span><strong><%= Html.escape(p.telefonoEmergencia() == null ? "—" : p.telefonoEmergencia()) %></strong></div>
  </div>

  <div class="patient-file-grid">
    <article class="patient-section patient-section-wide">
      <div class="section-title"><div><span>AGENDA</span><h3>Citas recientes</h3></div><span><%= file.citas().size() %> registro(s)</span></div>
      <div class="mini-table-scroll"><table class="mini-table"><thead><tr><th>Fecha</th><th>Hora</th><th>Odontólogo</th><th>Estado</th><th>Motivo</th></tr></thead><tbody>
      <% for (PacienteCitaDetalle x : file.citas()) { %><tr><td><%= Html.escape(x.fecha()) %></td><td><%= Html.escape(x.hora()) %></td><td><%= Html.escape(x.odontologo()) %></td><td><%= Html.escape(x.estado()) %></td><td><%= Html.escape(x.motivo()) %></td></tr><% } %>
      <% if (file.citas().isEmpty()) { %><tr><td colspan="5" class="empty">Todavía no hay citas registradas.</td></tr><% } %>
      </tbody></table></div>
    </article>

    <% if (canViewClinical) { %>
    <article class="patient-section">
      <div class="section-title"><div><span>CLÍNICA</span><h3>Historia clínica</h3></div></div>
      <% if (file.historia().isPresent()) { PacienteHistoriaDetalle h = file.historia().get(); %>
        <dl class="summary-list"><div><dt>Apertura</dt><dd><%= Html.escape(h.fechaApertura()) %></dd></div><div><dt>Estado</dt><dd><%= Html.escape(h.estado()) %></dd></div><div class="full"><dt>Observaciones</dt><dd><%= Html.escape(h.observaciones().isBlank() ? "Sin observaciones" : h.observaciones()) %></dd></div></dl>
      <% } else { %><p class="empty-note">Aún no tiene historia clínica registrada.</p><% } %>
    </article>

    <article class="patient-section">
      <div class="section-title"><div><span>TRATAMIENTO</span><h3>Recetas</h3></div><span><%= file.recetas().size() %></span></div>
      <div class="stack-list">
      <% for (PacienteRecetaDetalle x : file.recetas()) { %><div class="stack-item"><strong><%= Html.escape(x.fecha()) %> · <%= Html.escape(x.odontologo()) %></strong><span><%= Html.escape(x.medicamentos()) %></span><small><%= Html.escape(x.indicaciones()) %></small></div><% } %>
      <% if (file.recetas().isEmpty()) { %><p class="empty-note">Sin recetas emitidas.</p><% } %>
      </div>
    </article>

    <article class="patient-section">
      <div class="section-title"><div><span>ARCHIVOS</span><h3>Placas radiográficas</h3></div><span><%= file.placas().size() %></span></div>
      <div class="stack-list">
      <% for (PacientePlacaDetalle x : file.placas()) { %><div class="stack-item"><strong><%= Html.escape(x.fecha()) %></strong><span><%= Html.escape(x.archivo()) %></span><small><%= Html.escape(x.url()) %></small></div><% } %>
      <% if (file.placas().isEmpty()) { %><p class="empty-note">Sin placas registradas.</p><% } %>
      </div>
    </article>
    <% } %>

    <% if (canViewPayments) { %>
    <article class="patient-section">
      <div class="section-title"><div><span>CUENTA</span><h3>Pagos recientes</h3></div><span><%= file.pagos().size() %></span></div>
      <div class="stack-list">
      <% for (PacientePagoDetalle x : file.pagos()) { %><div class="stack-item payment-item"><strong>S/ <%= Html.escape(x.monto()) %></strong><span><%= Html.escape(x.metodo()) %> · <%= Html.escape(x.fecha()) %></span><small>Cita: <%= Html.escape(x.cita()) %></small></div><% } %>
      <% if (file.pagos().isEmpty()) { %><p class="empty-note">Sin pagos registrados.</p><% } %>
      </div>
    </article>
    <% } %>
  </div>
</section>
<% } %>

<section class="table-card crud-list patient-list">
  <div class="table-top">
    <div><h2><%= user.role() == pe.odontosync.Role.ODONTOLOGO ? "Pacientes vinculados" : "Pacientes registrados" %></h2><span>La baja es lógica: el historial clínico nunca se elimina.</span></div>
  </div>
  <div class="table-scroll">
    <table>
      <thead><tr><th>Paciente</th><th>Documento</th><th>Contacto</th><th>Estado</th><th>Acciones</th></tr></thead>
      <tbody>
      <% if (patients != null) for (PacienteDetalle item : patients) { %>
        <tr>
          <td><strong><%= Html.escape(item.nombreCompleto()) %></strong><br><small class="table-secondary"><%= Html.escape(item.correo() == null ? "Sin correo" : item.correo()) %></small></td>
          <td><%= Html.escape(item.tipoDocumento() + " " + item.numeroDocumento()) %></td>
          <td><%= Html.escape(item.telefono() == null ? "—" : item.telefono()) %></td>
          <td><span class="status-pill <%= "INACTIVO".equals(item.estado()) ? "status-inactive" : "" %>"><%= Html.escape(item.estado()) %></span></td>
          <td><div class="row-actions">
            <a class="btn tiny secondary" href="<%= Html.escape(base) %>/pacientes?ver=<%= item.id() %><%= query != null && !query.isBlank() ? "&q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8) : "" %>">Ver ficha</a>
            <% if (canManage) { %>
              <a class="btn tiny secondary" href="<%= Html.escape(base) %>/pacientes?editar=<%= item.id() %>">Editar</a>
              <form action="<%= Html.escape(base) %>/pacientes" method="post">
                <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
                <input type="hidden" name="accion" value="estado">
                <input type="hidden" name="id_paciente" value="<%= item.id() %>">
                <input type="hidden" name="estado" value="<%= "ACTIVO".equals(item.estado()) ? "INACTIVO" : "ACTIVO" %>">
                <button class="btn tiny <%= "ACTIVO".equals(item.estado()) ? "danger-btn" : "success-btn" %>" type="submit"><%= "ACTIVO".equals(item.estado()) ? "Dar de baja" : "Reactivar" %></button>
              </form>
            <% } %>
          </div></td>
        </tr>
      <% } %>
      <% if (patients == null || patients.isEmpty()) { %><tr><td colspan="5" class="empty">No se encontraron pacientes.</td></tr><% } %>
      </tbody>
    </table>
  </div>
</section>

<p class="read-only"><%= canManage ? "Administración y Recepción pueden mantener los datos del paciente. Odontología consulta únicamente pacientes vinculados a sus citas." : "Tu perfil es de consulta: los datos administrativos se gestionan desde Administración o Recepción." %></p>
<script src="<%= Html.escape(base) %>/js/pacientes-dialog.js" defer></script>
<%@ include file="layout-bottom.jspf" %>
