<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.odontosync.model.RecetaResumen" %>
<%@ include file="layout-top.jspf" %>
<%
RecetaResumen recipe = (RecetaResumen) request.getAttribute("recipe");
Long returnPatientId = (Long) request.getAttribute("returnPatientId");
long patientId = returnPatientId == null ? 0L : returnPatientId.longValue();
String historyHref = base + "/historias" + (patientId > 0 ? "?paciente=" + patientId : "");
String patientName = recipe == null ? "" : recipe.paciente();
String initial = patientName.isBlank() ? "P" : patientName.substring(0, 1).toUpperCase();
%>

<div class="page-heading">
  <div>
    <h1>Receta médica</h1>
    <p>Consulta el detalle de la prescripción registrada para el paciente.</p>
  </div>
</div>

<% if (recipe != null) { %>
<section class="clinical-context-card">
  <div class="clinical-patient-head">
    <div class="patient-avatar"><%= Html.escape(initial) %></div>
    <div>
      <span class="eyebrow">PACIENTE</span>
      <h2><%= Html.escape(recipe.paciente()) %></h2>
      <p>Receta #<%= recipe.idReceta() %></p>
    </div>
    <div class="clinical-head-actions">
      <a class="btn secondary" href="<%= Html.escape(historyHref) %>">Volver a Historia clínica</a>
      <a class="btn secondary" href="<%= Html.escape(base) %>/modulo/recetas">Ir a Recetas</a>
    </div>
  </div>

  <div class="clinical-context-grid">
    <div>
      <span>Fecha de emisión</span>
      <strong><%= Html.escape(recipe.fecha()) %></strong>
    </div>
    <div>
      <span>Estado</span>
      <strong>Prescripción registrada</strong>
    </div>
  </div>
</section>

<section class="crud-card clinical-editor">
  <div class="crud-head">
    <div>
      <h2>Detalle de la prescripción</h2>
      <p>Información registrada en la receta médica seleccionada.</p>
    </div>
  </div>

  <article class="consultation-entry">
    <div class="consultation-body">
      <div>
        <span>Medicamentos</span>
        <p><%= Html.escape(recipe.medicamentos()) %></p>
      </div>
      <div>
        <span>Indicaciones</span>
        <p><%= Html.escape(recipe.indicaciones()) %></p>
      </div>
    </div>
  </article>
</section>

<p class="read-only">
  Esta vista es solo de consulta. PDF, impresión y WhatsApp se integrarán en pasos posteriores.
</p>
<% } %>

<%@ include file="layout-bottom.jspf" %>
