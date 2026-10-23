<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,pe.odontosync.model.*" %>
<%@ include file="layout-top.jspf" %>
<%
List<HistoriaPacienteResumen> items = (List<HistoriaPacienteResumen>) request.getAttribute("historyList");
HistoriaPacienteVista view = (HistoriaPacienteVista) request.getAttribute("historyView");
String query = (String) request.getAttribute("historyQuery");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
%>
<div class="page-heading">
  <div>
    <h1>Historia clínica</h1>
    <p>Consulta y actualiza la información clínica longitudinal de tus pacientes.</p>
  </div>
  <span class="code-chip">CU03 · HISTORIA CLÍNICA</span>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<section class="patient-toolbar history-toolbar">
  <form class="patient-search" action="<%= Html.escape(base) %>/historias" method="get">
    <label>Buscar paciente o documento</label>
    <div class="search-row">
      <input name="q" maxlength="80" value="<%= Html.escape(query == null ? "" : query) %>" placeholder="Nombre, apellido o documento">
      <button class="btn primary" type="submit">Buscar</button>
      <a class="btn secondary" href="<%= Html.escape(base) %>/historias">Limpiar</a>
    </div>
  </form>
  <div class="patient-toolbar-note"><strong><%= items == null ? 0 : items.size() %></strong><span>pacientes vinculados</span></div>
</section>

<section class="table-card history-list-card">
  <div class="table-top">
    <div><h2>Mis historias</h2><span>Solo pacientes vinculados a tus citas.</span></div>
  </div>
  <div class="table-scroll">
    <table>
      <thead><tr><th>Paciente</th><th>Documento</th><th>Apertura</th><th>Estado</th><th>Última consulta</th><th></th></tr></thead>
      <tbody>
      <% if (items != null) for (HistoriaPacienteResumen h : items) { %>
        <tr>
          <td><strong><%= Html.escape(h.paciente()) %></strong></td>
          <td><%= Html.escape(h.documento()) %></td>
          <td><%= Html.escape(h.apertura()) %></td>
          <td><span class="history-status <%= "ACTIVA".equals(h.estado()) ? "active" : "pending" %>"><%= Html.escape(h.estado()) %></span></td>
          <td><%= Html.escape(h.ultimaConsulta()) %></td>
          <td><a class="btn tiny secondary" href="<%= Html.escape(base) %>/historias?paciente=<%= h.idPaciente() %>">Abrir</a></td>
        </tr>
      <% } %>
      <% if (items == null || items.isEmpty()) { %><tr><td class="empty" colspan="6">No hay pacientes que coincidan con la búsqueda.</td></tr><% } %>
      </tbody>
    </table>
  </div>
</section>

<% if (view != null) {
   PacienteDetalle p = view.paciente();
   HistoriaClinicaDetalle h = view.historia().orElse(null);
%>
<section class="patient-file clinical-history-file">
  <div class="patient-file-head">
    <div class="patient-avatar"><%= Html.escape(p.nombres().isBlank() ? "P" : p.nombres().substring(0,1).toUpperCase()) %></div>
    <div>
      <span class="eyebrow">HISTORIA DEL PACIENTE</span>
      <h2><%= Html.escape(p.nombreCompleto()) %></h2>
      <p><%= Html.escape(p.tipoDocumento()) %> <%= Html.escape(p.numeroDocumento()) %> · <%= Html.escape(p.estado()) %></p>
    </div>
    <div class="patient-file-actions">
      <span class="history-status <%= h != null && "ACTIVA".equals(h.estado()) ? "active" : "pending" %>"><%= h == null ? "SIN ABRIR" : Html.escape(h.estado()) %></span>
      <a class="btn secondary" href="<%= Html.escape(base) %>/pacientes?ficha=<%= p.id() %>">Ficha paciente</a>
    </div>
  </div>

  <div class="patient-data-grid clinical-patient-summary">
    <div><span>Fecha nacimiento</span><strong><%= p.fechaNacimiento() == null ? "—" : p.fechaNacimiento() %></strong></div>
    <div><span>Sexo</span><strong><%= Html.escape(p.sexo() == null ? "—" : p.sexo()) %></strong></div>
    <div><span>Teléfono</span><strong><%= Html.escape(p.telefono() == null ? "—" : p.telefono()) %></strong></div>
    <div><span>Historia abierta</span><strong><%= h == null ? "Se abrirá al guardar datos clínicos" : Html.escape(h.fechaApertura()) %></strong></div>
  </div>

  <div class="clinical-history-grid">
    <section class="patient-section history-notes-section">
      <div class="section-title"><div><span>RESUMEN</span><h3>Observaciones generales</h3></div></div>
      <form action="<%= Html.escape(base) %>/historias" method="post" class="clinical-inline-form">
        <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
        <input type="hidden" name="accion" value="notas">
        <input type="hidden" name="id_paciente" value="<%= p.id() %>">
        <textarea name="observaciones_generales" maxlength="5000" rows="5" placeholder="Resumen clínico relevante del paciente"><%= Html.escape(h == null ? "" : h.observacionesGenerales()) %></textarea>
        <button class="btn primary" type="submit">Guardar observaciones</button>
      </form>
    </section>

    <section class="patient-section">
      <div class="section-title"><div><span>SEGURIDAD</span><h3>Alergias</h3></div><span><%= view.alergias().size() %> registros</span></div>
      <div class="stack-list clinical-stack">
        <% for (AlergiaClinicaDetalle x : view.alergias()) { %>
          <div class="stack-item allergy-item"><strong><%= Html.escape(x.alergeno()) %></strong><span><%= Html.escape(x.reaccion().isBlank() ? "Reacción no especificada" : x.reaccion()) %></span><small><%= Html.escape(x.observaciones()) %> · <%= Html.escape(x.fechaRegistro()) %></small></div>
        <% } %>
        <% if (view.alergias().isEmpty()) { %><p class="empty-note">No hay alergias registradas.</p><% } %>
      </div>
      <form action="<%= Html.escape(base) %>/historias" method="post" class="mini-clinical-form">
        <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
        <input type="hidden" name="accion" value="alergia">
        <input type="hidden" name="id_paciente" value="<%= p.id() %>">
        <input name="alergeno" maxlength="150" required placeholder="Alérgeno">
        <input name="reaccion" maxlength="255" placeholder="Reacción">
        <textarea name="observaciones" maxlength="2000" rows="2" placeholder="Observaciones"></textarea>
        <button class="btn secondary" type="submit">Agregar alergia</button>
      </form>
    </section>

    <section class="patient-section">
      <div class="section-title"><div><span>ANTECEDENTES</span><h3>Antecedentes médicos</h3></div><span><%= view.antecedentes().size() %> registros</span></div>
      <div class="stack-list clinical-stack">
        <% for (AntecedenteClinicoDetalle x : view.antecedentes()) { %>
          <div class="stack-item"><strong><%= Html.escape(x.tipo()) %></strong><span><%= Html.escape(x.descripcion()) %></span><small><%= Html.escape(x.observaciones()) %> · <%= Html.escape(x.fechaRegistro()) %></small></div>
        <% } %>
        <% if (view.antecedentes().isEmpty()) { %><p class="empty-note">No hay antecedentes registrados.</p><% } %>
      </div>
      <form action="<%= Html.escape(base) %>/historias" method="post" class="mini-clinical-form">
        <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
        <input type="hidden" name="accion" value="antecedente">
        <input type="hidden" name="id_paciente" value="<%= p.id() %>">
        <input name="tipo" maxlength="100" required placeholder="Tipo: Médico, familiar, quirúrgico...">
        <input name="descripcion" maxlength="255" required placeholder="Descripción">
        <textarea name="observaciones" maxlength="2000" rows="2" placeholder="Observaciones"></textarea>
        <button class="btn secondary" type="submit">Agregar antecedente</button>
      </form>
    </section>

    <section class="patient-section patient-section-wide clinical-consultations">
      <div class="section-title"><div><span>EVOLUCIÓN</span><h3>Consultas clínicas</h3></div><span><%= view.consultas().size() %> atenciones</span></div>
      <div class="consultation-timeline">
        <% for (ConsultaHistorialDetalle x : view.consultas()) { %>
          <article class="consultation-entry">
            <div class="consultation-meta"><strong><%= Html.escape(x.fechaConsulta()) %></strong><span><%= Html.escape(x.odontologo()) %> · <%= Html.escape(x.especialidad().isBlank() ? "Sin especialidad indicada" : x.especialidad()) %></span></div>
            <div class="consultation-body">
              <div><span>Motivo</span><p><%= Html.escape(x.motivo().isBlank() ? "—" : x.motivo()) %></p></div>
              <div><span>Diagnóstico</span><p><%= Html.escape(x.diagnostico().isBlank() ? "—" : x.diagnostico()) %></p></div>
              <div><span>Tratamiento</span><p><%= Html.escape(x.tratamiento().isBlank() ? "—" : x.tratamiento()) %></p></div>
              <div><span>Recomendaciones</span><p><%= Html.escape(x.recomendaciones().isBlank() ? "—" : x.recomendaciones()) %></p></div>
            </div>
            <a class="btn tiny secondary" href="<%= Html.escape(base) %>/atencion?id=<%= x.idCita() %>">Ver atención</a>
          </article>
        <% } %>
        <% if (view.consultas().isEmpty()) { %><p class="empty-note">Todavía no existen atenciones clínicas registradas para este paciente.</p><% } %>
      </div>
    </section>
  </div>
</section>
<% } %>
<p class="read-only">Los antecedentes y alergias se agregan sin eliminación física para conservar la trazabilidad clínica del paciente.</p>
<%@ include file="layout-bottom.jspf" %>
