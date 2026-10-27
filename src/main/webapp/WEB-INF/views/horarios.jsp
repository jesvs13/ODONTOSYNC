<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,pe.odontosync.Role,pe.odontosync.model.HorarioDetalle,pe.odontosync.model.OdontologoOpcion" %>
<%@ include file="layout-top.jspf" %>
<link rel="stylesheet" href="<%= Html.escape(base) %>/css/horarios-dialog.css?v=20261024">
<%
List<HorarioDetalle> schedules = (List<HorarioDetalle>) request.getAttribute("horarios");
List<OdontologoOpcion> dentists = (List<OdontologoOpcion>) request.getAttribute("odontologosHorario");
HorarioDetalle edit = (HorarioDetalle) request.getAttribute("editSchedule");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
boolean canManage = Boolean.TRUE.equals(request.getAttribute("canManageSchedules"));
boolean editing = edit != null;
%>
<div class="page-heading">
  <div>
    <h1><%= user.role() == Role.ODONTOLOGO ? "Mis horarios" : "Horarios" %></h1>
    <p><%= user.role() == Role.ODONTOLOGO ? "Consulta tu disponibilidad semanal registrada." : "Disponibilidad semanal del equipo odontológico." %></p>
  </div>

  <div class="schedule-heading-actions">

    <% if (canManage) { %>
      <button class="btn primary" type="button" data-open-schedule-dialog>
        + Nuevo horario
      </button>
    <% } %>
  </div>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<% if (canManage) { %>
<dialog class="crud-card crud-dialog schedule-dialog"
        data-schedule-dialog
        data-auto-open="<%= editing %>">
  <div class="crud-head">
    <div>
      <h2><%= editing ? "Editar bloque horario" : "Agregar bloque horario" %></h2>
      <p>Los bloques activos de un mismo odontólogo no pueden cruzarse.</p>
    </div>
    <a class="btn secondary" href="<%= Html.escape(base) %>/horarios">Cancelar</a>
  </div>

  <form class="crud-form schedule-form" action="<%= Html.escape(base) %>/horarios" method="post">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="<%= editing ? "actualizar" : "crear" %>">
    <% if (editing) { %><input type="hidden" name="id_horario" value="<%= edit.idHorario() %>"><% } %>

    <label>Odontólogo
      <select name="id_odontologo" required>
        <option value="">Selecciona un odontólogo</option>
        <% if (dentists != null) for (OdontologoOpcion d : dentists) { boolean selected = editing && edit.idOdontologo() == d.idOdontologo(); %>
          <option value="<%= d.idOdontologo() %>" <%= selected ? "selected" : "" %> <%= !selected && "INACTIVO".equals(d.estado()) ? "disabled" : "" %>><%= Html.escape(d.nombre()) %><%= "INACTIVO".equals(d.estado()) ? " · INACTIVO" : "" %></option>
        <% } %>
      </select>
    </label>
    <label>Día
      <% int selectedDay = editing ? edit.diaSemana() : 1; %>
      <select name="dia_semana" required>
        <% String[] dayNames = {"","Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"};
           for (int d=1; d<=7; d++) { %>
          <option value="<%= d %>" <%= selectedDay == d ? "selected" : "" %>><%= dayNames[d] %></option>
        <% } %>
      </select>
    </label>
    <label>Hora de inicio
      <input type="time" name="hora_inicio" required value="<%= editing ? edit.horaInicio() : "08:00" %>">
    </label>
    <label>Hora de fin
      <input type="time" name="hora_fin" required value="<%= editing ? edit.horaFin() : "12:00" %>">
    </label>
    <label>Estado
      <% String selectedStatus = editing ? edit.estado() : "ACTIVO"; %>
      <select name="estado" required>
        <option value="ACTIVO" <%= "ACTIVO".equals(selectedStatus) ? "selected" : "" %>>ACTIVO</option>
        <option value="INACTIVO" <%= "INACTIVO".equals(selectedStatus) ? "selected" : "" %>>INACTIVO</option>
      </select>
    </label>
    <div class="schedule-tip"><strong>Validación automática</strong><span>OdontoSync impide registrar dos bloques activos que se superpongan el mismo día para el mismo profesional.</span></div>

    <div class="form-actions">
      <button class="btn primary" type="submit"><%= editing ? "Guardar cambios" : "Agregar horario" %></button>
    </div>
   </form>
</dialog>
<% } %>

<section class="schedule-summary">
  <% int activeCount = 0; if (schedules != null) for (HorarioDetalle h : schedules) if ("ACTIVO".equals(h.estado())) activeCount++; %>
  <div><span>Bloques registrados</span><strong><%= schedules == null ? 0 : schedules.size() %></strong></div>
  <div><span>Bloques activos</span><strong><%= activeCount %></strong></div>
  <div><span>Vista</span><strong class="summary-word"><%= user.role() == Role.ODONTOLOGO ? "Personal" : "Clínica" %></strong></div>
</section>

<section class="table-card crud-list schedule-list">
  <div class="table-top">
    <div><h2>Disponibilidad semanal</h2><span><%= canManage ? "Administración puede editar, desactivar o reactivar bloques." : "Vista de consulta según tu perfil." %></span></div>
  </div>
  <div class="table-scroll">
    <table>
      <thead><tr><% if (user.role() != Role.ODONTOLOGO) { %><th>Odontólogo</th><% } %><th>Día</th><th>Inicio</th><th>Fin</th><th>Estado</th><% if (canManage) { %><th>Acciones</th><% } %></tr></thead>
      <tbody>
      <% if (schedules != null) for (HorarioDetalle h : schedules) { %>
        <tr>
          <% if (user.role() != Role.ODONTOLOGO) { %><td><strong><%= Html.escape(h.odontologo()) %></strong></td><% } %>
          <td><span class="day-chip"><%= Html.escape(h.diaNombre()) %></span></td>
          <td><%= h.horaInicio() %></td>
          <td><%= h.horaFin() %></td>
          <td><span class="status-pill <%= "INACTIVO".equals(h.estado()) ? "status-inactive" : "" %>"><%= Html.escape(h.estado()) %></span></td>
          <% if (canManage) { %><td><div class="row-actions">
            <a class="btn tiny secondary" href="<%= Html.escape(base) %>/horarios?editar=<%= h.idHorario() %>">Editar</a>
            <form action="<%= Html.escape(base) %>/horarios" method="post">
              <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
              <input type="hidden" name="accion" value="estado">
              <input type="hidden" name="id_horario" value="<%= h.idHorario() %>">
              <input type="hidden" name="estado" value="<%= "ACTIVO".equals(h.estado()) ? "INACTIVO" : "ACTIVO" %>">
              <button class="btn tiny <%= "ACTIVO".equals(h.estado()) ? "danger-btn" : "success-btn" %>" type="submit"><%= "ACTIVO".equals(h.estado()) ? "Desactivar" : "Activar" %></button>
            </form>
          </div></td><% } %>
        </tr>
      <% } %>
      <% if (schedules == null || schedules.isEmpty()) { %><tr><td colspan="<%= canManage ? (user.role()!=Role.ODONTOLOGO ? 6 : 5) : (user.role()!=Role.ODONTOLOGO ? 5 : 4) %>" class="empty">No hay horarios registrados para esta vista.</td></tr><% } %>
      </tbody>
    </table>
  </div>
</section>

<p class="read-only"><%= canManage ? "Al inactivar un odontólogo, sus bloques horarios activos también pasan a INACTIVO para evitar disponibilidad incorrecta." : user.role() == Role.ODONTOLOGO ? "Los cambios de disponibilidad son gestionados por Administración." : "Recepción consulta la disponibilidad para coordinar citas; la configuración corresponde a Administración." %></p>
<script src="<%= Html.escape(base) %>/js/horarios-dialog.js" defer></script>
<%@ include file="layout-bottom.jspf" %>
