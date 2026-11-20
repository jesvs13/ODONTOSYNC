<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.math.BigDecimal,pe.odontosync.model.*" %>
<%@ include file="layout-top.jspf" %>
<link rel="stylesheet" href="<%= Html.escape(base) %>/css/inventario.css?v=20261120-issue6">
<%
List<InventarioInsumo> items = (List<InventarioInsumo>) request.getAttribute("inventoryItems");
List<InventarioCategoria> categories = (List<InventarioCategoria>) request.getAttribute("inventoryCategories");
List<InventarioMovimiento> history = (List<InventarioMovimiento>) request.getAttribute("inventoryHistory");
InventarioInsumo edit = (InventarioInsumo) request.getAttribute("editInventoryItem");
InventarioInsumo movementItem = (InventarioInsumo) request.getAttribute("movementInventoryItem");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
boolean canManage = Boolean.TRUE.equals(request.getAttribute("canManageInventory"));
boolean editing = edit != null;
int lowStock = 0;
int noStock = 0;
int activeItems = 0;
if (items != null) {
    for (InventarioInsumo item : items) {
        if ("ACTIVO".equals(item.estado())) {
            activeItems++;
            if (item.stockBajo()) lowStock++;
            if (item.sinStock()) noStock++;
        }
    }
}
%>
<div class="page-heading">
  <div>
    <h1>Inventario</h1>
    <p>Administra insumos, movimientos y alertas de stock de <%= Html.escape(user.clinicName()) %>.</p>
  </div>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<section class="inventory-kpis" aria-label="Resumen de inventario">
  <article><span>Insumos activos</span><strong><%= activeItems %></strong></article>
  <article class="<%= lowStock > 0 ? "inventory-warning" : "" %>"><span>Stock bajo</span><strong><%= lowStock %></strong></article>
  <article class="<%= noStock > 0 ? "inventory-danger" : "" %>"><span>Sin stock</span><strong><%= noStock %></strong></article>
  <article><span>Movimientos visibles</span><strong><%= history == null ? 0 : history.size() %></strong></article>
</section>

<% if (canManage) { %>
<section class="crud-card">
  <div class="crud-head">
    <div>
      <h2><%= editing ? "Editar insumo" : "Registrar insumo" %></h2>
      <p>El stock no se edita aquí: cambia únicamente mediante Entrada, Salida o Ajuste para conservar trazabilidad.</p>
    </div>
    <% if (editing) { %><a class="btn secondary" href="<%= Html.escape(base) %>/modulo/inventario">Cancelar edición</a><% } %>
  </div>

  <form class="crud-form" action="<%= Html.escape(base) %>/modulo/inventario" method="post" autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="<%= editing ? "actualizar" : "crear" %>">
    <% if (editing) { %><input type="hidden" name="id_insumo" value="<%= edit.id() %>"><% } %>

    <label>Nombre del insumo
      <input name="nombre" maxlength="150" required value="<%= Html.escape(editing ? edit.nombre() : "") %>" placeholder="Ej. Resina fotocurable A2">
    </label>

    <label>Categoría
      <select name="id_categoria" required>
        <option value="">Selecciona una categoría</option>
        <% if (categories != null) for (InventarioCategoria category : categories) { %>
          <option value="<%= category.id() %>" <%= editing && edit.categoriaId() == category.id() ? "selected" : "" %>><%= Html.escape(category.nombre()) %></option>
        <% } %>
      </select>
    </label>

    <label>Unidad de medida
      <input name="unidad_medida" maxlength="30" required value="<%= Html.escape(editing ? edit.unidadMedida() : "") %>" placeholder="unidad, caja, frasco...">
    </label>

    <label>Stock mínimo
      <input type="number" name="stock_minimo" min="0" max="99999999.99" step="0.01" required value="<%= editing ? edit.stockMinimo().toPlainString() : "0.00" %>">
      <small>Se mostrará una alerta cuando el stock actual sea igual o menor.</small>
    </label>

    <label>Precio referencial (S/)
      <input type="number" name="precio_referencial" min="0" max="99999999.99" step="0.01" value="<%= editing && edit.precioReferencial() != null ? edit.precioReferencial().toPlainString() : "" %>" placeholder="Opcional">
    </label>

    <label>Estado
      <% String currentStatus = editing ? edit.estado() : "ACTIVO"; %>
      <select name="estado" required>
        <option value="ACTIVO" <%= "ACTIVO".equals(currentStatus) ? "selected" : "" %>>ACTIVO</option>
        <option value="INACTIVO" <%= "INACTIVO".equals(currentStatus) ? "selected" : "" %>>INACTIVO</option>
      </select>
    </label>

    <label class="span-2">Descripción
      <textarea name="descripcion" maxlength="255" rows="3" placeholder="Detalle opcional del insumo"><%= Html.escape(editing && edit.descripcion() != null ? edit.descripcion() : "") %></textarea>
    </label>

    <div class="form-actions">
      <button class="btn primary" type="submit"><%= editing ? "Guardar cambios" : "Registrar insumo" %></button>
    </div>
  </form>
</section>

<section id="movimiento" class="crud-card inventory-movement-card">
  <div class="crud-head">
    <div>
      <h2>Registrar movimiento</h2>
      <p>Entrada suma stock, Salida descuenta stock y Ajuste establece el stock final según un conteo físico.</p>
    </div>
  </div>

  <form class="crud-form" action="<%= Html.escape(base) %>/modulo/inventario" method="post">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="movimiento">

    <label>Insumo
      <select name="id_insumo" required>
        <option value="">Selecciona un insumo activo</option>
        <% if (items != null) for (InventarioInsumo item : items) if ("ACTIVO".equals(item.estado())) { %>
          <option value="<%= item.id() %>" <%= movementItem != null && movementItem.id() == item.id() ? "selected" : "" %>><%= Html.escape(item.nombre()) %> · stock <%= item.stockActual().toPlainString() %> <%= Html.escape(item.unidadMedida()) %></option>
        <% } %>
      </select>
    </label>

    <label>Tipo de movimiento
      <select name="tipo" required>
        <option value="">Selecciona un tipo</option>
        <option value="ENTRADA">ENTRADA · sumar stock</option>
        <option value="SALIDA">SALIDA · descontar stock</option>
        <option value="AJUSTE">AJUSTE · establecer stock final</option>
      </select>
    </label>

    <label>Cantidad
      <input type="number" name="cantidad" min="0.01" max="99999999.99" step="0.01" required placeholder="0.00">
      <small>En AJUSTE, escribe la cantidad física final que debe quedar.</small>
    </label>

    <label>Motivo
      <input name="motivo" minlength="3" maxlength="255" required placeholder="Ej. Compra mensual, consumo clínico o conteo físico">
    </label>

    <div class="form-actions">
      <button class="btn primary" type="submit">Registrar movimiento</button>
    </div>
  </form>
</section>
<% } %>

<section class="table-card crud-list">
  <div class="table-top">
    <div>
      <h2>Existencias</h2>
      <span>El stock actual proviene exclusivamente del historial de movimientos.</span>
    </div>
  </div>
  <div class="table-scroll">
    <table>
      <thead>
        <tr><th>Insumo</th><th>Categoría</th><th>Stock</th><th>Mínimo</th><th>Alerta</th><th>Estado</th><th>Acciones</th></tr>
      </thead>
      <tbody>
      <% if (items != null) for (InventarioInsumo item : items) { %>
        <tr class="<%= item.sinStock() && "ACTIVO".equals(item.estado()) ? "inventory-row-danger" : item.stockBajo() && "ACTIVO".equals(item.estado()) ? "inventory-row-warning" : "" %>">
          <td><strong><%= Html.escape(item.nombre()) %></strong><br><small class="table-secondary"><%= Html.escape(item.unidadMedida()) %><%= item.descripcion() == null || item.descripcion().isBlank() ? "" : " · " + Html.escape(item.descripcion()) %></small></td>
          <td><%= Html.escape(item.categoria()) %></td>
          <td><strong><%= item.stockActual().toPlainString() %></strong></td>
          <td><%= item.stockMinimo().toPlainString() %></td>
          <td>
            <% if (item.sinStock()) { %><span class="status-pill inventory-alert-danger">SIN STOCK</span>
            <% } else if (item.stockBajo()) { %><span class="status-pill inventory-alert-warning">STOCK BAJO</span>
            <% } else { %><span class="status-pill">NORMAL</span><% } %>
          </td>
          <td><span class="status-pill <%= "INACTIVO".equals(item.estado()) ? "status-inactive" : "" %>"><%= Html.escape(item.estado()) %></span></td>
          <td>
            <div class="row-actions">
              <% if (canManage) { %>
                <a class="btn tiny secondary" href="<%= Html.escape(base) %>/modulo/inventario?movimiento=<%= item.id() %>#movimiento">Movimiento</a>
                <a class="btn tiny secondary" href="<%= Html.escape(base) %>/modulo/inventario?editar=<%= item.id() %>">Editar</a>
                <form action="<%= Html.escape(base) %>/modulo/inventario" method="post">
                  <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
                  <input type="hidden" name="accion" value="estado">
                  <input type="hidden" name="id_insumo" value="<%= item.id() %>">
                  <input type="hidden" name="estado" value="<%= "ACTIVO".equals(item.estado()) ? "INACTIVO" : "ACTIVO" %>">
                  <button class="btn tiny <%= "ACTIVO".equals(item.estado()) ? "danger-btn" : "success-btn" %>" type="submit"><%= "ACTIVO".equals(item.estado()) ? "Desactivar" : "Reactivar" %></button>
                </form>
              <% } %>
            </div>
          </td>
        </tr>
      <% } %>
      <% if (items == null || items.isEmpty()) { %><tr><td colspan="7" class="empty">Todavía no existen insumos registrados.</td></tr><% } %>
      </tbody>
    </table>
  </div>
</section>

<section class="table-card crud-list inventory-history">
  <div class="table-top">
    <div>
      <h2>Historial de movimientos</h2>
      <span>Últimos 100 movimientos de esta clínica, con usuario y motivo.</span>
    </div>
  </div>
  <div class="table-scroll">
    <table>
      <thead>
        <tr><th>Fecha</th><th>Insumo</th><th>Tipo</th><th>Cantidad</th><th>Motivo</th><th>Registrado por</th></tr>
      </thead>
      <tbody>
      <% if (history != null) for (InventarioMovimiento movement : history) { %>
        <tr>
          <td><%= Html.escape(movement.fecha()) %></td>
          <td><strong><%= Html.escape(movement.insumo()) %></strong></td>
          <td><span class="status-pill inventory-type-<%= movement.tipo().toLowerCase(java.util.Locale.ROOT) %>"><%= Html.escape(movement.tipo()) %></span></td>
          <td><%= movement.cantidad().toPlainString() %></td>
          <td><%= Html.escape(movement.motivo()) %></td>
          <td><%= Html.escape(movement.usuario()) %></td>
        </tr>
      <% } %>
      <% if (history == null || history.isEmpty()) { %><tr><td colspan="6" class="empty">Todavía no existen movimientos registrados.</td></tr><% } %>
      </tbody>
    </table>
  </div>
</section>

<p class="read-only">Los insumos y movimientos se limitan a la clínica de la sesión. El stock no se modifica directamente: toda variación queda registrada en el historial.</p>
<%@ include file="layout-bottom.jspf" %>
