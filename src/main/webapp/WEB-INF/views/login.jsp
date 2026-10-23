<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.odontosync.Html" %>
<%
String error = (String) request.getAttribute("error");
String base = request.getContextPath();
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Iniciar sesión | OdontoSync</title>
  <link rel="icon" type="image/svg+xml" href="<%= Html.escape(base) %>/brand/favicon.svg">
  <link rel="icon" type="image/png" sizes="32x32" href="<%= Html.escape(base) %>/brand/icon-32.png">
  <link rel="icon" type="image/png" sizes="16x16" href="<%= Html.escape(base) %>/brand/icon-16.png">
  <link rel="stylesheet" href="<%= Html.escape(base) %>/css/estilos.css?v=20261012-semana10">
</head>
<body>
  <main class="login-screen">
    <section class="login-card">
      <img class="brand-login" src="<%= Html.escape(base) %>/brand/logo-login.svg" alt="OdontoSync - Gestión Dental Digital" width="260" height="65">
      <p class="muted login-subtitle">Sistema de gestión para clínicas odontológicas</p>
      <div class="divider"></div>
      <h1>Iniciar sesión</h1>
      <% if (error != null) { %><p class="alert" role="alert"><%= Html.escape(error) %></p><% } %>
      <form action="<%= Html.escape(base) %>/login" method="post">
        <label for="correo">Correo electrónico</label>
        <input id="correo" name="correo" type="email" autocomplete="username" required maxlength="150" placeholder="usuario@odontosync.pe">
        <label for="clave">Contraseña</label>
        <input id="clave" name="clave" type="password" autocomplete="current-password" required>
        <button class="primary" type="submit">Ingresar</button>
      </form>
      <p class="login-note">El rol se reconoce a partir de tu cuenta.</p>
      <div class="login-info">
        <strong>ACCESO SEGURO</strong>
        <span>Ingresa con la cuenta registrada en tu clínica. El sistema mostrará las opciones de tu perfil.</span>
      </div>
    </section>
  </main>
</body>
</html>
