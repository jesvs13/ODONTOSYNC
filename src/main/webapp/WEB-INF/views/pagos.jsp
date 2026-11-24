<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.LinkedHashSet,java.util.Set,pe.odontosync.model.PagoPendiente,pe.odontosync.model.PagoResumen" %>
<%@ include file="layout-top.jspf" %>
<link rel="stylesheet" href="<%= Html.escape(base) %>/css/pagos-ui.css?v=20261120-issue29-ux1">
<%
List<PagoPendiente> pending = (List<PagoPendiente>) request.getAttribute("pendingPayments");
List<PagoResumen> history = (List<PagoResumen>) request.getAttribute("paymentHistory");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
boolean canRegister = Boolean.TRUE.equals(request.getAttribute("canRegisterPayments"));
int pendingCount = pending == null ? 0 : pending.size();
int historyCount = history == null ? 0 : history.size();
Set<String> methods = new LinkedHashSet<>();
if (history != null) {
    for (PagoResumen p : history) {
        if (p.metodo() != null && !p.metodo().isBlank()) methods.add(p.metodo());
    }
}
%>

<div class="page-heading payments-page-heading">
  <div>
    <h1>Pagos</h1>
    <p>Registra y consulta cobros asociados a citas atendidas de <%= Html.escape(user.clinicName()) %>.</p>
  </div>

  <% if (canRegister) { %>
  <div class="payments-heading-actions">
    <button class="btn primary"
            type="button"
            data-open-payment-dialog
            <%= pendingCount == 0 ? "disabled" : "" %>>
      + Registrar pago
    </button>
  </div>
  <% } %>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<section class="payments-kpis" aria-label="Resumen de pagos">
  <article>
    <span>Citas pendientes de pago</span>
    <strong><%= pendingCount %></strong>
    <small>Citas atendidas sin cobro registrado</small>
  </article>
  <article>
    <span>Cobros visibles</span>
    <strong><%= historyCount %></strong>
    <small>Últimos pagos recuperados por el módulo</small>
  </article>
  <article>
    <span>Métodos utilizados</span>
    <strong><%= methods.size() %></strong>
    <small><%= methods.isEmpty() ? "Sin cobros registrados" : Html.escape(String.join(" · ", methods)) %></small>
  </article>
</section>

<% if (canRegister && pendingCount == 0) { %>
<section class="payments-empty-note">
  <div>
    <strong>No hay cobros pendientes</strong>
    <span>Cuando una cita pase a estado ATENDIDA y aún no tenga pago registrado, aparecerá disponible para cobro.</span>
  </div>
</section>
<% } %>

<section class="table-card crud-list payments-list">
  <div class="table-top payments-table-top">
    <div>
      <h2>Historial de pagos</h2>
      <span>Últimos 50 cobros registrados para esta clínica.</span>
    </div>

    <% if (canRegister && pendingCount > 0) { %>
      <button class="btn tiny primary" type="button" data-open-payment-dialog>+ Registrar pago</button>
    <% } %>
  </div>

  <div class="table-scroll payments-table-scroll">
    <table>
      <thead>
        <tr>
          <th>Fecha</th>
          <th>Paciente</th>
          <th>Monto</th>
          <th>Método</th>
        </tr>
      </thead>
      <tbody>
      <% if (history != null) for (PagoResumen p : history) { %>
        <tr>
          <td><%= Html.escape(p.fecha()) %></td>
          <td><strong><%= Html.escape(p.paciente()) %></strong></td>
          <td class="payments-amount">S/ <%= Html.escape(p.monto()) %></td>
          <td>
            <span class="status-pill payment-method payment-method-<%= Html.escape(p.metodo().toLowerCase(java.util.Locale.ROOT)) %>">
              <%= Html.escape(p.metodo()) %>
            </span>
          </td>
        </tr>
      <% } %>

      <% if (history == null || history.isEmpty()) { %>
        <tr>
          <td colspan="4" class="empty">Todavía no existen pagos registrados.</td>
        </tr>
      <% } %>
      </tbody>
    </table>
  </div>
</section>

<% if (canRegister && pendingCount > 0) { %>
<dialog class="crud-card crud-dialog payments-dialog" data-payment-dialog>
  <div class="crud-head payments-dialog-head">
    <div>
      <span class="payments-dialog-eyebrow">COBROS</span>
      <h2>Registrar pago</h2>
      <p>Solo se muestran citas ATENDIDAS que todavía no tienen un cobro registrado.</p>
    </div>
    <button class="btn secondary" type="button" data-close-payment-dialog>Cerrar</button>
  </div>

  <form class="crud-form payments-form"
        action="<%= Html.escape(base) %>/modulo/pagos"
        method="post"
        autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="registrar">

    <label class="span-2">Cita atendida
      <select name="id_cita" required>
        <option value="">Selecciona una cita</option>
        <% for (PagoPendiente p : pending) { %>
          <option value="<%= p.idCita() %>">
            <%= Html.escape(p.fecha() + " " + p.hora() + " · " + p.paciente() + " · " + p.odontologo()) %>
          </option>
        <% } %>
      </select>
      <small>El pago quedará relacionado permanentemente con la cita seleccionada.</small>
    </label>

    <label>Monto (S/)
      <input type="number"
             name="monto"
             min="0.01"
             max="99999999.99"
             step="0.01"
             inputmode="decimal"
             required
             placeholder="0.00">
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

    <label class="span-2">Observaciones
      <textarea name="observaciones"
                maxlength="1000"
                rows="3"
                placeholder="Dato adicional del cobro (opcional)"></textarea>
    </label>

    <div class="payments-payment-note span-2">
      <strong>Registro único por cita</strong>
      <span>OdontoSync valida que la cita pertenezca a esta clínica, esté ATENDIDA y todavía no tenga un pago registrado.</span>
    </div>

    <div class="form-actions">
      <button class="btn secondary" type="button" data-close-payment-dialog>Cancelar</button>
      <button class="btn primary" type="submit">Registrar pago</button>
    </div>
  </form>
</dialog>
<% } %>

<p class="read-only payments-scope-note">
  Los cobros están aislados por clínica. La carga de comprobantes permanece fuera del alcance actual del módulo.
</p>

<script src="<%= Html.escape(base) %>/js/pagos-ui.js?v=20261120-issue29-ux1" defer></script>
<%@ include file="layout-bottom.jspf" %>
