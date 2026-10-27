<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,pe.odontosync.Role,pe.odontosync.model.UsuarioDetalle" %>
<%@ include file="layout-top.jspf" %>
<%
List<UsuarioDetalle> users = (List<UsuarioDetalle>) request.getAttribute("usuarios");
UsuarioDetalle edit = (UsuarioDetalle) request.getAttribute("editUser");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
boolean editing = edit != null;
%>
<div class="page-heading">
  <div>
    <h1>Usuarios y roles</h1>
    <p>Administra las cuentas que pueden ingresar a OdontoSync.</p>
  </div>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<section class="crud-card">
  <div class="crud-head">
    <div>
      <h2><%= editing ? "Editar usuario" : "Registrar usuario" %></h2>
      <p><%= editing ? "Modifica los datos de la cuenta seleccionada." : "Crea una cuenta nueva para administración, recepción u odontología." %></p>
    </div>
    <% if (editing) { %><a class="btn secondary" href="<%= Html.escape(base) %>/usuarios">Cancelar edición</a><% } %>
  </div>

  <form class="crud-form" action="<%= Html.escape(base) %>/usuarios" method="post" autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="<%= editing ? "actualizar" : "crear" %>">
    <% if (editing) { %><input type="hidden" name="id_usuario" value="<%= edit.id() %>"><% } %>

    <label>Nombres
      <input name="nombres" maxlength="100" required value="<%= Html.escape(editing ? edit.nombres() : "") %>">
    </label>
    <label>Apellidos
      <input name="apellidos" maxlength="100" required value="<%= Html.escape(editing ? edit.apellidos() : "") %>">
    </label>
    <label>Correo electrónico
      <input type="email" name="correo" maxlength="150" required value="<%= Html.escape(editing ? edit.correo() : "") %>">
    </label>
    <label>Teléfono
      <input name="telefono" maxlength="20" value="<%= Html.escape(editing && edit.telefono() != null ? edit.telefono() : "") %>">
    </label>
    <label>Rol
      <select name="rol" required>
        <% for (Role r : Role.values()) { boolean selected = editing ? edit.role() == r : r == Role.RECEPCIONISTA; %>
          <option value="<%= r.name() %>" <%= selected ? "selected" : "" %>><%= Html.escape(r.label()) %></option>
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
    </label>
    <label>N.º colegiatura
      <input name="colegiatura" maxlength="50" value="<%= Html.escape(editing && edit.numeroColegiatura() != null ? edit.numeroColegiatura() : "") %>">
      <small>Obligatorio únicamente si el rol es Odontólogo.</small>
    </label>
    <label>Contraseña
      <input type="password" name="clave" maxlength="72" <%= editing ? "" : "required" %>>
      <small><%= editing ? "Déjala vacía para conservar la contraseña actual." : "Mínimo 8 caracteres, con mayúscula, minúscula y número." %></small>
    </label>

    <div class="form-actions">
      <button class="btn primary" type="submit"><%= editing ? "Guardar cambios" : "Crear usuario" %></button>
    </div>
  </form>
</section>

<section class="table-card crud-list">
  <div class="table-top">
    <div><h2>Usuarios registrados</h2><span><%= users == null ? 0 : users.size() %> cuenta(s) de esta clínica</span></div>
  </div>
  <div class="table-scroll">
    <table>
      <thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Colegiatura</th><th>Acciones</th></tr></thead>
      <tbody>
      <% if (users != null) for (UsuarioDetalle item : users) { %>
        <tr>
          <td><strong><%= Html.escape(item.nombres() + " " + item.apellidos()) %></strong></td>
          <td><%= Html.escape(item.correo()) %></td>
          <td><span class="status-pill"><%= Html.escape(item.role().label()) %></span></td>
          <td><%= Html.escape(item.estado()) %></td>
          <td><%= Html.escape(item.numeroColegiatura() == null ? "—" : item.numeroColegiatura()) %></td>
          <td>
            <div class="row-actions">
              <a class="btn tiny secondary" href="<%= Html.escape(base) %>/usuarios?editar=<%= item.id() %>">Editar</a>
              <% if (item.id() != user.id()) { %>
              <form action="<%= Html.escape(base) %>/usuarios" method="post">
                <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
                <input type="hidden" name="accion" value="eliminar">
                <input type="hidden" name="id_usuario" value="<%= item.id() %>">
                <button class="btn tiny danger-btn" type="submit">Eliminar</button>
              </form>
              <% } else { %><span class="self-label">Cuenta actual</span><% } %>
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

<p class="read-only">Al crear un usuario con rol Odontólogo también se crea o vincula su perfil profesional. Las contraseñas se guardan con hash PBKDF2, nunca como texto plano.</p>
<%@ include file="layout-bottom.jspf" %>
