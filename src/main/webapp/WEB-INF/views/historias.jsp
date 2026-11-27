<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.net.URLEncoder,java.nio.charset.StandardCharsets,pe.odontosync.model.*" %>
<%@ include file="layout-top.jspf" %>
<link rel="stylesheet" href="<%= Html.escape(base) %>/css/historias-ui.css?v=31-2">
<%
List<HistoriaPacienteResumen> items =
    (List<HistoriaPacienteResumen>) request.getAttribute("historyList");
HistoriaPacienteVista view =
    (HistoriaPacienteVista) request.getAttribute("historyView");
List<RecetaResumen> patientRecipes =
    (List<RecetaResumen>) request.getAttribute("patientRecipes");
List<PlacaDetalle> patientRadiographs = (List<PlacaDetalle>) request.getAttribute("patientRadiographs");
if (patientRadiographs == null) patientRadiographs = List.of();
long activeRadiographs = patientRadiographs.stream().filter(rx -> "ACTIVA".equals(rx.estado())).count();
String query = (String) request.getAttribute("historyQuery");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
String historyTab = (String) request.getAttribute("historyTab");
if (historyTab == null) historyTab = "resumen";
%>

<div class="page-heading clinical-page-heading">
  <div>
    <h1>Historia clínica</h1>
    <p>Consulta la evolución clínica, antecedentes, alergias, recetas y radiografías de tus pacientes en un solo lugar.</p>
  </div>

  <% if (view != null) { %>
  <div class="clinical-page-actions">
    <a class="btn secondary" href="<%= Html.escape(base) %>/historias">← Volver a historias</a>
  </div>
  <% } %>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<% if (view == null) { %>
<section class="patient-toolbar history-toolbar history-toolbar-compact">
  <form class="patient-search" action="<%= Html.escape(base) %>/historias" method="get">
    <label>Buscar paciente o documento</label>
    <div class="search-row">
      <input name="q"
             maxlength="80"
             value="<%= Html.escape(query == null ? "" : query) %>"
             placeholder="Nombre, apellido o documento">
      <button class="btn primary" type="submit">Buscar</button>
      <a class="btn secondary" href="<%= Html.escape(base) %>/historias">Limpiar</a>
    </div>
  </form>

  <div class="patient-toolbar-note">
    <strong><%= items == null ? 0 : items.size() %></strong>
    <span>pacientes vinculados</span>
  </div>
</section>

<section class="table-card history-list-card history-list-card-compact">
  <div class="table-top">
    <div>
      <h2>Mis historias</h2>
      <span>Solo pacientes vinculados a tus citas dentro de la clínica.</span>
    </div>
  </div>

  <div class="table-scroll history-table-scroll">
    <table>
      <thead>
        <tr>
          <th>Paciente</th>
          <th>Documento</th>
          <th>Apertura</th>
          <th>Estado</th>
          <th>Última consulta</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
      <% if (items != null) for (HistoriaPacienteResumen h : items) { %>
        <tr>
          <td><strong><%= Html.escape(h.paciente()) %></strong></td>
          <td><%= Html.escape(h.documento()) %></td>
          <td><%= Html.escape(h.apertura()) %></td>
          <td>
            <span class="history-status <%= "ACTIVA".equals(h.estado()) ? "active" : "pending" %>">
              <%= Html.escape(h.estado()) %>
            </span>
          </td>
          <td><%= Html.escape(h.ultimaConsulta()) %></td>
          <td>
            <a class="btn tiny primary"
               href="<%= Html.escape(base) %>/historias?paciente=<%= h.idPaciente() %>">
              Abrir historia
            </a>
          </td>
        </tr>
      <% } %>

      <% if (items == null || items.isEmpty()) { %>
        <tr>
          <td class="empty" colspan="6">No hay pacientes que coincidan con la búsqueda.</td>
        </tr>
      <% } %>
      </tbody>
    </table>
  </div>
</section>

<p class="read-only">
  Al abrir un paciente accederás a su resumen, antecedentes, consultas y recetas sin cargar todo el contenido en una sola pantalla.
</p>

<% } else {
   PacienteDetalle p = view.paciente();
   HistoriaClinicaDetalle h = view.historia().orElse(null);

   String patientPhone = p.telefono() == null ? "" : p.telefono().trim();
   String patientWhatsappNumber = patientPhone.replaceAll("\\D", "");
   if (patientWhatsappNumber.length() == 9 && patientWhatsappNumber.startsWith("9")) {
       patientWhatsappNumber = "51" + patientWhatsappNumber;
   } else if (!(patientWhatsappNumber.length() == 11 && patientWhatsappNumber.startsWith("519"))) {
       patientWhatsappNumber = "";
   }
%>

<section class="patient-file clinical-history-file clinical-history-file-compact">
  <div class="patient-file-head">
    <div class="patient-avatar">
      <%= Html.escape(p.nombres().isBlank() ? "P" : p.nombres().substring(0,1).toUpperCase()) %>
    </div>

    <div class="clinical-patient-title">
      <span class="eyebrow">HISTORIA DEL PACIENTE</span>
      <h2><%= Html.escape(p.nombreCompleto()) %></h2>
      <p>
        <%= Html.escape(p.tipoDocumento()) %>
        <%= Html.escape(p.numeroDocumento()) %>
        ·
        <%= Html.escape(p.estado()) %>
      </p>
    </div>

    <div class="patient-file-actions">
      <span class="history-status <%= h != null && "ACTIVA".equals(h.estado()) ? "active" : "pending" %>">
        <%= h == null ? "SIN ABRIR" : Html.escape(h.estado()) %>
      </span>
      <button class="btn secondary"
              type="button"
              data-open-clinical-dialog="patient-file">
        Ficha paciente
      </button>
    </div>
  </div>

  <div class="patient-data-grid clinical-patient-summary clinical-summary-compact">
    <div>
      <span>Fecha nacimiento</span>
      <strong><%= p.fechaNacimiento() == null ? "—" : p.fechaNacimiento() %></strong>
    </div>
    <div>
      <span>Sexo</span>
      <strong><%= Html.escape(p.sexo() == null ? "—" : p.sexo()) %></strong>
    </div>
    <div>
      <span>Teléfono</span>
      <strong><%= Html.escape(p.telefono() == null ? "—" : p.telefono()) %></strong>
    </div>
    <div>
      <span>Historia abierta</span>
      <strong><%= h == null ? "Pendiente" : Html.escape(h.fechaApertura()) %></strong>
    </div>
  </div>
</section>


<dialog class="crud-card crud-dialog clinical-dialog clinical-patient-dialog"
        data-clinical-dialog="patient-file">
  <div class="crud-head clinical-modal-head">
    <div>
      <span class="clinical-dialog-eyebrow">FICHA DEL PACIENTE</span>
      <h2><%= Html.escape(p.nombreCompleto()) %></h2>
      <p>Datos personales y de contacto registrados para este paciente.</p>
    </div>
    <button class="btn secondary" type="button" data-close-clinical-dialog>Cerrar</button>
  </div>

  <div class="clinical-patient-file-grid">
    <div><span>Tipo de documento</span><strong><%= Html.escape(p.tipoDocumento()) %></strong></div>
    <div><span>Número de documento</span><strong><%= Html.escape(p.numeroDocumento()) %></strong></div>
    <div><span>Nombres</span><strong><%= Html.escape(p.nombres()) %></strong></div>
    <div><span>Apellidos</span><strong><%= Html.escape(p.apellidos()) %></strong></div>
    <div><span>Fecha de nacimiento</span><strong><%= p.fechaNacimiento() == null ? "—" : p.fechaNacimiento() %></strong></div>
    <div><span>Sexo</span><strong><%= Html.escape(p.sexo() == null || p.sexo().isBlank() ? "—" : p.sexo()) %></strong></div>
    <div><span>Teléfono</span><strong><%= Html.escape(patientPhone.isBlank() ? "No registrado" : patientPhone) %></strong></div>
    <div><span>Correo</span><strong><%= Html.escape(p.correo() == null || p.correo().isBlank() ? "No registrado" : p.correo()) %></strong></div>
    <div class="span-2"><span>Dirección</span><strong><%= Html.escape(p.direccion() == null || p.direccion().isBlank() ? "No registrada" : p.direccion()) %></strong></div>
    <div><span>Ocupación</span><strong><%= Html.escape(p.ocupacion() == null || p.ocupacion().isBlank() ? "No registrada" : p.ocupacion()) %></strong></div>
    <div><span>Estado</span><strong><%= Html.escape(p.estado()) %></strong></div>
    <div><span>Contacto de emergencia</span><strong><%= Html.escape(p.contactoEmergencia() == null || p.contactoEmergencia().isBlank() ? "No registrado" : p.contactoEmergencia()) %></strong></div>
    <div><span>Teléfono de emergencia</span><strong><%= Html.escape(p.telefonoEmergencia() == null || p.telefonoEmergencia().isBlank() ? "No registrado" : p.telefonoEmergencia()) %></strong></div>
  </div>

  <div class="clinical-modal-footer">
    <span>Esta ficha se consulta sin salir de la historia clínica actual.</span>
    <button class="btn primary" type="button" data-close-clinical-dialog>Cerrar ficha</button>
  </div>
</dialog>

<nav class="clinical-tabs" aria-label="Secciones de historia clínica">
  <a class="<%= "resumen".equals(historyTab) ? "active" : "" %>"
     href="<%= Html.escape(base) %>/historias?paciente=<%= p.id() %>&tab=resumen">
    Resumen
  </a>

  <a class="<%= "antecedentes".equals(historyTab) ? "active" : "" %>"
     href="<%= Html.escape(base) %>/historias?paciente=<%= p.id() %>&tab=antecedentes">
    Antecedentes y alergias
    <span><%= view.antecedentes().size() + view.alergias().size() %></span>
  </a>

  <a class="<%= "consultas".equals(historyTab) ? "active" : "" %>"
     href="<%= Html.escape(base) %>/historias?paciente=<%= p.id() %>&tab=consultas">
    Consultas
    <span><%= view.consultas().size() %></span>
  </a>

  <a class="<%= "recetas".equals(historyTab) ? "active" : "" %>"
     href="<%= Html.escape(base) %>/historias?paciente=<%= p.id() %>&tab=recetas">
    Recetas
    <span><%= patientRecipes == null ? 0 : patientRecipes.size() %></span>
  </a>
  <a class="<%= "radiografias".equals(historyTab) ? "active" : "" %>"
     href="<%= Html.escape(base) %>/historias?paciente=<%= p.id() %>&amp;tab=radiografias">
    Radiografías <span><%= activeRadiographs %></span>
  </a>
</nav>

<% if ("resumen".equals(historyTab)) {
   String generalNote = h == null || h.observacionesGenerales() == null
       ? ""
       : h.observacionesGenerales().trim();
%>
<section class="clinical-summary-dashboard">
  <div class="clinical-summary-metrics">
    <article>
      <span>Estado de historia</span>
      <strong><%= h == null ? "SIN ABRIR" : Html.escape(h.estado()) %></strong>
      <small><%= h == null ? "Sin apertura registrada" : "Apertura: " + Html.escape(h.fechaApertura()) %></small>
    </article>

    <article class="<%= view.alergias().isEmpty() ? "" : "clinical-metric-alert" %>">
      <span>Alergias</span>
      <strong><%= view.alergias().size() %></strong>
      <small><%= view.alergias().isEmpty() ? "Sin alergias registradas" : "Revisar antes de atender" %></small>
    </article>

    <article>
      <span>Antecedentes</span>
      <strong><%= view.antecedentes().size() %></strong>
      <small>Registros médicos asociados</small>
    </article>

    <article>
      <span>Consultas</span>
      <strong><%= view.consultas().size() %></strong>
      <small>Atenciones clínicas registradas</small>
    </article>

    <article>
      <span>Recetas</span>
      <strong><%= patientRecipes == null ? 0 : patientRecipes.size() %></strong>
      <small>Prescripciones emitidas</small>
    </article>
    <article>
      <span>Radiografías</span>
      <strong><%= activeRadiographs %></strong>
      <small>Registros activos</small>
    </article>
  </div>

  <div class="clinical-summary-grid">
    <section class="patient-section clinical-tab-card clinical-alert-summary">
      <div class="section-title">
        <div>
          <span>SEGURIDAD CLÍNICA</span>
          <h3>Alertas importantes</h3>
        </div>
        <a class="btn tiny secondary"
           href="<%= Html.escape(base) %>/historias?paciente=<%= p.id() %>&tab=antecedentes">
          Ver antecedentes
        </a>
      </div>

      <% if (view.alergias().isEmpty()) { %>
        <div class="clinical-clear-state">
          <strong>Sin alergias registradas</strong>
          <span>No existen alertas de alergia documentadas en la historia.</span>
        </div>
      <% } else { %>
        <div class="clinical-alert-list">
          <% int allergyLimit = Math.min(3, view.alergias().size());
             for (int i = 0; i < allergyLimit; i++) {
               AlergiaClinicaDetalle allergy = view.alergias().get(i); %>
            <div>
              <strong><%= Html.escape(allergy.alergeno()) %></strong>
              <span><%= Html.escape(allergy.reaccion().isBlank() ? "Reacción no especificada" : allergy.reaccion()) %></span>
            </div>
          <% } %>

          <% if (view.alergias().size() > 3) { %>
            <small>+ <%= view.alergias().size() - 3 %> alergia(s) adicional(es) en Antecedentes y alergias.</small>
          <% } %>
        </div>
      <% } %>
    </section>

    <section class="patient-section clinical-tab-card clinical-general-note-card">
      <div class="section-title">
        <div>
          <span>NOTA GENERAL</span>
          <h3>Nota clínica transversal</h3>
        </div>
        <button class="btn tiny secondary"
                type="button"
                data-open-clinical-dialog="general-note">
          <%= generalNote.isBlank() ? "+ Agregar nota" : "Editar nota" %>
        </button>
      </div>

      <% if (generalNote.isBlank()) { %>
        <div class="clinical-clear-state">
          <strong>Sin nota clínica general</strong>
          <span>Úsala solo para información transversal que deba permanecer visible entre atenciones.</span>
        </div>
      <% } else { %>
        <p class="clinical-general-note"><%= Html.escape(generalNote) %></p>
        <small class="clinical-note-help">
          Esta nota no reemplaza antecedentes, alergias, diagnósticos ni la evolución registrada en cada consulta.
        </small>
      <% } %>
    </section>
  </div>
</section>

<dialog class="crud-card crud-dialog clinical-dialog clinical-general-note-dialog"
        data-clinical-dialog="general-note">
  <div class="crud-head clinical-modal-head">
    <div>
      <span class="clinical-dialog-eyebrow">NOTA CLÍNICA GENERAL</span>
      <h2>Editar nota transversal</h2>
      <p>Registra solo información que deba mantenerse visible entre diferentes atenciones del paciente.</p>
    </div>
    <button class="btn secondary" type="button" data-close-clinical-dialog>Cerrar</button>
  </div>

  <form action="<%= Html.escape(base) %>/historias"
        method="post"
        class="crud-form">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="notas">
    <input type="hidden" name="id_paciente" value="<%= p.id() %>">

    <label class="span-2">Nota clínica general
      <textarea name="observaciones_generales"
                maxlength="5000"
                rows="6"
                placeholder="Ej. Paciente con ansiedad marcada durante procedimientos."><%= Html.escape(generalNote) %></textarea>
      <small>No dupliques aquí alergias, antecedentes o información propia de una consulta.</small>
    </label>

    <div class="form-actions">
      <button class="btn secondary" type="button" data-close-clinical-dialog>Cancelar</button>
      <button class="btn primary" type="submit">Guardar nota</button>
    </div>
  </form>
</dialog>
<% } %>

<% if ("antecedentes".equals(historyTab)) { %>
<div class="clinical-two-column">
  <section class="patient-section clinical-tab-card">
    <div class="section-title">
      <div>
        <span>SEGURIDAD</span>
        <h3>Alergias</h3>
      </div>
      <button class="btn tiny primary" type="button" data-open-clinical-dialog="allergy">
        + Agregar
      </button>
    </div>

    <div class="stack-list clinical-stack clinical-stack-scroll">
      <% for (AlergiaClinicaDetalle x : view.alergias()) { %>
        <div class="stack-item allergy-item">
          <strong><%= Html.escape(x.alergeno()) %></strong>
          <span><%= Html.escape(x.reaccion().isBlank() ? "Reacción no especificada" : x.reaccion()) %></span>
          <small><%= Html.escape(x.observaciones()) %> · <%= Html.escape(x.fechaRegistro()) %></small>
        </div>
      <% } %>

      <% if (view.alergias().isEmpty()) { %>
        <p class="empty-note">No hay alergias registradas.</p>
      <% } %>
    </div>
  </section>

  <section class="patient-section clinical-tab-card">
    <div class="section-title">
      <div>
        <span>ANTECEDENTES</span>
        <h3>Antecedentes médicos</h3>
      </div>
      <button class="btn tiny primary" type="button" data-open-clinical-dialog="history">
        + Agregar
      </button>
    </div>

    <div class="stack-list clinical-stack clinical-stack-scroll">
      <% for (AntecedenteClinicoDetalle x : view.antecedentes()) { %>
        <div class="stack-item">
          <strong><%= Html.escape(x.tipo()) %></strong>
          <span><%= Html.escape(x.descripcion()) %></span>
          <small><%= Html.escape(x.observaciones()) %> · <%= Html.escape(x.fechaRegistro()) %></small>
        </div>
      <% } %>

      <% if (view.antecedentes().isEmpty()) { %>
        <p class="empty-note">No hay antecedentes registrados.</p>
      <% } %>
    </div>
  </section>
</div>

<dialog class="crud-card crud-dialog clinical-dialog"
        data-clinical-dialog="allergy">
  <div class="crud-head">
    <div>
      <span class="clinical-dialog-eyebrow">SEGURIDAD CLÍNICA</span>
      <h2>Agregar alergia</h2>
      <p>El registro se incorpora a la historia longitudinal del paciente.</p>
    </div>
    <button class="btn secondary" type="button" data-close-clinical-dialog>Cerrar</button>
  </div>

  <form action="<%= Html.escape(base) %>/historias"
        method="post"
        class="crud-form">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="alergia">
    <input type="hidden" name="id_paciente" value="<%= p.id() %>">

    <label class="span-2">Alérgeno
      <input name="alergeno" maxlength="150" required placeholder="Ej. Penicilina">
    </label>

    <label class="span-2">Reacción
      <input name="reaccion" maxlength="255" placeholder="Ej. Urticaria">
    </label>

    <label class="span-2">Observaciones
      <textarea name="observaciones" maxlength="2000" rows="3" placeholder="Información adicional"></textarea>
    </label>

    <div class="form-actions">
      <button class="btn secondary" type="button" data-close-clinical-dialog>Cancelar</button>
      <button class="btn primary" type="submit">Agregar alergia</button>
    </div>
  </form>
</dialog>

<dialog class="crud-card crud-dialog clinical-dialog"
        data-clinical-dialog="history">
  <div class="crud-head">
    <div>
      <span class="clinical-dialog-eyebrow">ANTECEDENTES</span>
      <h2>Agregar antecedente</h2>
      <p>El registro quedará asociado permanentemente a la historia del paciente.</p>
    </div>
    <button class="btn secondary" type="button" data-close-clinical-dialog>Cerrar</button>
  </div>

  <form action="<%= Html.escape(base) %>/historias"
        method="post"
        class="crud-form">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="antecedente">
    <input type="hidden" name="id_paciente" value="<%= p.id() %>">

    <label class="span-2">Tipo
      <input name="tipo"
             maxlength="100"
             required
             placeholder="Médico, familiar, quirúrgico...">
    </label>

    <label class="span-2">Descripción
      <input name="descripcion"
             maxlength="255"
             required
             placeholder="Descripción del antecedente">
    </label>

    <label class="span-2">Observaciones
      <textarea name="observaciones"
                maxlength="2000"
                rows="3"
                placeholder="Información adicional"></textarea>
    </label>

    <div class="form-actions">
      <button class="btn secondary" type="button" data-close-clinical-dialog>Cancelar</button>
      <button class="btn primary" type="submit">Agregar antecedente</button>
    </div>
  </form>
</dialog>
<% } %>

<% if ("consultas".equals(historyTab)) { %>
<section class="patient-section clinical-tab-card">
  <div class="section-title">
    <div>
      <span>EVOLUCIÓN</span>
      <h3>Consultas clínicas</h3>
    </div>
    <span><%= view.consultas().size() %> atenciones</span>
  </div>

  <div class="consultation-timeline clinical-timeline-scroll">
    <% for (ConsultaHistorialDetalle x : view.consultas()) { %>
      <article class="consultation-entry">
        <div class="consultation-meta">
          <strong><%= Html.escape(x.fechaConsulta()) %></strong>
          <span>
            <%= Html.escape(x.odontologo()) %>
            ·
            <%= Html.escape(x.especialidad().isBlank() ? "Sin especialidad indicada" : x.especialidad()) %>
          </span>
        </div>

        <div class="consultation-body">
          <div>
            <span>Motivo</span>
            <p><%= Html.escape(x.motivo().isBlank() ? "—" : x.motivo()) %></p>
          </div>
          <div>
            <span>Diagnóstico</span>
            <p><%= Html.escape(x.diagnostico().isBlank() ? "—" : x.diagnostico()) %></p>
          </div>
          <div>
            <span>Tratamiento</span>
            <p><%= Html.escape(x.tratamiento().isBlank() ? "—" : x.tratamiento()) %></p>
          </div>
          <div>
            <span>Recomendaciones</span>
            <p><%= Html.escape(x.recomendaciones().isBlank() ? "—" : x.recomendaciones()) %></p>
          </div>
        </div>

        <a class="btn tiny secondary"
           href="<%= Html.escape(base) %>/atencion?id=<%= x.idCita() %>">
          Ver atención
        </a>
      </article>
    <% } %>

    <% if (view.consultas().isEmpty()) { %>
      <p class="empty-note">
        Todavía no existen atenciones clínicas registradas para este paciente.
      </p>
    <% } %>
  </div>
</section>
<% } %>

<% if ("recetas".equals(historyTab)) { %>
<section class="patient-section clinical-tab-card">
  <div class="section-title">
    <div>
      <span>TRATAMIENTO</span>
      <h3>Recetas médicas</h3>
    </div>
    <button class="btn tiny primary" type="button" data-open-clinical-dialog="recipe">
      + Emitir receta
    </button>
  </div>

  <div class="consultation-timeline clinical-timeline-scroll">
    <% if (patientRecipes != null) for (RecetaResumen r : patientRecipes) { %>
      <article class="consultation-entry">
        <div class="consultation-meta">
          <strong><%= Html.escape(r.fecha()) %></strong>
          <span>Receta #<%= r.idReceta() %></span>
        </div>

        <div class="consultation-body">
          <div>
            <span>Medicamentos</span>
            <p><%= Html.escape(r.medicamentos()) %></p>
          </div>
          <div>
            <span>Indicaciones</span>
            <p><%= Html.escape(r.indicaciones()) %></p>
          </div>
        </div>

        <%
          String recipePdfUrl = base + "/modulo/recetas/pdf?id=" + r.idReceta();
          String recipeWhatsappMessage =
              "Hola " + p.nombreCompleto()
              + ". Le compartimos su receta médica N.° " + r.idReceta()
              + " emitida el " + r.fecha()
              + ". El PDF de la receta ha sido generado desde OdontoSync.";
          String recipeWhatsappUrl = patientWhatsappNumber.isBlank()
              ? ""
              : "https://wa.me/" + patientWhatsappNumber + "?text="
                  + URLEncoder.encode(recipeWhatsappMessage, StandardCharsets.UTF_8).replace("+", "%20");
        %>
        <button class="btn tiny secondary"
                type="button"
                data-open-recipe-view
                data-recipe-id="<%= r.idReceta() %>"
                data-recipe-date="<%= Html.escape(r.fecha()) %>"
                data-recipe-medications="<%= Html.escape(r.medicamentos()) %>"
                data-recipe-indications="<%= Html.escape(r.indicaciones()) %>"
                data-recipe-pdf="<%= Html.escape(recipePdfUrl) %>"
                data-recipe-whatsapp="<%= Html.escape(recipeWhatsappUrl) %>">
          Ver receta
        </button>
      </article>
    <% } %>

    <% if (patientRecipes == null || patientRecipes.isEmpty()) { %>
      <p class="empty-note">
        Todavía no existen recetas médicas registradas para este paciente.
      </p>
    <% } %>
  </div>
</section>


<dialog class="crud-card crud-dialog clinical-dialog clinical-recipe-view-dialog"
        data-recipe-view-dialog>
  <div class="crud-head clinical-modal-head">
    <div>
      <span class="clinical-dialog-eyebrow">RECETA EMITIDA</span>
      <h2>Receta #<span data-recipe-view-id></span></h2>
      <p>
        Emitida para <strong><%= Html.escape(p.nombreCompleto()) %></strong>
        · <span data-recipe-view-date></span>
      </p>
    </div>
    <button class="btn secondary" type="button" data-close-recipe-view>Cerrar</button>
  </div>

  <div class="clinical-recipe-detail-grid">
    <section>
      <span>Medicamentos</span>
      <p data-recipe-view-medications></p>
    </section>
    <section>
      <span>Indicaciones</span>
      <p data-recipe-view-indications></p>
    </section>
  </div>

  <div class="clinical-modal-footer clinical-recipe-actions">
    <span>La receta se consulta sin abandonar la historia clínica del paciente.</span>
    <div>
      <a class="btn secondary" href="#" data-recipe-view-pdf>Descargar PDF</a>
      <a class="btn primary" href="#" data-recipe-view-whatsapp>WhatsApp</a>
    </div>
  </div>
</dialog>

<dialog class="crud-card crud-dialog clinical-dialog clinical-recipe-dialog"
        data-clinical-dialog="recipe">
  <div class="crud-head">
    <div>
      <span class="clinical-dialog-eyebrow">RECETA MÉDICA</span>
      <h2>Emitir receta</h2>
      <p>
        La receta quedará asociada a
        <strong><%= Html.escape(p.nombreCompleto()) %></strong>,
        a tu cuenta de odontólogo y a esta clínica.
      </p>
    </div>
    <button class="btn secondary" type="button" data-close-clinical-dialog>Cerrar</button>
  </div>

  <form action="<%= Html.escape(base) %>/historias"
        method="post"
        class="crud-form">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="receta">
    <input type="hidden" name="id_paciente" value="<%= p.id() %>">

    <label class="span-2">Medicamentos
      <textarea name="medicamentos"
                maxlength="5000"
                rows="5"
                required
                placeholder="Ejemplo: Ibuprofeno 400 mg, 1 tableta cada 8 horas por 3 días."></textarea>
    </label>

    <label class="span-2">Indicaciones
      <textarea name="indicaciones"
                maxlength="5000"
                rows="5"
                required
                placeholder="Cuidados, duración, restricciones y otras indicaciones para el paciente."></textarea>
    </label>

    <div class="clinical-integrity-tip span-2">
      <strong>TRAZABILIDAD</strong>
      <span>
        La receta conservará paciente, clínica y odontólogo emisor. El PDF y WhatsApp seguirán disponibles desde el detalle individual.
      </span>
    </div>

    <div class="form-actions">
      <button class="btn secondary" type="button" data-close-clinical-dialog>Cancelar</button>
      <button class="btn primary" type="submit">Emitir receta</button>
    </div>
  </form>
</dialog>
<% } %>

<% if ("radiografias".equals(historyTab)) { %>
<section class="patient-section clinical-tab-card">
  <div class="section-title">
    <div><span>IMÁGENES CLÍNICAS</span><h3>Radiografías registradas</h3></div>
    <button class="btn primary" type="button" data-open-clinical-dialog="radiograph">+ Registrar radiografía</button>
  </div>
  <div class="rx-list">
    <% for (PlacaDetalle rx : patientRadiographs) {
         boolean activeRx = "ACTIVA".equals(rx.estado());
         String rxUrl = base + "/modulo/placas/archivo?id=" + rx.idPlaca();
    %>
      <article class="rx-card">
        <div class="rx-card-heading">
          <div class="rx-file-symbol" aria-hidden="true"><%= "application/pdf".equals(rx.tipoMime()) ? "PDF" : "RX" %></div>
          <div class="rx-card-title">
            <strong><%= Html.escape(rx.tipoRadiografia()) %></strong>
            <small><%= rx.fechaRadiografia() %><%= rx.regionDental() == null || rx.regionDental().isBlank() ? "" : " · " + Html.escape(rx.regionDental()) %></small>
          </div>
          <span class="status-pill <%= activeRx ? "" : "status-inactive" %>"><%= Html.escape(rx.estado()) %></span>
        </div>
        <p class="rx-filename"><%= Html.escape(rx.nombreArchivo()) %></p>
        <% if (activeRx) { %>
        <div class="rx-actions">
          <button class="btn tiny primary" type="button" data-rx-view
            data-rx-title="<%= Html.escape(rx.tipoRadiografia()) %>"
            data-rx-date="<%= rx.fechaRadiografia() %>"
            data-rx-region="<%= Html.escape(rx.regionDental()) %>"
            data-rx-notes="<%= Html.escape(rx.observaciones()) %>"
            data-rx-mime="<%= Html.escape(rx.tipoMime()) %>"
            data-rx-url="<%= Html.escape(rxUrl) %>">Ver radiografía</button>
          <a class="btn tiny secondary" href="<%= Html.escape(rxUrl) %>">Descargar</a>
          <% if (rx.idUsuario() == user.id()) { %>
          <button class="btn tiny danger-btn" type="button" data-rx-annul="<%= rx.idPlaca() %>">Anular</button>
          <% } %>
        </div>
        <% } else { %>
        <small class="rx-annulled">Registro conservado por trazabilidad. Archivo no disponible para consulta.</small>
        <% } %>
      </article>
    <% } %>
    <% if (patientRadiographs.isEmpty()) { %>
      <p class="empty-note">Todavía no existen radiografías registradas para este paciente.</p>
    <% } %>
  </div>
</section>

<dialog class="crud-card crud-dialog clinical-dialog rx-dialog" data-clinical-dialog="radiograph" aria-labelledby="rx-create-title">
  <div class="crud-head">
    <div><span class="clinical-dialog-eyebrow">RADIOGRAFÍAS</span><h2 id="rx-create-title">Registrar radiografía</h2>
      <p>Paciente: <strong><%= Html.escape(p.nombreCompleto()) %></strong></p></div>
    <button class="btn secondary" type="button" data-close-clinical-dialog>Cerrar</button>
  </div>
  <form class="crud-form" method="post" enctype="multipart/form-data" data-rx-upload-form action="<%= Html.escape(base) %>/modulo/placas?paciente=<%= p.id() %>">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="crear">
    <input type="hidden" name="id_paciente" value="<%= p.id() %>">
    <label>Tipo de radiografía
      <select name="tipo_radiografia" required>
        <option value="">Seleccionar tipo</option><option value="PANORAMICA">Panorámica</option>
        <option value="PERIAPICAL">Periapical</option><option value="BITEWING">Bitewing</option>
        <option value="OCLUSAL">Oclusal</option><option value="CEFALOMETRICA">Cefalométrica</option><option value="OTRA">Otra</option>
      </select>
    </label>
    <label>Fecha de radiografía
      <input type="date" name="fecha_radiografia" required max="<%= java.time.LocalDate.now() %>" value="<%= java.time.LocalDate.now() %>">
    </label>
    <label class="span-2">Pieza dental o región (opcional)
      <input name="region_dental" maxlength="100" placeholder="Ej. Pieza 36">
    </label>
    <label class="span-2">Archivo radiográfico
      <input type="file" name="archivo" required accept=".jpg,.jpeg,.png,.pdf,image/jpeg,image/png,application/pdf" aria-describedby="rx-upload-help rx-upload-error">
      <small id="rx-upload-help">JPG, JPEG, PNG o PDF. Máximo 10 MB.</small>
      <span id="rx-upload-error" class="rx-upload-error" role="alert" hidden></span>
    </label>
    <label class="span-2">Observaciones clínicas (opcional)
      <textarea name="observaciones" maxlength="5000" rows="4"></textarea>
    </label>
    <div class="form-actions">
      <button class="btn secondary" type="button" data-close-clinical-dialog>Cancelar</button>
      <button class="btn primary" type="submit">Registrar radiografía</button>
    </div>
  </form>
</dialog>

<dialog class="crud-card crud-dialog clinical-dialog rx-view-dialog" data-rx-view-dialog aria-labelledby="rx-view-title">
  <div class="crud-head">
    <div><span class="clinical-dialog-eyebrow">RADIOGRAFÍA REGISTRADA</span><h2 id="rx-view-title" data-rx-field="title">Radiografía</h2>
      <p><%= Html.escape(p.nombreCompleto()) %> · <span data-rx-field="date"></span></p></div>
    <button class="btn secondary" type="button" data-rx-close>Cerrar</button>
  </div>
  <div class="rx-image-controls" data-rx-controls hidden>
    <button class="btn tiny secondary" type="button" data-rx-command="out" aria-label="Alejar imagen">−</button>
    <span data-rx-zoom>100 %</span>
    <button class="btn tiny secondary" type="button" data-rx-command="in" aria-label="Acercar imagen">+</button>
    <button class="btn tiny secondary" type="button" data-rx-command="rotate">Girar 90°</button>
    <button class="btn tiny secondary" type="button" data-rx-command="reset">Restablecer</button>
    <button class="btn tiny secondary" type="button" data-rx-command="expand">Ampliar</button>
  </div>
  <div class="rx-viewport" data-rx-viewport hidden>
    <div class="rx-stage" data-rx-stage><img data-rx-image alt="Radiografía del paciente"></div>
  </div>
  <p class="rx-message" data-rx-message role="status"></p>
  <div class="rx-detail"><strong>Región dental</strong><p data-rx-field="region"></p></div>
  <div class="rx-detail"><strong>Observaciones clínicas</strong><p data-rx-field="notes"></p></div>
  <div class="form-actions"><a class="btn primary" data-rx-download>Descargar original</a></div>
</dialog>

<dialog class="crud-card crud-dialog clinical-dialog rx-dialog" data-rx-annul-dialog aria-labelledby="rx-annul-title">
  <div class="crud-head"><div><h2 id="rx-annul-title">Anular radiografía</h2><p>Se conservará el registro y su archivo original.</p></div>
    <button class="btn secondary" type="button" data-rx-close>Cerrar</button></div>
  <form class="crud-form" method="post" action="<%= Html.escape(base) %>/modulo/placas">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="anular">
    <input type="hidden" name="id_paciente" value="<%= p.id() %>">
    <input type="hidden" name="id_placa" data-rx-annul-id>
    <label class="span-2">Motivo de anulación<textarea name="motivo" required maxlength="500" rows="4"></textarea></label>
    <div class="form-actions"><button class="btn secondary" type="button" data-rx-close>Cancelar</button><button class="btn danger-btn" type="submit">Confirmar anulación</button></div>
  </form>
</dialog>
<script src="<%= Html.escape(base) %>/js/radiografias-ui.js?v=31-2" defer></script>
<% } %>

<p class="read-only clinical-scope-note">
  Los antecedentes, alergias, consultas y recetas forman parte de la historia longitudinal del paciente y mantienen su trazabilidad.
</p>

<script src="<%= Html.escape(base) %>/js/historias-ui.js?v=20261120-issue29-ux2" defer></script>
<% } %>

<%@ include file="layout-bottom.jspf" %>
