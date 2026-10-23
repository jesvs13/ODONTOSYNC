<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.odontosync.model.AtencionCitaDetalle,pe.odontosync.model.ConsultaClinicaDetalle" %>
<%@ include file="layout-top.jspf" %>
<%
AtencionCitaDetalle a = (AtencionCitaDetalle) request.getAttribute("attention");
ConsultaClinicaDetalle c = a.consulta().orElse(null);
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
boolean editingClinical = c != null;
%>
<div class="page-heading">
  <div>
    <h1>Atención clínica</h1>
    <p>Registra diagnóstico, tratamiento y recomendaciones de la cita confirmada.</p>
  </div>
  <span class="code-chip">CU03 · ATENCIÓN CLÍNICA</span>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<section class="clinical-context-card">
  <div class="clinical-patient-head">
    <div class="patient-avatar"><%= Html.escape(a.paciente().isBlank() ? "P" : a.paciente().substring(0,1).toUpperCase()) %></div>
    <div>
      <span class="eyebrow">PACIENTE</span>
      <h2><%= Html.escape(a.paciente()) %></h2>
      <p><%= Html.escape(a.documento()) %></p>
    </div>
    <div class="clinical-head-actions">
      <span class="appointment-status status-<%= a.estadoCita().toLowerCase() %>"><%= Html.escape(a.estadoCita()) %></span>
      <a class="btn secondary" href="<%= Html.escape(base) %>/historias?paciente=<%= a.idPaciente() %>">Ver historia clínica</a>
      <a class="btn secondary" href="<%= Html.escape(base) %>/agenda">Volver a Agenda</a>
    </div>
  </div>
  <div class="clinical-context-grid">
    <div><span>Fecha</span><strong><%= a.fecha() %></strong></div>
    <div><span>Horario</span><strong><%= a.horaInicio() %> - <%= a.horaFin() %></strong></div>
    <div><span>Especialidad</span><strong><%= Html.escape(a.especialidad().isBlank() ? "Sin especificar" : a.especialidad()) %></strong></div>
    <div><span>Odontólogo</span><strong><%= Html.escape(a.odontologo()) %></strong></div>
    <div class="span-2"><span>Motivo programado</span><strong><%= Html.escape(a.motivoCita().isBlank() ? "—" : a.motivoCita()) %></strong></div>
    <div class="span-2"><span>Observación de agenda</span><strong><%= Html.escape(a.observacionesCita().isBlank() ? "—" : a.observacionesCita()) %></strong></div>
  </div>
</section>

<section class="crud-card clinical-editor">
  <div class="crud-head">
    <div>
      <h2><%= editingClinical ? "Revisar / actualizar atención" : "Registrar atención" %></h2>
      <p><%= editingClinical ? "Esta cita ya tiene una consulta clínica. Puedes completar o corregir el registro." : "Al guardar por primera vez, la cita cambiará automáticamente a ATENDIDA." %></p>
    </div>
    <% if (editingClinical) { %><span class="clinical-saved-chip">Consulta #<%= c.idConsulta() %> · <%= Html.escape(c.fechaConsulta()) %></span><% } %>
  </div>

  <form class="crud-form clinical-form" action="<%= Html.escape(base) %>/atencion" method="post" autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="id_cita" value="<%= a.idCita() %>">

    <label class="span-2">Motivo de consulta
      <textarea name="motivo_consulta" maxlength="2000" rows="2" required><%= Html.escape(editingClinical ? c.motivoConsulta() : a.motivoCita()) %></textarea>
    </label>
    <label class="span-2">Diagnóstico
      <textarea name="diagnostico" maxlength="5000" rows="4" required placeholder="Hallazgos y diagnóstico clínico"><%= Html.escape(editingClinical ? c.diagnostico() : "") %></textarea>
    </label>
    <label class="span-2">Tratamiento realizado / indicado
      <textarea name="tratamiento" maxlength="5000" rows="4" required placeholder="Procedimiento realizado o plan de tratamiento"><%= Html.escape(editingClinical ? c.tratamiento() : "") %></textarea>
    </label>
    <label>Observaciones clínicas
      <textarea name="observaciones" maxlength="5000" rows="4" placeholder="Evolución, hallazgos adicionales, tolerancia"><%= Html.escape(editingClinical ? c.observaciones() : "") %></textarea>
    </label>
    <label>Recomendaciones
      <textarea name="recomendaciones" maxlength="5000" rows="4" placeholder="Cuidados, controles o indicaciones posteriores"><%= Html.escape(editingClinical ? c.recomendaciones() : "") %></textarea>
    </label>

    <div class="clinical-integrity-tip span-2">
      <strong>TRAZABILIDAD CLÍNICA</strong>
      <span>La atención queda vinculada a esta cita y a tu cuenta de odontólogo. La historia clínica se abre automáticamente si el paciente aún no tiene una.</span>
    </div>
    <div class="form-actions">
      <a class="btn secondary" href="<%= Html.escape(base) %>/agenda">Cancelar</a>
      <button class="btn primary" type="submit"><%= editingClinical ? "Guardar cambios clínicos" : "Guardar atención" %></button>
    </div>
  </form>
</section>
<%@ include file="layout-bottom.jspf" %>
