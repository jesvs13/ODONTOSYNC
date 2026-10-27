<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.Map,java.time.LocalDate,pe.odontosync.Role,pe.odontosync.model.*" %>
<%@ include file="layout-top.jspf" %>
<%
List<CitaDetalle> appointments = (List<CitaDetalle>) request.getAttribute("agenda");
List<PacienteOpcion> patients = (List<PacienteOpcion>) request.getAttribute("agendaPatients");
List<OdontologoOpcion> dentists = (List<OdontologoOpcion>) request.getAttribute("agendaDentists");
List<EspecialidadOpcion> specialties = (List<EspecialidadOpcion>) request.getAttribute("agendaSpecialties");
Map<Long,List<Long>> dentistSpecialtyIds = (Map<Long,List<Long>>) request.getAttribute("agendaDentistSpecialtyIds");
List<CitaEstadoOpcion> states = (List<CitaEstadoOpcion>) request.getAttribute("agendaStates");
CitaDetalle edit = (CitaDetalle) request.getAttribute("editAppointment");
Long preselectedPatientId = (Long) request.getAttribute("preselectedPatientId");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
String filterFrom = (String) request.getAttribute("filterFrom");
String filterTo = (String) request.getAttribute("filterTo");
String filterStatus = (String) request.getAttribute("filterStatus");
String filterQuery = (String) request.getAttribute("filterQuery");
boolean canSchedule = Boolean.TRUE.equals(request.getAttribute("canScheduleAgenda"));
boolean editing = edit != null;

int pending = 0, confirmed = 0, attended = 0, cancelled = 0;
if (appointments != null) for (CitaDetalle x : appointments) {
  if ("PENDIENTE".equals(x.estado())) pending++;
  else if ("CONFIRMADA".equals(x.estado())) confirmed++;
  else if ("ATENDIDA".equals(x.estado())) attended++;
  else if ("CANCELADA".equals(x.estado()) || "NO_ASISTIO".equals(x.estado())) cancelled++;
}
%>
<div class="page-heading">
  <div>
    <h1><%= user.role() == Role.ODONTOLOGO ? "Mis citas" : "Agenda" %></h1>
    <p><%= canSchedule ? "Programa, confirma, reprograma y controla las citas de la clínica." : "Consulta tus citas y registra el resultado de las atenciones confirmadas." %></p>
  </div>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<section class="agenda-summary">
  <div><span>Resultados</span><strong><%= appointments == null ? 0 : appointments.size() %></strong></div>
  <div><span>Pendientes</span><strong><%= pending %></strong></div>
  <div><span>Confirmadas</span><strong><%= confirmed %></strong></div>
  <div><span>Atendidas</span><strong><%= attended %></strong></div>
  <div><span>Cerradas</span><strong><%= cancelled %></strong></div>
</section>

<section class="agenda-filter-card">
  <form class="agenda-filters" action="<%= Html.escape(base) %>/agenda" method="get">
    <label>Desde
      <input type="date" name="desde" value="<%= Html.escape(filterFrom == null ? "" : filterFrom) %>">
    </label>
    <label>Hasta
      <input type="date" name="hasta" value="<%= Html.escape(filterTo == null ? "" : filterTo) %>">
    </label>
    <label>Estado
      <select name="estado">
        <option value="">Todos</option>
        <% if (states != null) for (CitaEstadoOpcion st : states) { %>
          <option value="<%= Html.escape(st.nombre()) %>" <%= st.nombre().equals(filterStatus) ? "selected" : "" %>><%= Html.escape(st.nombre().replace('_',' ')) %></option>
        <% } %>
      </select>
    </label>
    <label class="agenda-query">Buscar
      <input name="q" maxlength="80" value="<%= Html.escape(filterQuery == null ? "" : filterQuery) %>" placeholder="Paciente, DNI, odontólogo o motivo">
    </label>
    <div class="agenda-filter-actions">
      <button class="btn primary" type="submit">Aplicar filtros</button>
      <a class="btn secondary" href="<%= Html.escape(base) %>/agenda?desde=&hasta=&estado=&q=">Ver todo</a>
    </div>
  </form>
</section>

<% if (canSchedule) { %>
<section class="crud-card agenda-editor">
  <div class="crud-head">
    <div>
      <h2><%= editing ? "Reprogramar / editar cita" : "Registrar nueva cita" %></h2>
      <p>OdontoSync valida horario del odontólogo, bloqueos y cruces con otras citas.</p>
    </div>
    <div class="crud-head-actions">
      <a class="btn secondary" href="<%= Html.escape(base) %>/horarios">Consultar horarios</a>
      <% if (editing) { %><a class="btn secondary" href="<%= Html.escape(base) %>/agenda">Cancelar edición</a><% } %>
    </div>
  </div>

  <form class="crud-form agenda-form" action="<%= Html.escape(base) %>/agenda" method="post" autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="<%= editing ? "actualizar" : "crear" %>">
    <% if (editing) { %><input type="hidden" name="id_cita" value="<%= edit.idCita() %>"><% } %>

    <label>Paciente
      <select name="id_paciente" required>
        <option value="">Selecciona un paciente</option>
        <% if (patients != null) for (PacienteOpcion p : patients) {
             boolean selected = editing ? edit.idPaciente() == p.idPaciente() : preselectedPatientId != null && preselectedPatientId == p.idPaciente(); %>
          <option value="<%= p.idPaciente() %>" <%= selected ? "selected" : "" %>><%= Html.escape(p.nombre()) %> · <%= Html.escape(p.documento()) %></option>
        <% } %>
      </select>
    </label>

    <label>Odontólogo
      <select id="agenda-dentist" name="id_odontologo" required>
        <option value="">Selecciona un odontólogo</option>
        <% if (dentists != null) for (OdontologoOpcion d : dentists) {
             boolean selected = editing && edit.idOdontologo() == d.idOdontologo();
             List<Long> assignedIds = dentistSpecialtyIds == null ? null : dentistSpecialtyIds.get(d.idOdontologo());
             StringBuilder specialtyIds = new StringBuilder();
             if (assignedIds != null) for (Long sid : assignedIds) {
               if (specialtyIds.length() > 0) specialtyIds.append(",");
               specialtyIds.append(sid);
             }
        %>
          <option value="<%= d.idOdontologo() %>" data-specialties="<%= specialtyIds %>" <%= selected ? "selected" : "" %>><%= Html.escape(d.nombre()) %></option>
        <% } %>
      </select>
    </label>

    <label>Especialidad <small>(opcional)</small>
      <select id="agenda-specialty" name="id_especialidad">
        <option value="">Sin especificar</option>
        <% if (specialties != null) for (EspecialidadOpcion sp : specialties) { boolean selected = editing && edit.idEspecialidad() != null && edit.idEspecialidad() == sp.id(); %>
          <option value="<%= sp.id() %>" <%= selected ? "selected" : "" %>><%= Html.escape(sp.nombre()) %></option>
        <% } %>
      </select>
      <small id="agenda-specialty-help">Primero selecciona un odontólogo. Solo se mostrarán sus especialidades asignadas.</small>
    </label>

    <label>Fecha
      <input type="date" name="fecha" min="<%= LocalDate.now() %>" required value="<%= editing ? edit.fecha() : "" %>">
    </label>
    <label>Hora de inicio
      <input type="time" name="hora_inicio" required value="<%= editing ? edit.horaInicio() : "09:00" %>">
    </label>
    <label>Hora de fin
      <input type="time" name="hora_fin" required value="<%= editing ? edit.horaFin() : "09:45" %>">
    </label>
    <label class="span-2">Motivo de la cita
      <input name="motivo" maxlength="255" required value="<%= Html.escape(editing ? edit.motivo() : "") %>" placeholder="Ej. Consulta general, control, evaluación">
    </label>
    <label class="span-2">Observaciones
      <textarea name="observaciones" maxlength="2000" rows="3" placeholder="Información adicional para coordinar la atención"><%= Html.escape(editing ? edit.observaciones() : "") %></textarea>
    </label>

    <div class="agenda-validation-tip span-2">
      <strong>Validación automática</strong>
      <span>La cita debe estar dentro de un bloque horario ACTIVO del odontólogo. También se revisan bloqueos de agenda, cruces del profesional y cruces del paciente.</span>
    </div>
    <div class="form-actions">
      <button class="btn primary" type="submit"><%= editing ? "Guardar reprogramación" : "Registrar cita" %></button>
    </div>
  </form>
</section>
<% } %>

<section class="table-card crud-list agenda-list">
  <div class="table-top">
    <div>
      <h2><%= user.role() == Role.ODONTOLOGO ? "Agenda personal" : "Citas de la clínica" %></h2>
      <span><%= canSchedule ? "Recepción y Administración coordinan la programación; Odontología registra el resultado clínico." : "Solo se muestran las citas asociadas a tu cuenta de odontólogo." %></span>
    </div>
  </div>
  <div class="table-scroll">
    <table class="agenda-table">
      <thead><tr><th>Fecha / hora</th><th>Paciente</th><% if (user.role() != Role.ODONTOLOGO) { %><th>Odontólogo</th><% } %><th>Motivo</th><th>Estado</th><th>Acciones</th></tr></thead>
      <tbody>
      <% if (appointments != null) for (CitaDetalle c : appointments) { %>
        <tr>
          <td><strong><%= c.fecha() %></strong><br><small class="table-secondary"><%= c.horaInicio() %> - <%= c.horaFin() %></small></td>
          <td><strong><%= Html.escape(c.paciente()) %></strong><br><small class="table-secondary"><%= Html.escape(c.documento()) %></small></td>
          <% if (user.role() != Role.ODONTOLOGO) { %><td><%= Html.escape(c.odontologo()) %><br><small class="table-secondary"><%= Html.escape(c.especialidad().isBlank() ? "Sin especialidad indicada" : c.especialidad()) %></small></td><% } %>
          <td><%= Html.escape(c.motivo().isBlank() ? "—" : c.motivo()) %></td>
          <td><span class="appointment-status status-<%= c.estado().toLowerCase() %>"><%= Html.escape(c.estado().replace('_',' ')) %></span></td>
          <td><div class="row-actions agenda-actions">
            <% if (canSchedule && ("PENDIENTE".equals(c.estado()) || "CONFIRMADA".equals(c.estado()))) { %>
              <a class="btn tiny secondary" href="<%= Html.escape(base) %>/agenda?editar=<%= c.idCita() %>">Editar</a>
            <% } %>

            <% if ("PENDIENTE".equals(c.estado()) && canSchedule) { %>
              <form action="<%= Html.escape(base) %>/agenda" method="post"><input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>"><input type="hidden" name="accion" value="estado"><input type="hidden" name="id_cita" value="<%= c.idCita() %>"><input type="hidden" name="estado" value="CONFIRMADA"><button class="btn tiny success-btn" type="submit">Confirmar</button></form>
              <form action="<%= Html.escape(base) %>/agenda" method="post"><input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>"><input type="hidden" name="accion" value="estado"><input type="hidden" name="id_cita" value="<%= c.idCita() %>"><input type="hidden" name="estado" value="CANCELADA"><button class="btn tiny danger-btn" type="submit">Cancelar</button></form>
            <% } %>

            <% if ("CONFIRMADA".equals(c.estado())) { %>
              <% if (user.role() == Role.ODONTOLOGO) { %>
                <a class="btn tiny success-btn" href="<%= Html.escape(base) %>/atencion?id=<%= c.idCita() %>">Atender</a>
              <% } %>
              <form action="<%= Html.escape(base) %>/agenda" method="post"><input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>"><input type="hidden" name="accion" value="estado"><input type="hidden" name="id_cita" value="<%= c.idCita() %>"><input type="hidden" name="estado" value="NO_ASISTIO"><button class="btn tiny secondary" type="submit">No asistió</button></form>
              <% if (canSchedule) { %>
                <form action="<%= Html.escape(base) %>/agenda" method="post"><input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>"><input type="hidden" name="accion" value="estado"><input type="hidden" name="id_cita" value="<%= c.idCita() %>"><input type="hidden" name="estado" value="CANCELADA"><button class="btn tiny danger-btn" type="submit">Cancelar</button></form>
              <% } %>
            <% } %>

            <% if ("ATENDIDA".equals(c.estado()) && user.role() == Role.ODONTOLOGO) { %>
              <a class="btn tiny secondary" href="<%= Html.escape(base) %>/atencion?id=<%= c.idCita() %>">Ver atención</a>
              <a class="btn tiny secondary" href="<%= Html.escape(base) %>/historias?paciente=<%= c.idPaciente() %>">Historia</a>
            <% } %>
            <% if ("CANCELADA".equals(c.estado()) || "NO_ASISTIO".equals(c.estado())) { %><span class="closed-label">Cita cerrada</span><% } %>
          </div></td>
        </tr>
      <% } %>
      <% if (appointments == null || appointments.isEmpty()) { %><tr><td colspan="<%= user.role() == Role.ODONTOLOGO ? 5 : 6 %>" class="empty">No hay citas que coincidan con los filtros.</td></tr><% } %>
      </tbody>
    </table>
  </div>
</section>

<p class="read-only"><%= canSchedule ? "Las citas no se eliminan físicamente: se cancelan para conservar la trazabilidad. Una cita cancelada o marcada como no asistió libera nuevamente el horario." : "Odontología registra la atención clínica de las citas confirmadas; al guardar la consulta, OdontoSync cambia automáticamente la cita a ATENDIDA." %></p>

<% if (canSchedule) { %>
<script src="<%= Html.escape(base) %>/js/agenda.js?v=20261010-2" defer></script>
<% } %>
<%@ include file="layout-bottom.jspf" %>
