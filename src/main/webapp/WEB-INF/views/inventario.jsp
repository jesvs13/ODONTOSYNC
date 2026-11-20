<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.math.BigDecimal,pe.odontosync.model.*" %>
<%@ include file="layout-top.jspf" %>
<link rel="stylesheet" href="<%= Html.escape(base) %>/css/inventario.css?v=20261120-issue29-ux1">
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
boolean movementSelected = movementItem != null;
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

<div class="page-heading inventory-heading">
  <div>
    <h1>Inventario</h1>
    <p>Administra insumos, movimientos y alertas de stock de <%= Html.escape(user.clinicName()) %>.</p>
  </div>
  <% if (canManage) { %>
  <div class="inventory-heading-actions">
    <button class="btn secondary" type="button" data-open-inventory-movement>Registrar movimiento</button>
    <button class="btn primary" type="button" data-open-inventory-item>+ Nuevo insumo</button>
  </div>
  <% } %>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<section class="inventory-kpis" aria-label="Resumen de inventario">
  <article>
    <span>Insumos activos</span>
    <strong><%= activeItems %></strong>
    <small>Disponibles para movimientos</small>
  </article>
  <article class="<%= lowStock > 0 ? "inventory-warning" : "" %>">
    <span>Stock bajo</span>
    <strong><%= lowStock %></strong>
    <small>Igual o menor al mínimo</small>
  </article>
  <article class="<%= noStock > 0 ? "inventory-danger" : "" %>">
    <span>Sin stock</span>
    <strong><%= noStock %></strong>
    <small>Requieren reposición</small>
  </article>
  <article>
    <span>Movimientos visibles</span>
    <strong><%= history == null ? 0 : history.size() %></strong>
    <small>Historial reciente de la clínica</small>
  </article>
</section>

<nav class="inventory-tabs" aria-label="Vistas de inventario">
  <button class="inventory-tab active" type="button" data-inventory-tab="existencias" aria-selected="true">
    Existencias
    <span><%= items == null ? 0 : items.size() %></span>
  </button>
  <button class="inventory-tab" type="button" data-inventory-tab="historial" aria-selected="false">
    Historial de movimientos
    <span><%= history == null ? 0 : history.size() %></span>
  </button>
</nav>

<section class="inventory-panel" data-inventory-panel="existencias">
  <div class="table-card crud-list">
    <div class="table-top inventory-table-top">
      <div>
        <h2>Existencias</h2>
        <span>El stock actual proviene exclusivamente del historial de movimientos.</span>
      </div>
      <% if (canManage) { %>
        <button class="btn tiny primary" type="button" data-open-inventory-item>+ Nuevo insumo</button>
      <% } %>
    </div>

    <div class="table-scroll inventory-table-scroll">
      <table>
        <thead>
          <tr>
            <th>Insumo</th>
            <th>Categoría</th>
            <th>Stock</th>
            <th>Mínimo</th>
            <th>Alerta</th>
            <th>Estado</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
        <% if (items != null) for (InventarioInsumo item : items) { %>
          <tr class="<%= item.sinStock() && "ACTIVO".equals(item.estado()) ? "inventory-row-danger" : item.stockBajo() && "ACTIVO".equals(item.estado()) ? "inventory-row-warning" : "" %>">
            <td>
              <strong><%= Html.escape(item.nombre()) %></strong><br>
              <small class="table-secondary">
                <%= Html.escape(item.unidadMedida()) %><%= item.descripcion() == null || item.descripcion().isBlank() ? "" : " · " + Html.escape(item.descripcion()) %>
              </small>
            </td>
            <td><%= Html.escape(item.categoria()) %></td>
            <td><strong><%= item.stockActual().toPlainString() %></strong></td>
            <td><%= item.stockMinimo().toPlainString() %></td>
            <td>
              <% if (item.sinStock()) { %>
                <span class="status-pill inventory-alert-danger">SIN STOCK</span>
              <% } else if (item.stockBajo()) { %>
                <span class="status-pill inventory-alert-warning">STOCK BAJO</span>
              <% } else { %>
                <span class="status-pill">NORMAL</span>
              <% } %>
            </td>
            <td>
              <span class="status-pill <%= "INACTIVO".equals(item.estado()) ? "status-inactive" : "" %>">
                <%= Html.escape(item.estado()) %>
              </span>
            </td>
            <td>
              <div class="row-actions">
                <% if (canManage) { %>
                  <a class="btn tiny secondary" href="<%= Html.escape(base) %>/modulo/inventario?movimiento=<%= item.id() %>">Movimiento</a>
                  <a class="btn tiny secondary" href="<%= Html.escape(base) %>/modulo/inventario?editar=<%= item.id() %>">Editar</a>
                  <form action="<%= Html.escape(base) %>/modulo/inventario" method="post">
                    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
                    <input type="hidden" name="accion" value="estado">
                    <input type="hidden" name="id_insumo" value="<%= item.id() %>">
                    <input type="hidden" name="estado" value="<%= "ACTIVO".equals(item.estado()) ? "INACTIVO" : "ACTIVO" %>">
                    <button class="btn tiny <%= "ACTIVO".equals(item.estado()) ? "danger-btn" : "success-btn" %>" type="submit">
                      <%= "ACTIVO".equals(item.estado()) ? "Desactivar" : "Reactivar" %>
                    </button>
                  </form>
                <% } %>
              </div>
            </td>
          </tr>
        <% } %>
        <% if (items == null || items.isEmpty()) { %>
          <tr><td colspan="7" class="empty">Todavía no existen insumos registrados.</td></tr>
        <% } %>
        </tbody>
      </table>
    </div>
  </div>
</section>

<section class="inventory-panel" data-inventory-panel="historial" hidden>
  <div class="table-card crud-list">
    <div class="table-top inventory-table-top">
      <div>
        <h2>Historial de movimientos</h2>
        <span>Últimos 100 movimientos de esta clínica, con usuario y motivo.</span>
      </div>
      <% if (canManage) { %>
        <button class="btn tiny primary" type="button" data-open-inventory-movement>Registrar movimiento</button>
      <% } %>
    </div>

    <div class="table-scroll inventory-table-scroll inventory-history-scroll">
      <table>
        <thead>
          <tr>
            <th>Fecha</th>
            <th>Insumo</th>
            <th>Tipo</th>
            <th>Cantidad</th>
            <th>Motivo</th>
            <th>Registrado por</th>
          </tr>
        </thead>
        <tbody>
        <% if (history != null) for (InventarioMovimiento movement : history) { %>
          <tr>
            <td><%= Html.escape(movement.fecha()) %></td>
            <td><strong><%= Html.escape(movement.insumo()) %></strong></td>
            <td>
              <span class="status-pill inventory-type-<%= movement.tipo().toLowerCase(java.util.Locale.ROOT) %>">
                <%= Html.escape(movement.tipo()) %>
              </span>
            </td>
            <td><%= movement.cantidad().toPlainString() %></td>
            <td><%= Html.escape(movement.motivo()) %></td>
            <td><%= Html.escape(movement.usuario()) %></td>
          </tr>
        <% } %>
        <% if (history == null || history.isEmpty()) { %>
          <tr><td colspan="6" class="empty">Todavía no existen movimientos registrados.</td></tr>
        <% } %>
        </tbody>
      </table>
    </div>
  </div>
</section>

<% if (canManage) { %>
<dialog class="crud-card crud-dialog inventory-dialog inventory-item-dialog"
        data-inventory-item-dialog
        data-auto-open="<%= editing %>">
  <div class="crud-head">
    <div>
      <span class="inventory-dialog-eyebrow">INSUMOS</span>
      <h2><%= editing ? "Editar insumo" : "Registrar insumo" %></h2>
      <p>El stock no se edita aquí. Toda variación debe registrarse mediante un movimiento.</p>
    </div>
    <a class="btn secondary" href="<%= Html.escape(base) %>/modulo/inventario">Cancelar</a>
  </div>

  <form class="crud-form inventory-item-form"
        action="<%= Html.escape(base) %>/modulo/inventario"
        method="post"
        autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="<%= editing ? "actualizar" : "crear" %>">
    <% if (editing) { %><input type="hidden" name="id_insumo" value="<%= edit.id() %>"><% } %>

    <label>Nombre del insumo
      <input name="nombre" maxlength="150" required
             value="<%= Html.escape(editing ? edit.nombre() : "") %>"
             placeholder="Ej. Resina fotocurable A2">
    </label>

    <label>Categoría
      <select name="id_categoria" required>
        <option value="">Selecciona una categoría</option>
        <% if (categories != null) for (InventarioCategoria category : categories) { %>
          <option value="<%= category.id() %>"
                  <%= editing && edit.categoriaId() == category.id() ? "selected" : "" %>>
            <%= Html.escape(category.nombre()) %>
          </option>
        <% } %>
      </select>
    </label>

    <label>Unidad de medida
      <input name="unidad_medida" maxlength="30" required
             value="<%= Html.escape(editing ? edit.unidadMedida() : "") %>"
             placeholder="unidad, caja, frasco...">
    </label>

    <label>Stock mínimo
      <input type="number" name="stock_minimo" min="0" max="99999999.99" step="0.01" required
             value="<%= editing ? edit.stockMinimo().toPlainString() : "0.00" %>">
      <small>Se mostrará una alerta cuando el stock actual sea igual o menor.</small>
    </label>

    <label>Precio referencial (S/)
      <input type="number" name="precio_referencial" min="0" max="99999999.99" step="0.01"
             value="<%= editing && edit.precioReferencial() != null ? edit.precioReferencial().toPlainString() : "" %>"
             placeholder="Opcional">
    </label>

    <label>Estado
      <% String currentStatus = editing ? edit.estado() : "ACTIVO"; %>
      <select name="estado" required>
        <option value="ACTIVO" <%= "ACTIVO".equals(currentStatus) ? "selected" : "" %>>ACTIVO</option>
        <option value="INACTIVO" <%= "INACTIVO".equals(currentStatus) ? "selected" : "" %>>INACTIVO</option>
      </select>
    </label>

    <label class="span-2">Descripción
      <textarea name="descripcion" maxlength="255" rows="3"
                placeholder="Detalle opcional del insumo"><%= Html.escape(editing && edit.descripcion() != null ? edit.descripcion() : "") %></textarea>
    </label>

    <div class="form-actions">
      <a class="btn secondary" href="<%= Html.escape(base) %>/modulo/inventario">Cancelar</a>
      <button class="btn primary" type="submit"><%= editing ? "Guardar cambios" : "Registrar insumo" %></button>
    </div>
  </form>
</dialog>

<dialog class="crud-card crud-dialog inventory-dialog inventory-movement-dialog"
        data-inventory-movement-dialog
        data-auto-open="<%= movementSelected %>">
  <div class="crud-head">
    <div>
      <span class="inventory-dialog-eyebrow">MOVIMIENTOS</span>
      <h2>Registrar movimiento</h2>
      <p>Entrada suma stock, Salida descuenta y Ajuste establece el stock final del conteo físico.</p>
    </div>
    <a class="btn secondary" href="<%= Html.escape(base) %>/modulo/inventario">Cancelar</a>
  </div>

  <form class="crud-form inventory-movement-form"
        action="<%= Html.escape(base) %>/modulo/inventario"
        method="post"
        autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="movimiento">

    <label class="span-2">Insumo
      <select name="id_insumo" required>
        <option value="">Selecciona un insumo activo</option>
        <% if (items != null) for (InventarioInsumo item : items) if ("ACTIVO".equals(item.estado())) { %>
          <option value="<%= item.id() %>"
                  <%= movementItem != null && movementItem.id() == item.id() ? "selected" : "" %>>
            <%= Html.escape(item.nombre()) %> · stock <%= item.stockActual().toPlainString() %> <%= Html.escape(item.unidadMedida()) %>
          </option>
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

    <label class="span-2">Motivo
      <input name="motivo" minlength="3" maxlength="255" required
             placeholder="Ej. Compra mensual, consumo clínico o conteo físico">
    </label>

    <div class="form-actions">
      <a class="btn secondary" href="<%= Html.escape(base) %>/modulo/inventario">Cancelar</a>
      <button class="btn primary" type="submit">Registrar movimiento</button>
    </div>
  </form>
</dialog>
<% } %>

<p class="read-only inventory-scope-note">
  Los insumos y movimientos se limitan a la clínica de la sesión. El stock no se modifica directamente: toda variación queda registrada en el historial.
</p>

<script src="<%= Html.escape(base) %>/js/inventario-ui.js?v=20261120-issue29-ux1" defer></script>
<%@ include file="layout-bottom.jspf" %>
