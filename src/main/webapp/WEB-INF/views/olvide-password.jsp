<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.odontosync.Html" %>
<%
String error = (String) request.getAttribute("error");
String success = (String) request.getAttribute("success");
String csrf = (String) request.getAttribute("csrf");
String base = request.getContextPath();
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Recuperar contraseña | OdontoSync</title>
  <link rel="icon" type="image/svg+xml" href="<%= Html.escape(base) %>/brand/favicon.svg">
  <link rel="stylesheet" href="<%= Html.escape(base) %>/css/estilos.css?v=20261112-password-reset">
</head>
<body>
  <main class="login-screen">
    <section class="login-card">
      <img class="brand-login" src="<%= Html.escape(base) %>/brand/logo-login.svg" alt="OdontoSync - Gestión Dental Digital" width="260" height="65">
      <p class="muted login-subtitle">Recuperación segura de acceso</p>
      <div class="divider"></div>
      <h1>¿Olvidaste tu contraseña?</h1>
      <p class="reset-help">Ingresa el correo registrado. Por seguridad, mostraremos el mismo resultado exista o no una cuenta.</p>

      <% if (error != null) { %><p class="alert danger" role="alert"><%= Html.escape(error) %></p><% } %>
      <% if (success != null) { %><p class="alert success" role="status"><%= Html.escape(success) %></p><% } %>

      <form action="<%= Html.escape(base) %>/olvide-password" method="post">
        <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
        <label for="correo">Correo electrónico</label>
        <input id="correo" name="correo" type="email" autocomplete="email" required maxlength="150" placeholder="usuario@odontosync.pe">
        <button class="primary" type="submit">Enviar instrucciones</button>
      </form>

      <p class="login-note reset-back"><a href="<%= Html.escape(base) %>/login">Volver a iniciar sesión</a></p>
    </section>
  </main>
</body>
</html>
