<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.time.LocalDate,java.util.List,java.util.Set,java.util.HashSet,pe.odontosync.model.OdontologoDetalle,pe.odontosync.model.EspecialidadOpcion" %>
<%@ include file="layout-top.jspf" %>
<link rel="stylesheet" href="<%= Html.escape(base) %>/css/odontologos-dialog.css?v=20261024">
<%
List<OdontologoDetalle> dentists = (List<OdontologoDetalle>) request.getAttribute("odontologos");
List<EspecialidadOpcion> specialties = (List<EspecialidadOpcion>) request.getAttribute("especialidades");
OdontologoDetalle edit = (OdontologoDetalle) request.getAttribute("editDentist");
String query = (String) request.getAttribute("query");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
boolean canManage = Boolean.TRUE.equals(request.getAttribute("canManageDentists"));
boolean editing = edit != null;
Set<Long> selectedSpecialties = editing ? new HashSet<>(edit.especialidadIds()) : Set.of();
%>
<div class="page-heading">
  <div>
    <h1>Odontólogos</h1>
    <p>Directorio profesional, cuenta de acceso, especialidades y estado del equipo clínico.</p>
  </div>
  <div class="dentist-heading-actions">
    <% if (canManage) { %><button class="btn primary" type="button" data-open-dentist-dialog>+ Nuevo odontólogo</button><% } %>
  </div>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<section class="patient-toolbar dentist-toolbar">
  <form class="patient-search" action="<%= Html.escape(base) %>/odontologos" method="get">
    <label>Buscar profesional</label>
    <div class="search-row">
      <input name="q" maxlength="80" value="<%= Html.escape(query == null ? "" : query) %>" placeholder="Nombre, colegiatura, correo o especialidad">
      <button class="btn secondary" type="submit">Buscar</button>
      <% if (query != null && !query.isBlank()) { %><a class="btn secondary" href="<%= Html.escape(base) %>/odontologos">Limpiar</a><% } %>
    </div>
  </form>
  <div class="patient-toolbar-note"><strong><%= dentists == null ? 0 : dentists.size() %></strong><span>profesional(es)</span></div>
</section>

<% if (canManage) { %>
<dialog class="crud-card crud-dialog dentist-dialog" data-dentist-dialog data-auto-open="<%= editing %>">
  <div class="crud-head">
    <div>
      <h2><%= editing ? "Editar odontólogo" : "Registrar odontólogo" %></h2>
      <p><%= editing ? "Actualiza el perfil profesional y su cuenta de acceso." : "Crea el perfil profesional y una cuenta OdontoSync con rol Odontólogo." %></p>
    </div>
    <a class="btn secondary" href="<%= Html.escape(base) %>/odontologos">Cancelar</a>
  </div>

  <form class="crud-form dentist-form" action="<%= Html.escape(base) %>/odontologos" method="post" autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="<%= editing ? "actualizar" : "crear" %>">
    <% if (editing) { %><input type="hidden" name="id_odontologo" value="<%= edit.idOdontologo() %>"><% } %>

    <label>Nombres
      <input name="nombres" maxlength="100" required value="<%= Html.escape(editing ? edit.nombres() : "") %>">
    </label>
    <label>Apellidos
      <input name="apellidos" maxlength="100" required value="<%= Html.escape(editing ? edit.apellidos() : "") %>">
    </label>
    <label>Correo de acceso
      <input type="email" name="correo" maxlength="150" required value="<%= Html.escape(editing ? edit.correo() : "") %>">
    </label>
    <label>Teléfono
      <input name="telefono" maxlength="20" value="<%= Html.escape(editing && edit.telefono() != null ? edit.telefono() : "") %>">
    </label>
    <label>N.º colegiatura
      <input name="colegiatura" maxlength="50" required value="<%= Html.escape(editing ? edit.numeroColegiatura() : "") %>" placeholder="Ej. COP-12345">
    </label>
    <label>Fecha de ingreso
      <input type="date" name="fecha_ingreso" required max="<%= LocalDate.now() %>" value="<%= editing && edit.fechaIngreso() != null ? edit.fechaIngreso() : LocalDate.now() %>">
    </label>
    <label>Estado
      <% String selectedStatus = editing ? edit.estado() : "ACTIVO"; %>
      <select name="estado" required>
        <option value="ACTIVO" <%= "ACTIVO".equals(selectedStatus) ? "selected" : "" %>>ACTIVO</option>
        <option value="INACTIVO" <%= "INACTIVO".equals(selectedStatus) ? "selected" : "" %>>INACTIVO</option>
      </select>
      <small>Al inactivar también se bloquea su ingreso y se desactivan sus horarios.</small>
    </label>
    <label>Contraseña
      <input type="password" name="clave" maxlength="72" <%= editing ? "" : "required" %>>
      <small><%= editing ? "Déjala vacía para conservar la actual." : "Mínimo 8 caracteres, con mayúscula, minúscula y número." %></small>
    </label>

    <fieldset class="specialty-field span-2">
      <legend>Especialidades</legend>
      <p>Selecciona al menos una especialidad para el perfil profesional.</p>
      <div class="specialty-grid">
        <% if (specialties != null) for (EspecialidadOpcion sp : specialties) { %>
          <label class="specialty-option">
            <input type="checkbox" name="especialidades" value="<%= sp.id() %>" <%= selectedSpecialties.contains(sp.id()) ? "checked" : "" %>>
            <span><%= Html.escape(sp.nombre()) %></span>
          </label>
        <% } %>
      </div>
    </fieldset>

    <div class="form-actions">
      <button class="btn primary" type="submit"><%= editing ? "Guardar cambios" : "Registrar odontólogo" %></button>
    </div>
  </form>
</dialog>
<% } %>

<section class="table-card crud-list dentist-list">
  <div class="table-top">
    <div><h2>Equipo odontológico</h2><span><%= canManage ? "Administración mantiene perfiles y cuentas vinculadas." : "Vista de consulta para Recepción." %></span></div>
  </div>
  <div class="table-scroll">
    <table>
      <thead><tr><th>Odontólogo</th><th>Colegiatura</th><th>Especialidades</th><th>Contacto</th><th>Ingreso</th><th>Estado</th><% if (canManage) { %><th>Acciones</th><% } %></tr></thead>
      <tbody>
      <% if (dentists != null) for (OdontologoDetalle d : dentists) { %>
        <tr>
          <td><strong><%= Html.escape(d.nombreCompleto()) %></strong><br><small class="table-secondary"><%= Html.escape(d.correo()) %></small></td>
          <td><%= Html.escape(d.numeroColegiatura()) %></td>
          <td><span class="specialty-text"><%= Html.escape(d.especialidadesTexto()) %></span></td>
          <td><%= Html.escape(d.telefono() == null ? "—" : d.telefono()) %></td>
          <td><%= d.fechaIngreso() == null ? "—" : d.fechaIngreso() %></td>
          <td><span class="status-pill <%= "INACTIVO".equals(d.estado()) ? "status-inactive" : "" %>"><%= Html.escape(d.estado()) %></span></td>
          <% if (canManage) { %><td><div class="row-actions">
            <a class="btn tiny secondary" href="<%= Html.escape(base) %>/odontologos?editar=<%= d.idOdontologo() %>">Editar</a>
            <a class="btn tiny secondary" href="<%= Html.escape(base) %>/horarios">Horarios</a>
            <form action="<%= Html.escape(base) %>/odontologos" method="post">
              <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
              <input type="hidden" name="accion" value="estado">
              <input type="hidden" name="id_odontologo" value="<%= d.idOdontologo() %>">
              <input type="hidden" name="estado" value="<%= "ACTIVO".equals(d.estado()) ? "INACTIVO" : "ACTIVO" %>">
              <button class="btn tiny <%= "ACTIVO".equals(d.estado()) ? "danger-btn" : "success-btn" %>" type="submit"><%= "ACTIVO".equals(d.estado()) ? "Inactivar" : "Reactivar" %></button>
            </form>
          </div></td><% } %>
        </tr>
      <% } %>
      <% if (dentists == null || dentists.isEmpty()) { %><tr><td colspan="<%= canManage ? 7 : 6 %>" class="empty">No se encontraron odontólogos.</td></tr><% } %>
      </tbody>
    </table>
  </div>
</section>

<p class="read-only"><%= canManage ? "La baja es lógica: citas, historias y atenciones anteriores nunca se eliminan." : "Recepción puede consultar profesionales y sus datos de contacto; la edición corresponde a Administración." %></p>
<script src="<%= Html.escape(base) %>/js/odontologos-dialog.js" defer></script>
<%@ include file="layout-bottom.jspf" %>
