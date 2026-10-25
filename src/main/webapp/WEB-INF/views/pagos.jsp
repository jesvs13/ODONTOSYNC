<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,pe.odontosync.model.PagoPendiente,pe.odontosync.model.PagoResumen" %>
<%@ include file="layout-top.jspf" %>
<%
List<PagoPendiente> pending = (List<PagoPendiente>) request.getAttribute("pendingPayments");
List<PagoResumen> history = (List<PagoResumen>) request.getAttribute("paymentHistory");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
boolean canRegister = Boolean.TRUE.equals(request.getAttribute("canRegisterPayments"));
%>
<div class="page-heading">
  <div>
    <h1>Pagos</h1>
    <p>Registra cobros únicamente de citas que ya fueron atendidas.</p>
  </div>
  <span class="code-chip">COBROS · CITAS ATENDIDAS</span>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<% if (canRegister) { %>
<section class="crud-card">
  <div class="crud-head">
    <div>
      <h2>Registrar pago</h2>
      <p>Solo aparecen citas ATENDIDAS que todavía no tienen un cobro registrado.</p>
    </div>
  </div>

  <% if (pending != null && !pending.isEmpty()) { %>
  <form class="crud-form" action="<%= Html.escape(base) %>/modulo/pagos" method="post">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="registrar">

    <label>Cita atendida
      <select name="id_cita" required>
        <option value="">Selecciona una cita</option>
        <% for (PagoPendiente p : pending) { %>
          <option value="<%= p.idCita() %>"><%= Html.escape(p.fecha() + " " + p.hora() + " · " + p.paciente() + " · " + p.odontologo()) %></option>
        <% } %>
      </select>
      <small>El pago queda relacionado con la cita seleccionada.</small>
    </label>

    <label>Monto (S/)
      <input type="number" name="monto" min="0.01" max="99999999.99" step="0.01" inputmode="decimal" required placeholder="0.00">
    </label>

    <label>Método de pago
      <select name="metodo_pago" required>
        <option value="">Selecciona un método</option>
        <option value="EFECTIVO">Efectivo</option>
        <option value="YAPE">Yape</option>
        <option value="PLIN">Plin</option>
        <option value="TRANSFERENCIA">Transferencia</option>
        <option value="TARJETA">Tarjeta</option>
      </select>
    </label>

    <label>Observaciones
      <textarea name="observaciones" maxlength="1000" rows="3" placeholder="Dato adicional del cobro (opcional)"></textarea>
    </label>

    <div class="form-actions">
      <button class="btn primary" type="submit">Registrar pago</button>
    </div>
  </form>
  <% } else { %>
    <p class="read-only">No existen citas ATENDIDAS pendientes de pago en este momento.</p>
  <% } %>
</section>
<% } %>

<section class="table-card crud-list">
  <div class="table-top">
    <div>
      <h2>Historial de pagos</h2>
      <span>Últimos cobros registrados para la clínica.</span>
    </div>
  </div>
  <div class="table-scroll">
    <table>
      <thead>
        <tr><th>Fecha</th><th>Paciente</th><th>Monto</th><th>Método</th></tr>
      </thead>
      <tbody>
      <% if (history != null) for (PagoResumen p : history) { %>
        <tr>
          <td><%= Html.escape(p.fecha()) %></td>
          <td><strong><%= Html.escape(p.paciente()) %></strong></td>
          <td>S/ <%= Html.escape(p.monto()) %></td>
          <td><span class="status-pill"><%= Html.escape(p.metodo()) %></span></td>
        </tr>
      <% } %>
      <% if (history == null || history.isEmpty()) { %>
        <tr><td colspan="4" class="empty">Todavía no existen pagos registrados.</td></tr>
      <% } %>
      </tbody>
    </table>
  </div>
</section>

<p class="read-only">Para este avance se registra el cobro y su método de pago. La carga de comprobantes queda fuera de este alcance parcial.</p>
<%@ include file="layout-bottom.jspf" %>
