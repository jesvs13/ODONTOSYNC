<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.time.LocalDate,java.util.List,java.util.Set,java.util.HashSet,pe.odontosync.model.OdontologoDetalle,pe.odontosync.model.EspecialidadOpcion,pe.odontosync.model.EspecialidadCatalogo" %>
<%@ include file="layout-top.jspf" %>
<link rel="stylesheet" href="<%= Html.escape(base) %>/css/odontologos-dialog.css?v=20261127-issue37-84">
<%
List<OdontologoDetalle> dentists = (List<OdontologoDetalle>) request.getAttribute("odontologos");
List<EspecialidadOpcion> specialties = (List<EspecialidadOpcion>) request.getAttribute("especialidades");
OdontologoDetalle edit = (OdontologoDetalle) request.getAttribute("editDentist");
String query = (String) request.getAttribute("query");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
boolean canManage = Boolean.TRUE.equals(request.getAttribute("canManageDentists"));
boolean editing = edit != null;
List<EspecialidadCatalogo> catalog = canManage && request.getAttribute("catalogoEspecialidades") != null
    ? (List<EspecialidadCatalogo>) request.getAttribute("catalogoEspecialidades") : List.of();
boolean openCatalog = canManage && !editing && "1".equals(request.getParameter("especialidades"));
long activeCatalogCount = catalog.stream().filter(EspecialidadCatalogo::active).count();
Set<Long> selectedSpecialties = editing ? new HashSet<>(edit.especialidadIds()) : Set.of();
%>
<div class="page-heading">
  <div>
    <h1>Odontólogos</h1>
    <p>Directorio profesional, especialidades y estado del equipo clínico.</p>
  </div>
  <div class="dentist-heading-actions">
    <% if (canManage) { %><button class="btn secondary" type="button" data-open-specialty-dialog>Gestionar especialidades</button><% } %>
    <% if (canManage) { if (editing) { %><a class="btn primary" href="<%= Html.escape(base) %>/odontologos">+ Nuevo odontólogo</a><% } else { %><button class="btn primary" type="button" data-open-dentist-dialog>+ Nuevo odontólogo</button><% } } %>
  </div>
</div>

<% if (!openCatalog && success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (!openCatalog && error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

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
      <p><%= editing ? "Actualiza los datos profesionales. Las credenciales se gestionan en Usuarios." : "Crea el perfil profesional y una cuenta OdontoSync con rol Odontólogo." %></p>
    </div>
    <a class="btn secondary" href="<%= Html.escape(base) %>/odontologos">Cancelar</a>
  </div>

  <form class="crud-form dentist-form" action="<%= Html.escape(base) %>/odontologos" method="post" <%= editing ? "" : "data-institutional-form" %> autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="<%= editing ? "actualizar" : "crear" %>">
    <% if (editing) { %><input type="hidden" name="id_odontologo" value="<%= edit.idOdontologo() %>"><% } %>

    <label>Nombres
      <input name="nombres" maxlength="100" required value="<%= Html.escape(editing ? edit.nombres() : "") %>">
    </label>
    <% if (editing) { %>
      <label>Apellidos
        <input name="apellidos" maxlength="100" required value="<%= Html.escape(edit.apellidos()) %>">
      </label>
    <% } else { %>
      <label>Primer apellido
        <input name="primer_apellido" maxlength="100" required>
      </label>
      <label>Segundo apellido
        <input name="segundo_apellido" maxlength="100" placeholder="Opcional">
      </label>
    <% } %>
    <label class="span-2">Correo institucional
      <input type="email" name="correo" readonly data-institutional-email value="<%= Html.escape(editing ? edit.correo() : "") %>" placeholder="Se genera con los nombres y apellidos">
      <% if (editing) { %><small>El correo de acceso se conserva al editar el perfil.</small>
      <% } else { %><small data-email-status role="status" aria-live="polite">Completa los nombres y apellidos.</small><% } %>
    </label>
    <% if (!editing) { %>
      <input type="hidden" name="alta_nonce" value="<%= Html.escape((String) request.getAttribute("altaNonce")) %>">
      <label>DNI
        <input name="dni" type="text" inputmode="numeric" maxlength="8" minlength="8" pattern="[0-9]{8}" required autocomplete="off" placeholder="8 dígitos">
      </label>
    <% } %>
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
      <small>Al inactivar se deshabilita el acceso activo y los horarios. Para habilitar el acceso tras reactivar, utiliza Usuarios.</small>
    </label>
    <% if (!editing) { %>
      <div class="span-2 read-only"><strong>Contraseña temporal automática</strong><p>Se mostrará una sola vez al registrar la cuenta. El usuario deberá cambiarla después de verificar su correo.</p></div>
    <% } else { %>
      <p class="span-2 read-only">Para gestionar la contraseña o el bloqueo de acceso, utiliza <a href="<%= Html.escape(base) %>/usuarios?editar=<%= edit.idUsuario() %>">Usuarios</a>.</p>
    <% } %>

    <fieldset class="specialty-field span-2">
      <legend>Especialidades</legend>
      <p>Selecciona al menos una especialidad para el perfil profesional. <% if (editing) { %>Las especialidades inactivas ya asignadas pueden conservarse o retirarse; no se podrán volver a asignar mientras sigan inactivas.<% } %></p>
      <% if (specialties == null || specialties.isEmpty()) { %><p>No hay especialidades disponibles. Activa o registra una desde Gestionar especialidades.</p><% } %>
      <div class="specialty-grid">
        <% if (specialties != null) for (EspecialidadOpcion sp : specialties) { %>
          <label class="specialty-option">
            <input type="checkbox" name="especialidades" value="<%= sp.id() %>" <%= selectedSpecialties.contains(sp.id()) ? "checked" : "" %>>
            <span><%= Html.escape(sp.nombre()) %><% if ("INACTIVA".equals(sp.estado())) { %> <small>(Inactiva · asignada)</small><% } %></span>
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

<% if (canManage) { %>
<dialog class="crud-card crud-dialog dentist-dialog specialty-dialog" data-specialty-dialog data-auto-open="<%= openCatalog %>" aria-labelledby="specialty-title">
  <div class="crud-head specialty-head">
    <div><p class="specialty-eyebrow">CATÁLOGO CLÍNICO</p><h2 id="specialty-title">Gestionar especialidades</h2><p>Administra las especialidades disponibles en tu clínica.</p></div>
    <button class="btn secondary" type="button" data-close-specialty-dialog>Cerrar</button>
  </div>
  <% if (openCatalog && success != null) { %><div class="alert success" role="status"><%= Html.escape(success) %></div><% } %>
  <% if (openCatalog && error != null) { %><div class="alert danger" role="alert"><%= Html.escape(error) %></div><% } %>
  <div class="specialty-metrics">
    <div><span>Total</span><strong><%= catalog.size() %></strong></div>
    <div><span>Activas</span><strong><%= activeCatalogCount %></strong></div>
    <div><span>Inactivas</span><strong><%= catalog.size() - activeCatalogCount %></strong></div>
  </div>
  <div class="specialty-toolbar">
    <label>Buscar especialidad<input type="search" data-specialty-search maxlength="100" placeholder="Nombre, origen o estado" autocomplete="off"></label>
    <button class="btn primary" type="button" data-new-specialty>+ Nueva especialidad</button>
  </div>
  <form class="crud-form specialty-editor" data-specialty-editor hidden action="<%= Html.escape(base) %>/odontologos" method="post">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="crear-especialidad">
    <input type="hidden" name="id_especialidad" value="">
    <label class="span-2"><span data-specialty-editor-label>Nueva especialidad</span><input name="nombre_especialidad" maxlength="100" required autocomplete="off"></label>
    <div class="specialty-editor-actions span-2"><button type="button" class="btn secondary" data-cancel-specialty>Cancelar</button><button class="btn primary" type="submit">Guardar especialidad</button></div>
  </form>
  <div class="specialty-catalog-scroll">
    <table class="specialty-catalog-table">
      <thead><tr><th>Especialidad</th><th>Estado</th><th>Acciones</th></tr></thead>
      <tbody>
        <% for (EspecialidadCatalogo sp : catalog) { %>
        <tr data-specialty-row data-specialty-search-text="<%= Html.escape(sp.nombre() + " " + (sp.clinicId() == null ? "Catálogo global" : "Mi clínica") + " " + (sp.active() ? "Activa" : "Inactiva")) %>">
          <td><strong><%= Html.escape(sp.nombre()) %></strong><small><%= sp.clinicId() == null ? "Catálogo global" : "Mi clínica" %></small></td>
          <td><span class="status-pill <%= sp.active() ? "" : "status-inactive" %>"><%= sp.active() ? "Activa" : "Inactiva" %></span></td>
          <td><div class="specialty-row-actions">
            <% if (sp.clinicId() != null) { %><button type="button" class="btn tiny secondary" data-edit-specialty data-id="<%= sp.id() %>" data-name="<%= Html.escape(sp.nombre()) %>">Editar</button><% } %>
            <form action="<%= Html.escape(base) %>/odontologos" method="post" data-specialty-status>
              <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
              <input type="hidden" name="accion" value="estado-especialidad">
              <input type="hidden" name="id_especialidad" value="<%= sp.id() %>">
              <input type="hidden" name="estado_especialidad" value="<%= sp.active() ? "INACTIVA" : "ACTIVA" %>">
              <button type="submit" class="btn tiny <%= sp.active() ? "danger-btn" : "success-btn" %>"><%= sp.active() ? "Inactivar" : "Activar" %></button>
            </form>
          </div></td>
        </tr>
        <% } %>
        <tr data-specialty-empty <%= catalog.isEmpty() ? "" : "hidden" %>><td colspan="3" class="empty">No se encontraron especialidades.</td></tr>
      </tbody>
    </table>
  </div>
  <p class="specialty-note">Las especialidades globales no se pueden renombrar. Su disponibilidad se gestiona por clínica. Inactivar no elimina asignaciones ni información histórica.</p>
</dialog>
<% } %>

<section class="table-card crud-list dentist-list">
  <div class="table-top">
    <div><h2>Equipo odontológico</h2><span><%= canManage ? "Administración gestiona los perfiles profesionales y sus especialidades." : "Vista de consulta para Recepción." %></span></div>
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
<script src="<%= Html.escape(base) %>/js/odontologos-dialog.js?v=20261127-issue37-84" defer></script>
<script src="<%= Html.escape(base) %>/js/correo-institucional-ui.js?v=20261127-1" defer></script>
<%@ include file="layout-bottom.jspf" %>
