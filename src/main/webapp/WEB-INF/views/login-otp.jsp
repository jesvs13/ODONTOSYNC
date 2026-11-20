<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.odontosync.Html" %>
<%
String error = (String) request.getAttribute("error");
String success = (String) request.getAttribute("success");
String csrf = (String) request.getAttribute("csrf");
String maskedEmail = (String) request.getAttribute("maskedEmail");
String base = request.getContextPath();
%>
<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Verificar inicio de sesión | OdontoSync</title>
  <link rel="icon" type="image/svg+xml" href="<%= Html.escape(base) %>/brand/favicon.svg">
  <link rel="icon" type="image/png" sizes="32x32" href="<%= Html.escape(base) %>/brand/icon-32.png">
  <link rel="icon" type="image/png" sizes="16x16" href="<%= Html.escape(base) %>/brand/icon-16.png">
  <link rel="stylesheet" href="<%= Html.escape(base) %>/css/estilos.css?v=20261120-login-otp">
</head>
<body>
  <main class="login-screen">
    <section class="login-card">
      <img class="brand-login"
           src="<%= Html.escape(base) %>/brand/logo-login.svg"
           alt="OdontoSync - Gestión Dental Digital"
           width="260"
           height="65">
      <p class="muted login-subtitle">Verificación de seguridad</p>
      <div class="divider"></div>

      <h1>Ingresa tu código</h1>
      <p class="muted">
        Enviamos un código de 6 dígitos a
        <strong><%= Html.escape(maskedEmail) %></strong>.
        Vence en 5 minutos.
      </p>

      <% if (error != null) { %>
        <p class="alert danger" role="alert"><%= Html.escape(error) %></p>
      <% } %>
      <% if (success != null) { %>
        <p class="alert success" role="status"><%= Html.escape(success) %></p>
      <% } %>

      <form action="<%= Html.escape(base) %>/verificar-login" method="post">
        <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">

        <label for="codigo">Código de seguridad</label>
        <input id="codigo"
               name="codigo"
               type="text"
               inputmode="numeric"
               autocomplete="one-time-code"
               pattern="[0-9]{6}"
               minlength="6"
               maxlength="6"
               required
               autofocus
               placeholder="000000">

        <button class="primary" type="submit">Verificar e ingresar</button>
      </form>

      <form action="<%= Html.escape(base) %>/verificar-login" method="post">
        <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
        <input type="hidden" name="accion" value="reenviar">
        <button class="btn secondary" type="submit">Reenviar código</button>
      </form>

      <p class="login-note">
        Nunca compartas este código. OdontoSync no te lo solicitará por teléfono.
      </p>
    </section>
  </main>
</body>
</html>
