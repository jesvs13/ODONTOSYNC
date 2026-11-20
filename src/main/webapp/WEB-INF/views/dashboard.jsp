<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.math.BigDecimal,java.time.LocalDate,java.util.List,java.util.Locale" %>
<%@ page import="pe.odontosync.model.DashboardAnalyticsSnapshot,pe.odontosync.model.DashboardCategory,pe.odontosync.model.DashboardPoint,pe.odontosync.model.OdontologoDetalle" %>
<%@ include file="layout-top.jspf" %>
<%
DashboardAnalyticsSnapshot dashboard =
    (DashboardAnalyticsSnapshot) request.getAttribute("dashboard");
@SuppressWarnings("unchecked")
List<OdontologoDetalle> dentists =
    (List<OdontologoDetalle>) request.getAttribute("dentists");
String dashboardError = (String) request.getAttribute("dashboardError");
String period = (String) request.getAttribute("period");
String section = (String) request.getAttribute("section");
LocalDate from = (LocalDate) request.getAttribute("from");
LocalDate to = (LocalDate) request.getAttribute("to");
Long dentistUserId = (Long) request.getAttribute("dentistUserId");

if (period == null) period = "30d";
if (section == null) section = "resumen";
if (dentists == null) dentists = List.of();

boolean sectionCitas = "citas".equals(section);
boolean sectionFinanzas = "finanzas".equals(section);
boolean showDentistFilter = sectionCitas || sectionFinanzas;

int inventoryTotal = 0;
int inventoryLow = 0;
int inventoryEmpty = 0;
if (dashboard != null) {
    for (DashboardCategory item : dashboard.inventoryStatus()) {
        int value = item.value().intValue();
        inventoryTotal += value;
        String label = item.label() == null ? "" : item.label().toLowerCase(Locale.ROOT);
        if (label.contains("bajo")) inventoryLow += value;
        if (label.contains("sin stock")) inventoryEmpty += value;
    }
}
%>
<link rel="stylesheet" href="<%= Html.escape(base) %>/css/dashboard-analytics.css?v=20261120-3">

<section class="analytics-heading">
  <div>
    <span class="analytics-eyebrow">ANÁLISIS DE LA CLÍNICA</span>
    <h1>Dashboard</h1>
    <p>
      Indicadores y tendencias de
      <strong><%= Html.escape(user.clinicName()) %></strong>.
      Los datos pertenecen exclusivamente a esta clínica.
    </p>
  </div>
  <span class="analytics-clinic-chip"><%= Html.escape(user.clinicName()) %></span>
</section>

<nav class="analytics-sections" aria-label="Secciones del dashboard">
  <a class="<%= "resumen".equals(section) ? "active" : "" %>"
     href="<%= Html.escape(base) %>/modulo/dashboard?section=resumen">Resumen</a>
  <a class="<%= "citas".equals(section) ? "active" : "" %>"
     href="<%= Html.escape(base) %>/modulo/dashboard?section=citas">Citas y atención</a>
  <a class="<%= "pacientes".equals(section) ? "active" : "" %>"
     href="<%= Html.escape(base) %>/modulo/dashboard?section=pacientes">Pacientes</a>
  <a class="<%= "finanzas".equals(section) ? "active" : "" %>"
     href="<%= Html.escape(base) %>/modulo/dashboard?section=finanzas">Finanzas</a>
  <a class="<%= "inventario".equals(section) ? "active" : "" %>"
     href="<%= Html.escape(base) %>/modulo/dashboard?section=inventario">Inventario</a>
</nav>

<% if (!"inventario".equals(section)) { %>
<form class="analytics-filters <%= showDentistFilter ? "" : "analytics-filters-simple" %>"
      method="get"
      action="<%= Html.escape(base) %>/modulo/dashboard">
  <input type="hidden" name="section" value="<%= Html.escape(section) %>">

  <label>
    <span>Período</span>
    <select name="period" id="dashboardPeriod">
      <option value="7d" <%= "7d".equals(period) ? "selected" : "" %>>Últimos 7 días</option>
      <option value="30d" <%= "30d".equals(period) ? "selected" : "" %>>Últimos 30 días</option>
      <option value="month" <%= "month".equals(period) ? "selected" : "" %>>Este mes</option>
      <option value="year" <%= "year".equals(period) ? "selected" : "" %>>Este año</option>
      <option value="custom" <%= "custom".equals(period) ? "selected" : "" %>>Personalizado</option>
    </select>
  </label>

  <label class="analytics-date-field">
    <span>Desde</span>
    <input type="date" name="from"
      value="<%= from == null ? "" : Html.escape(from.toString()) %>">
  </label>

  <label class="analytics-date-field">
    <span>Hasta</span>
    <input type="date" name="to"
      value="<%= to == null ? "" : Html.escape(to.toString()) %>">
  </label>

  <% if (showDentistFilter) { %>
  <label>
    <span>Odontólogo</span>
    <select name="odontologo">
      <option value="">Todos los odontólogos</option>
      <% for (OdontologoDetalle dentist : dentists) { %>
        <option
          value="<%= dentist.idUsuario() %>"
          <%= dentistUserId != null && dentistUserId == dentist.idUsuario() ? "selected" : "" %>>
          <%= Html.escape(dentist.nombreCompleto()) %>
        </option>
      <% } %>
    </select>
  </label>
  <% } %>

  <button class="analytics-filter-button" type="submit">Aplicar filtros</button>
</form>
<% } else { %>
<section class="analytics-context-note">
  <div>
    <strong>Inventario actual de la clínica</strong>
    <span>Esta sección muestra el estado vigente del stock y no depende de un odontólogo ni de un período clínico.</span>
  </div>
</section>
<% } %>

<% if (dashboardError != null) { %>
  <div class="analytics-alert" role="alert"><%= Html.escape(dashboardError) %></div>
<% } %>

<% if (dashboard != null) { %>

<% if ("resumen".equals(section)) { %>
<section class="analytics-section-intro">
  <div>
    <span>RESUMEN GENERAL</span>
    <h2>Panorama ejecutivo de la clínica</h2>
    <p>Una lectura rápida de pacientes, actividad, ingresos y asistencia del período seleccionado.</p>
  </div>
</section>

<section class="analytics-kpis" aria-label="Indicadores principales">
  <article class="analytics-kpi">
    <span>Pacientes registrados</span>
    <strong><%= dashboard.summary().patients() %></strong>
    <small>Solo de <%= Html.escape(user.clinicName()) %></small>
  </article>
  <article class="analytics-kpi">
    <span>Citas del período</span>
    <strong><%= dashboard.summary().appointments() %></strong>
    <small>Actividad clínica registrada</small>
  </article>
  <article class="analytics-kpi">
    <span>Ingresos</span>
    <strong>S/ <%= String.format(Locale.US, "%,.2f", dashboard.summary().income()) %></strong>
    <small>Pagos registrados en el período</small>
  </article>
  <article class="analytics-kpi">
    <span>Tasa de asistencia</span>
    <strong><%= String.format(Locale.US, "%.1f", dashboard.summary().attendanceRate()) %>%</strong>
    <small>Atendidas sobre citas concluidas</small>
  </article>
</section>

<section class="analytics-domain-links" aria-label="Áreas analíticas">
  <a href="<%= Html.escape(base) %>/modulo/dashboard?section=citas">
    <span>Actividad clínica</span>
    <strong>Citas y atención</strong>
    <small>Agenda, estados y rendimiento profesional</small>
  </a>
  <a href="<%= Html.escape(base) %>/modulo/dashboard?section=pacientes">
    <span>Crecimiento</span>
    <strong>Pacientes</strong>
    <small>Evolución de nuevos registros</small>
  </a>
  <a href="<%= Html.escape(base) %>/modulo/dashboard?section=finanzas">
    <span>Gestión económica</span>
    <strong>Finanzas</strong>
    <small>Ingresos y métodos de pago</small>
  </a>
  <a href="<%= Html.escape(base) %>/modulo/dashboard?section=inventario">
    <span>Abastecimiento</span>
    <strong>Inventario</strong>
    <small>Stock normal, bajo y agotado</small>
  </a>
</section>
<% } %>

<% if ("citas".equals(section)) { %>
<section class="analytics-section-intro">
  <div>
    <span>CITAS Y ATENCIÓN</span>
    <h2>Actividad asistencial</h2>
    <p>Analiza el comportamiento de las citas y, cuando corresponda, filtra por odontólogo.</p>
  </div>
</section>

<section class="analytics-kpis analytics-kpis-2">
  <article class="analytics-kpi">
    <span>Citas del período</span>
    <strong><%= dashboard.summary().appointments() %></strong>
    <small>Según los filtros aplicados</small>
  </article>
  <article class="analytics-kpi">
    <span>Tasa de asistencia</span>
    <strong><%= String.format(Locale.US, "%.1f", dashboard.summary().attendanceRate()) %>%</strong>
    <small>Atendidas sobre citas concluidas</small>
  </article>
</section>

<section class="analytics-grid">
  <article class="analytics-chart-card analytics-wide">
    <header>
      <div><span>TENDENCIA</span><h2>Citas por período</h2></div>
      <small>Gráfico de barras</small>
    </header>
    <div class="analytics-chart-wrap">
      <canvas class="analytics-chart" data-chart-type="bar" aria-label="Citas por período"></canvas>
    </div>
    <ul class="analytics-chart-data">
      <% for (DashboardPoint item : dashboard.appointmentsTrend()) { %>
        <li data-label="<%= Html.escape(item.label()) %>" data-value="<%= item.value() %>">
          <span><%= Html.escape(item.label()) %></span><strong><%= item.value() %></strong>
        </li>
      <% } %>
    </ul>
  </article>

  <article class="analytics-chart-card">
    <header>
      <div><span>DISTRIBUCIÓN</span><h2>Estado de citas</h2></div>
      <small>Gráfico de dona</small>
    </header>
    <div class="analytics-chart-wrap">
      <canvas class="analytics-chart" data-chart-type="donut" aria-label="Estado de citas"></canvas>
    </div>
    <ul class="analytics-chart-data analytics-legend">
      <% for (DashboardCategory item : dashboard.appointmentStatus()) { %>
        <li data-label="<%= Html.escape(item.label()) %>" data-value="<%= item.value() %>">
          <span><%= Html.escape(item.label()) %></span><strong><%= item.value() %></strong>
        </li>
      <% } %>
    </ul>
  </article>

  <article class="analytics-chart-card">
    <header>
      <div><span>PROFESIONALES</span><h2>Atenciones por odontólogo</h2></div>
      <small>Gráfico de barras</small>
    </header>
    <div class="analytics-chart-wrap">
      <canvas class="analytics-chart" data-chart-type="bar" data-index-axis="y" aria-label="Atenciones por odontólogo"></canvas>
    </div>
    <ul class="analytics-chart-data analytics-legend">
      <% for (DashboardCategory item : dashboard.attentionsByDentist()) { %>
        <li data-label="<%= Html.escape(item.label()) %>" data-value="<%= item.value() %>">
          <span><%= Html.escape(item.label()) %></span><strong><%= item.value() %></strong>
        </li>
      <% } %>
    </ul>
  </article>
</section>
<% } %>

<% if ("pacientes".equals(section)) { %>
<section class="analytics-section-intro">
  <div>
    <span>PACIENTES</span>
    <h2>Crecimiento de la base de pacientes</h2>
    <p>Consulta los registros de pacientes de la clínica y su evolución en el período.</p>
  </div>
</section>

<section class="analytics-kpis analytics-kpis-1">
  <article class="analytics-kpi">
    <span>Pacientes registrados</span>
    <strong><%= dashboard.summary().patients() %></strong>
    <small>Total registrado en tu clínica</small>
  </article>
</section>

<section class="analytics-grid">
  <article class="analytics-chart-card analytics-wide">
    <header>
      <div><span>CRECIMIENTO</span><h2>Pacientes nuevos</h2></div>
      <small>Gráfico de línea</small>
    </header>
    <div class="analytics-chart-wrap">
      <canvas class="analytics-chart" data-chart-type="line" aria-label="Pacientes nuevos"></canvas>
    </div>
    <ul class="analytics-chart-data">
      <% for (DashboardPoint item : dashboard.newPatientsTrend()) { %>
        <li data-label="<%= Html.escape(item.label()) %>" data-value="<%= item.value() %>">
          <span><%= Html.escape(item.label()) %></span><strong><%= item.value() %></strong>
        </li>
      <% } %>
    </ul>
  </article>
</section>
<% } %>

<% if ("finanzas".equals(section)) { %>
<section class="analytics-section-intro">
  <div>
    <span>FINANZAS</span>
    <h2>Ingresos y formas de cobro</h2>
    <p>Analiza la evolución de ingresos y la composición de los métodos de pago registrados.</p>
  </div>
</section>

<section class="analytics-kpis analytics-kpis-1">
  <article class="analytics-kpi">
    <span>Ingresos del período</span>
    <strong>S/ <%= String.format(Locale.US, "%,.2f", dashboard.summary().income()) %></strong>
    <small>Pagos según los filtros aplicados</small>
  </article>
</section>

<section class="analytics-grid">
  <article class="analytics-chart-card">
    <header>
      <div><span>INGRESOS</span><h2>Evolución de ingresos</h2></div>
      <small>Gráfico de barras</small>
    </header>
    <div class="analytics-chart-wrap">
      <canvas class="analytics-chart" data-chart-type="bar" data-value-prefix="S/ " aria-label="Ingresos por período"></canvas>
    </div>
    <ul class="analytics-chart-data">
      <% for (DashboardPoint item : dashboard.incomeTrend()) { %>
        <li data-label="<%= Html.escape(item.label()) %>" data-value="<%= item.value() %>">
          <span><%= Html.escape(item.label()) %></span><strong>S/ <%= item.value() %></strong>
        </li>
      <% } %>
    </ul>
  </article>

  <article class="analytics-chart-card">
    <header>
      <div><span>COBROS</span><h2>Métodos de pago</h2></div>
      <small>Gráfico de pastel</small>
    </header>
    <div class="analytics-chart-wrap">
      <canvas class="analytics-chart" data-chart-type="pie" data-value-prefix="S/ " aria-label="Métodos de pago"></canvas>
    </div>
    <ul class="analytics-chart-data analytics-legend">
      <% for (DashboardCategory item : dashboard.paymentMethods()) { %>
        <li data-label="<%= Html.escape(item.label()) %>" data-value="<%= item.value() %>">
          <span><%= Html.escape(item.label()) %></span><strong>S/ <%= item.value() %></strong>
        </li>
      <% } %>
    </ul>
  </article>
</section>
<% } %>

<% if ("inventario".equals(section)) { %>
<section class="analytics-section-intro">
  <div>
    <span>INVENTARIO</span>
    <h2>Estado de abastecimiento</h2>
    <p>Consulta la situación vigente del stock perteneciente exclusivamente a esta clínica.</p>
  </div>
</section>

<section class="analytics-kpis analytics-kpis-3">
  <article class="analytics-kpi">
    <span>Insumos registrados</span>
    <strong><%= inventoryTotal %></strong>
    <small>Clasificados por estado de stock</small>
  </article>
  <article class="analytics-kpi">
    <span>Stock bajo</span>
    <strong><%= inventoryLow %></strong>
    <small>Requieren reposición próxima</small>
  </article>
  <article class="analytics-kpi">
    <span>Sin stock</span>
    <strong><%= inventoryEmpty %></strong>
    <small>Requieren atención prioritaria</small>
  </article>
</section>

<section class="analytics-grid">
  <article class="analytics-chart-card analytics-wide">
    <header>
      <div><span>ABASTECIMIENTO</span><h2>Estado del inventario</h2></div>
      <small>Gráfico de dona</small>
    </header>
    <div class="analytics-chart-wrap">
      <canvas class="analytics-chart" data-chart-type="donut" aria-label="Estado del inventario"></canvas>
    </div>
    <ul class="analytics-chart-data analytics-legend">
      <% for (DashboardCategory item : dashboard.inventoryStatus()) { %>
        <li data-label="<%= Html.escape(item.label()) %>" data-value="<%= item.value() %>">
          <span><%= Html.escape(item.label()) %></span><strong><%= item.value() %></strong>
        </li>
      <% } %>
    </ul>
  </article>
</section>
<% } %>

<% } %>

<script src="<%= Html.escape(base) %>/webjars/chart.js/4.6.1/dist/chart.umd.min.js" defer></script>
<script src="<%= Html.escape(base) %>/js/dashboard-analytics.js?v=20261120-2" defer></script>
<%@ include file="layout-bottom.jspf" %>
