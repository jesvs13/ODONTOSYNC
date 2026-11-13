<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="layout-top.jspf" %>
<%
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
%>
<div class="page-heading">
  <div>
    <h1>Cambiar contraseña</h1>
    <p>Actualiza la contraseña de tu cuenta sin almacenar la clave en texto plano.</p>
  </div>
</div>

<% if (success != null) { %>
  <div class="alert success"><%= Html.escape(success) %></div>
<% } %>
<% if (error != null) { %>
  <div class="alert danger"><%= Html.escape(error) %></div>
<% } %>

<section class="crud-card password-card">
  <div class="crud-head">
    <div>
      <h2>Seguridad de la cuenta</h2>
      <p>Confirma tu contraseña actual y define una nueva contraseña segura.</p>
    </div>
  </div>

  <form class="crud-form password-form" action="<%= Html.escape(base) %>/cambiar-password" method="post" autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">

    <label>Contraseña actual
      <input type="password" name="clave_actual" maxlength="1000" autocomplete="current-password" required>
    </label>

    <label>Nueva contraseña
      <input type="password" name="clave_nueva" maxlength="72" autocomplete="new-password" required>
      <small>Debe tener entre 8 y 72 caracteres e incluir mayúscula, minúscula y número.</small>
    </label>

    <label>Confirmar nueva contraseña
      <input type="password" name="clave_confirmacion" maxlength="72" autocomplete="new-password" required>
    </label>

    <div class="form-actions password-actions">
      <button class="btn primary" type="submit">Actualizar contraseña</button>
    </div>
  </form>
</section>

<p class="read-only">La nueva contraseña se procesa con PBKDF2-HMAC-SHA256 y un salt aleatorio antes de guardarse.</p>
<%@ include file="layout-bottom.jspf" %>
