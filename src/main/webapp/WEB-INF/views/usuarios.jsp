<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,pe.odontosync.Role,pe.odontosync.model.UsuarioDetalle" %>
<%@ include file="layout-top.jspf" %>
<link rel="stylesheet" href="<%= Html.escape(base) %>/css/usuarios-ui.css?v=20261120-issue29-ux1">
<%
List<UsuarioDetalle> users = (List<UsuarioDetalle>) request.getAttribute("usuarios");
UsuarioDetalle edit = (UsuarioDetalle) request.getAttribute("editUser");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
boolean editing = edit != null;

int totalUsers = users == null ? 0 : users.size();
int activeUsers = 0;
int inactiveUsers = 0;
int blockedUsers = 0;

if (users != null) {
    for (UsuarioDetalle item : users) {
        if ("ACTIVO".equals(item.estado())) activeUsers++;
        else if ("INACTIVO".equals(item.estado())) inactiveUsers++;
        else if ("BLOQUEADO".equals(item.estado())) blockedUsers++;
    }
}
%>

<div class="page-heading users-page-heading">
  <div>
    <h1>Usuarios y roles</h1>
    <p>Administra las cuentas que pueden ingresar a OdontoSync.</p>
  </div>
  <div class="users-heading-actions">
    <% if (editing) { %>
      <a class="btn primary" href="<%= Html.escape(base) %>/usuarios">+ Nuevo usuario</a>
    <% } else { %>
      <button class="btn primary" type="button" data-open-user-dialog>+ Nuevo usuario</button>
    <% } %>
  </div>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<section class="users-kpis" aria-label="Resumen de usuarios">
  <article>
    <span>Total de cuentas</span>
    <strong><%= totalUsers %></strong>
    <small>Usuarios registrados en esta clínica</small>
  </article>
  <article>
    <span>Activos</span>
    <strong><%= activeUsers %></strong>
    <small>Pueden iniciar sesión según sus permisos</small>
  </article>
  <article class="<%= inactiveUsers > 0 ? "users-kpi-muted" : "" %>">
    <span>Inactivos</span>
    <strong><%= inactiveUsers %></strong>
    <small>Cuentas deshabilitadas administrativamente</small>
  </article>
  <article class="<%= blockedUsers > 0 ? "users-kpi-warning" : "" %>">
    <span>Bloqueados</span>
    <strong><%= blockedUsers %></strong>
    <small>Requieren revisión del administrador</small>
  </article>
</section>

<section class="table-card crud-list users-list">
  <div class="table-top users-table-top">
    <div>
      <h2>Usuarios registrados</h2>
      <span><%= totalUsers %> cuenta(s) pertenecientes a <%= Html.escape(user.clinicName()) %></span>
    </div>
    <% if (!editing) { %>
      <button class="btn tiny primary" type="button" data-open-user-dialog>+ Nuevo usuario</button>
    <% } %>
  </div>

  <div class="table-scroll users-table-scroll">
    <table>
      <thead>
        <tr>
          <th>Usuario</th>
          <th>Contacto</th>
          <th>Rol</th>
          <th>Estado</th>
          <th>Colegiatura</th>
          <th>Acciones</th>
        </tr>
      </thead>
      <tbody>
      <% if (users != null) for (UsuarioDetalle item : users) { %>
        <tr class="<%= item.id() == user.id() ? "users-current-row" : "" %>">
          <td>
            <strong><%= Html.escape(item.nombres() + " " + item.apellidos()) %></strong>
            <% if (item.id() == user.id()) { %>
              <span class="users-current-badge">Cuenta actual</span>
            <% } %>
          </td>
          <td>
            <%= Html.escape(item.correo()) %>
            <% if (item.telefono() != null && !item.telefono().isBlank()) { %>
              <br><small class="table-secondary"><%= Html.escape(item.telefono()) %></small>
            <% } %>
          </td>
          <td><span class="status-pill users-role-pill"><%= Html.escape(item.role().label()) %></span></td>
          <td>
            <span class="status-pill users-status users-status-<%= item.estado().toLowerCase(java.util.Locale.ROOT) %>">
              <%= Html.escape(item.estado()) %>
            </span>
          </td>
          <td><%= Html.escape(item.numeroColegiatura() == null ? "—" : item.numeroColegiatura()) %></td>
          <td>
            <div class="row-actions users-row-actions">
              <a class="btn tiny secondary" href="<%= Html.escape(base) %>/usuarios?editar=<%= item.id() %>">Editar</a>
              <% if (item.id() != user.id()) { %>
              <form action="<%= Html.escape(base) %>/usuarios" method="post" data-user-delete-form>
                <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
                <input type="hidden" name="accion" value="eliminar">
                <input type="hidden" name="id_usuario" value="<%= item.id() %>">
                <button class="btn tiny danger-btn" type="submit">Eliminar</button>
              </form>
              <% } %>
            </div>
          </td>
        </tr>
      <% } %>

      <% if (users == null || users.isEmpty()) { %>
        <tr><td colspan="6" class="empty">No hay usuarios registrados.</td></tr>
      <% } %>
      </tbody>
    </table>
  </div>
</section>

<dialog class="crud-card crud-dialog users-dialog"
        data-user-dialog
        data-auto-open="<%= editing %>">
  <div class="crud-head users-dialog-head">
    <div>
      <span class="users-dialog-eyebrow">ACCESO Y ROLES</span>
      <h2><%= editing ? "Editar usuario" : "Registrar usuario" %></h2>
      <p><%= editing
          ? "Modifica los datos de la cuenta seleccionada."
          : "Crea una cuenta para administración, recepción u odontología." %></p>
    </div>
    <a class="btn secondary" href="<%= Html.escape(base) %>/usuarios">Cerrar</a>
  </div>

  <form class="crud-form users-form"
        action="<%= Html.escape(base) %>/usuarios"
        method="post"
        autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="<%= editing ? "actualizar" : "crear" %>">
    <% if (editing) { %><input type="hidden" name="id_usuario" value="<%= edit.id() %>"><% } %>

    <label>Nombres
      <input name="nombres" maxlength="100" required
             value="<%= Html.escape(editing ? edit.nombres() : "") %>">
    </label>

    <label>Apellidos
      <input name="apellidos" maxlength="100" required
             value="<%= Html.escape(editing ? edit.apellidos() : "") %>">
    </label>

    <label>Correo electrónico
      <input type="email" name="correo" maxlength="150" required
             value="<%= Html.escape(editing ? edit.correo() : "") %>">
    </label>

    <label>Teléfono
      <input name="telefono" maxlength="20"
             value="<%= Html.escape(editing && edit.telefono() != null ? edit.telefono() : "") %>"
             placeholder="Opcional">
    </label>

    <label>Rol
      <select name="rol" required data-user-role>
        <% for (Role r : Role.values()) {
             boolean selected = editing ? edit.role() == r : r == Role.RECEPCIONISTA; %>
          <option value="<%= r.name() %>" <%= selected ? "selected" : "" %>>
            <%= Html.escape(r.label()) %>
          </option>
        <% } %>
      </select>
    </label>

    <label>Estado
      <select name="estado" required>
        <% String selectedStatus = editing ? edit.estado() : "ACTIVO"; %>
        <% for (String st : List.of("ACTIVO","INACTIVO","BLOQUEADO")) { %>
          <option value="<%= st %>" <%= st.equals(selectedStatus) ? "selected" : "" %>><%= st %></option>
        <% } %>
      </select>
      <% if (editing && edit.id() == user.id()) { %>
        <small>Tu propia cuenta debe conservar el rol Administrador y estado ACTIVO.</small>
      <% } %>
    </label>

    <label data-collegiatura-field>
      N.º colegiatura
      <input name="colegiatura" maxlength="50"
             value="<%= Html.escape(editing && edit.numeroColegiatura() != null ? edit.numeroColegiatura() : "") %>">
      <small>Obligatorio únicamente si el rol es Odontólogo.</small>
    </label>

    <label>Contraseña
      <input type="password" name="clave" maxlength="72" <%= editing ? "" : "required" %>
             autocomplete="new-password">
      <small><%= editing
          ? "Déjala vacía para conservar la contraseña actual."
          : "Mínimo 8 caracteres, con mayúscula, minúscula y número." %></small>
    </label>

    <div class="users-security-note span-2">
      <strong>Seguridad de la cuenta</strong>
      <span>Los permisos se determinan por rol y las contraseñas se almacenan mediante hash PBKDF2, nunca como texto plano.</span>
    </div>

    <div class="form-actions">
      <a class="btn secondary" href="<%= Html.escape(base) %>/usuarios">Cancelar</a>
      <button class="btn primary" type="submit"><%= editing ? "Guardar cambios" : "Crear usuario" %></button>
    </div>
  </form>
</dialog>

<p class="read-only users-scope-note">
  Al crear un usuario con rol Odontólogo también se crea o vincula su perfil profesional. Todas las cuentas de esta vista pertenecen a la clínica de la sesión.
</p>

<script src="<%= Html.escape(base) %>/js/usuarios-ui.js?v=20261120-issue29-ux1" defer></script>
<%@ include file="layout-bottom.jspf" %>
