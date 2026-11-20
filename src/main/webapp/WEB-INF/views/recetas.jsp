<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,pe.odontosync.model.PacienteOpcion,pe.odontosync.model.RecetaResumen" %>
<%@ include file="layout-top.jspf" %>
<%
List<PacienteOpcion> patients = (List<PacienteOpcion>) request.getAttribute("patients");
List<RecetaResumen> history = (List<RecetaResumen>) request.getAttribute("recipeHistory");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
Long selectedPatientId = (Long) request.getAttribute("selectedPatientId");
long selected = selectedPatientId == null ? 0L : selectedPatientId.longValue();
%>

<div class="page-heading">
  <div>
    <h1>Recetas médicas</h1>
    <p>Emite prescripciones para pacientes vinculados a tus citas y consulta el historial registrado.</p>
  </div>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<section class="crud-card">
  <div class="crud-head">
    <div>
      <h2>Emitir receta</h2>
      <p>La receta quedará asociada a tu cuenta de odontólogo y al paciente seleccionado.</p>
    </div>
  </div>

  <% if (patients != null && !patients.isEmpty()) { %>
  <form class="crud-form" action="<%= Html.escape(base) %>/modulo/recetas" method="post" autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="crear">

    <label class="span-2">Paciente
      <select name="id_paciente" required>
        <option value="">Selecciona un paciente</option>
        <% for (PacienteOpcion p : patients) { %>
          <option value="<%= p.idPaciente() %>" <%= selected == p.idPaciente() ? "selected" : "" %>>
            <%= Html.escape(p.nombre() + " · " + p.documento()) %>
          </option>
        <% } %>
      </select>
      <small>Solo aparecen pacientes vinculados a tus citas dentro de la clínica.</small>
    </label>

    <label class="span-2">Medicamentos
      <textarea name="medicamentos" maxlength="5000" rows="5" required
        placeholder="Ejemplo: Ibuprofeno 400 mg, 1 tableta cada 8 horas por 3 días."></textarea>
    </label>

    <label class="span-2">Indicaciones
      <textarea name="indicaciones" maxlength="5000" rows="5" required
        placeholder="Cuidados, duración, restricciones y otras indicaciones para el paciente."></textarea>
    </label>

    <div class="clinical-integrity-tip span-2">
      <strong>TRAZABILIDAD</strong>
      <span>La receta se registra con el paciente, la clínica y tu cuenta de odontólogo. No se permite emitir recetas para pacientes ajenos a tus citas.</span>
    </div>

    <div class="form-actions">
      <button class="btn primary" type="submit">Emitir receta</button>
    </div>
  </form>
  <% } else { %>
    <p class="read-only">No tienes pacientes activos vinculados a tus citas para emitir una receta.</p>
  <% } %>
</section>

<section class="table-card crud-list">
  <div class="table-top">
    <div>
      <h2>Recetas emitidas</h2>
      <span>Últimas prescripciones registradas por tu cuenta de odontólogo.</span>
    </div>
  </div>
  <div class="table-scroll">
    <table>
      <thead>
        <tr><th>Fecha</th><th>Paciente</th><th>Medicamentos</th><th>Indicaciones</th></tr>
      </thead>
      <tbody>
      <% if (history != null) for (RecetaResumen r : history) { %>
        <tr>
          <td><%= Html.escape(r.fecha()) %></td>
          <td><strong><%= Html.escape(r.paciente()) %></strong></td>
          <td><%= Html.escape(r.medicamentos()) %></td>
          <td><%= Html.escape(r.indicaciones()) %></td>
        </tr>
      <% } %>
      <% if (history == null || history.isEmpty()) { %>
        <tr><td colspan="4" class="empty">Todavía no existen recetas emitidas por tu cuenta.</td></tr>
      <% } %>
      </tbody>
    </table>
  </div>
</section>

<p class="read-only">Las recetas también quedan visibles dentro de la ficha longitudinal del paciente.</p>
<%@ include file="layout-bottom.jspf" %>
