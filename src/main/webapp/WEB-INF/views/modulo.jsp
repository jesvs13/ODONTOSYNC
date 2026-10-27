<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,pe.odontosync.TableView" %>
<%@ include file="layout-top.jspf" %>
<% TableView table = (TableView) request.getAttribute("table"); %>
<div class="page-heading">
  <div><h1><%= Html.escape(table.title()) %></h1><p><%= Html.escape(table.subtitle()) %></p></div>
</div>
<section class="table-card">
  <div class="table-top">
    <h2><%= Html.escape(table.title()) %></h2>
    <div class="table-actions">
      <span>Vista de consulta</span>
      <% if ("reportes".equals(active)) { %>
        <a class="btn primary" href="<%= Html.escape(base) %>/reportes/exportar-citas.xlsx">Exportar Excel</a>
      <% } %>
    </div>
  </div>
  <div class="table-scroll">
    <table>
      <thead><tr>
      <% for (String header : table.headers()) { %><th scope="col"><%= Html.escape(header) %></th><% } %>
      </tr></thead>
      <tbody>
      <% for (List<String> row : table.rows()) { %>
        <tr><% for (String value : row) { %><td><%= Html.escape(value) %></td><% } %></tr>
      <% } %>
      <% if (table.rows().isEmpty()) { %>
        <tr><td colspan="<%= table.headers().size() %>" class="empty">No hay registros para mostrar.</td></tr>
      <% } %>
      </tbody>
    </table>
  </div>
</section>
<p class="read-only">
  <% if ("reportes".equals(active)) { %>
    Datos consultados en MySQL. El archivo XLSX se genera en Java con Apache POI y la descarga queda registrada en el log de auditoría.
  <% } else { %>
    Datos consultados en MySQL.
  <% } %>
</p>
<%@ include file="layout-bottom.jspf" %>
