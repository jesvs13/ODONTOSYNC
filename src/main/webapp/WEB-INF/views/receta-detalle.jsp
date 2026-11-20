<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.odontosync.model.RecetaResumen" %>
<%@ page import="pe.odontosync.model.PacienteDetalle" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ include file="layout-top.jspf" %>
<%
RecetaResumen recipe = (RecetaResumen) request.getAttribute("recipe");
PacienteDetalle recipePatient = (PacienteDetalle) request.getAttribute("recipePatient");

Long returnPatientId = (Long) request.getAttribute("returnPatientId");
long patientId = returnPatientId == null ? 0L : returnPatientId.longValue();
String historyHref = base + "/historias" + (patientId > 0 ? "?paciente=" + patientId : "");

String patientName = recipePatient != null
    ? recipePatient.nombreCompleto()
    : (recipe == null ? "" : recipe.paciente());
String initial = patientName.isBlank() ? "P" : patientName.substring(0, 1).toUpperCase();

String patientPhone = recipePatient == null || recipePatient.telefono() == null
    ? ""
    : recipePatient.telefono().trim();

String whatsappNumber = patientPhone.replaceAll("\\D", "");
if (whatsappNumber.length() == 9 && whatsappNumber.startsWith("9")) {
    whatsappNumber = "51" + whatsappNumber;
} else if (!(whatsappNumber.length() == 11 && whatsappNumber.startsWith("519"))) {
    whatsappNumber = "";
}

String whatsappMessage = recipe == null
    ? ""
    : "Hola " + patientName
        + ". Le compartimos su receta médica N.° " + recipe.idReceta()
        + " emitida el " + recipe.fecha()
        + ". El PDF de la receta ha sido generado desde OdontoSync.";

String whatsappHref = whatsappNumber.isBlank()
    ? ""
    : "https://wa.me/" + whatsappNumber + "?text="
        + URLEncoder.encode(whatsappMessage, StandardCharsets.UTF_8).replace("+", "%20");

String pdfHref = recipe == null
    ? ""
    : base + "/modulo/recetas/pdf?id=" + recipe.idReceta();
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
      <h2><%= Html.escape(patientName) %></h2>
      <p>Receta #<%= recipe.idReceta() %></p>
    </div>

    <div class="clinical-head-actions">
      <a class="btn secondary" href="<%= Html.escape(historyHref) %>">
        Volver a Historia clínica
      </a>

      <a class="btn secondary" href="<%= Html.escape(base) %>/modulo/recetas">
        Ir a Recetas
      </a>

      <a class="btn primary" href="<%= Html.escape(pdfHref) %>">
        Descargar PDF
      </a>

      <% if (!whatsappHref.isBlank()) { %>
        <a
          id="whatsappRecipe"
          class="btn secondary"
          href="<%= Html.escape(whatsappHref) %>"
          target="_blank"
          rel="noopener"
          data-pdf-url="<%= Html.escape(pdfHref) %>">
          WhatsApp
        </a>
      <% } else { %>
        <button
          class="btn secondary"
          type="button"
          disabled
          title="El paciente no tiene un número móvil válido registrado en su ficha.">
          WhatsApp
        </button>
      <% } %>
    </div>
  </div>

  <div class="clinical-context-grid">
    <div>
      <span>Fecha de emisión</span>
      <strong><%= Html.escape(recipe.fecha()) %></strong>
    </div>
    <div>
      <span>Teléfono</span>
      <strong><%= Html.escape(patientPhone.isBlank() ? "No registrado" : patientPhone) %></strong>
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

<% if (whatsappHref.isBlank()) { %>
<p class="read-only no-print">
  WhatsApp no está disponible porque la ficha del paciente no tiene un número móvil peruano válido.
</p>
<% } else { %>
<p class="read-only no-print">
  Al usar WhatsApp se abrirá el chat del número registrado y se descargará el PDF de la receta para adjuntarlo.
</p>
<% } %>

<script src="<%= Html.escape(base) %>/js/receta-detalle.js?v=20261120-4" defer></script>
<% } %>

<%@ include file="layout-bottom.jspf" %>
