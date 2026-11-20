<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.odontosync.Html" %>
<%
String error = (String) request.getAttribute("error");
String token = (String) request.getAttribute("token");
String csrf = (String) request.getAttribute("csrf");
String base = request.getContextPath();
boolean usableForm = error == null || (token != null && token.length() == 43);
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Restablecer contraseña | OdontoSync</title>
  <link rel="icon" type="image/svg+xml" href="<%= Html.escape(base) %>/brand/favicon.svg">
  <link rel="stylesheet" href="<%= Html.escape(base) %>/css/estilos.css?v=20261112-password-reset">
</head>
<body>
  <main class="login-screen">
    <section class="login-card password-reset-card">
      <img class="brand-login" src="<%= Html.escape(base) %>/brand/logo-login.svg" alt="OdontoSync - Gestión Dental Digital" width="260" height="65">
      <p class="muted login-subtitle">Protección de cuenta</p>
      <div class="divider"></div>
      <h1>Restablecer contraseña</h1>

      <% if (error != null) { %><p class="alert danger" role="alert"><%= Html.escape(error) %></p><% } %>

      <% if (usableForm) { %>
      <form action="<%= Html.escape(base) %>/restablecer-password" method="post">
        <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
        <input type="hidden" name="token" value="<%= Html.escape(token) %>">

        <label for="clave_nueva">Nueva contraseña</label>
        <input id="clave_nueva" name="clave_nueva" type="password" autocomplete="new-password" required minlength="8" maxlength="72">

        <label for="clave_confirmacion">Confirmar nueva contraseña</label>
        <input id="clave_confirmacion" name="clave_confirmacion" type="password" autocomplete="new-password" required minlength="8" maxlength="72">

        <p class="reset-requirements">Debe tener entre 8 y 72 caracteres e incluir mayúscula, minúscula y número.</p>
        <button class="primary" type="submit">Guardar nueva contraseña</button>
      </form>
      <% } %>

      <p class="login-note reset-back"><a href="<%= Html.escape(base) %>/olvide-password">Solicitar un nuevo enlace</a></p>
      <p class="login-note reset-back"><a href="<%= Html.escape(base) %>/login">Volver al login</a></p>
    </section>
  </main>
</body>
</html>
