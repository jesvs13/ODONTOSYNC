<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.odontosync.Html" %>
<%
String base = request.getContextPath();
String csrf = (String) request.getAttribute("csrf");
String error = (String) request.getAttribute("error");
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Activar cuenta | OdontoSync</title>
  <link rel="icon" type="image/svg+xml" href="<%= Html.escape(base) %>/brand/favicon.svg">
  <link rel="stylesheet" href="<%= Html.escape(base) %>/css/estilos.css?v=20261127-activacion">
</head>
<body>
  <main class="login-screen">
    <section class="login-card password-reset-card">
      <img class="brand-login" src="<%= Html.escape(base) %>/brand/logo-login.svg" alt="OdontoSync - Gestión Dental Digital" width="260" height="65">
      <p class="muted login-subtitle">Activación de cuenta · Verificación completada</p>
      <div class="divider"></div>
      <h1>Establece tu contraseña</h1>
      <p class="muted">Por seguridad, debes cambiar tu contraseña temporal antes de ingresar al sistema.</p>
      <% if (error != null) { %><p class="alert danger" role="alert"><%= Html.escape(error) %></p><% } %>
      <form action="<%= Html.escape(base) %>/cambiar-password" method="post">
        <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
        <label for="clave_actual">Contraseña temporal actual</label>
        <input id="clave_actual" name="clave_actual" type="password" autocomplete="current-password" required maxlength="1000">
        <label for="clave_nueva">Nueva contraseña</label>
        <input id="clave_nueva" name="clave_nueva" type="password" autocomplete="new-password" required minlength="8" maxlength="72">
        <label for="clave_confirmacion">Confirmar nueva contraseña</label>
        <input id="clave_confirmacion" name="clave_confirmacion" type="password" autocomplete="new-password" required minlength="8" maxlength="72">
        <p class="reset-requirements">Usa entre 8 y 72 caracteres, con mayúscula, minúscula y número.</p>
        <button class="primary" type="submit">Activar cuenta e ingresar</button>
      </form>
      <form action="<%= Html.escape(base) %>/logout" method="post">
        <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
        <button class="btn secondary" type="submit">Cerrar sesión</button>
      </form>
      <p class="login-note">No compartas tu nueva contraseña con nadie.</p>
    </section>
  </main>
</body>
</html>
