<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="pe.odontosync.service.CreatedAccount" %>
<%@ include file="layout-top.jspf" %>
<%
CreatedAccount created = (CreatedAccount) request.getAttribute("createdAccount");
boolean reset = Boolean.TRUE.equals(request.getAttribute("credentialReset"));
%>
<div class="page-heading"><div><h1><%= reset ? "Contraseña restablecida" : "Cuenta registrada" %></h1><p>Entrega las credenciales al titular por un canal seguro.</p></div></div>
<section class="crud-card">
  <div class="alert success"><%= reset ? "La contraseña se restableció correctamente. Las credenciales anteriores ya no son válidas." : "La cuenta se registró correctamente." %></div>
  <p>La contraseña temporal solo se muestra en esta confirmación. No recargues la página ni compartas estas credenciales con otras personas.</p>
  <div class="crud-form">
    <label>Correo institucional
      <input type="text" readonly value="<%= Html.escape(created.correo()) %>" autocomplete="off">
    </label>
    <label>Contraseña temporal
      <input type="text" readonly value="<%= Html.escape(new String(created.temporaryPassword())) %>" autocomplete="off">
    </label>
  </div>
  <p>Cuando la cuenta esté activa, el usuario deberá validar el OTP y establecer una nueva contraseña.</p>
  <a class="btn primary" href="<%= Html.escape(base) %>/<%= Html.escape(active) %>">Volver al listado</a>
</section>
<%@ include file="layout-bottom.jspf" %>
