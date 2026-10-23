<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,pe.odontosync.Card" %>
<%@ include file="layout-top.jspf" %>
<%
@SuppressWarnings("unchecked")
List<Card> cards = (List<Card>) request.getAttribute("cards");
%>
<div class="page-heading">
  <div><h1><%= Html.escape(user.role().heading()) %></h1><p><%= Html.escape(user.role().subtitle()) %></p></div>
  <span class="code-chip"><%= Html.escape(user.role().code()) %></span>
</div>
<section class="welcome">
  <img class="welcome-icon" src="<%= Html.escape(base) %>/brand/icon-48.png" width="48" height="48" alt="" aria-hidden="true">
  <div><h2><%= Html.escape(user.role().welcome()) %></h2><p><%= Html.escape(user.role().detail()) %></p></div>
</section>
<div class="metric-grid">
  <% for (Card card : cards) { if (user.role().allows(card.link())) { %>
    <a class="metric-card" href="<%= Html.escape(base) %>/modulo/<%= Html.escape(card.link()) %>">
      <span class="metric-chip"><%= Html.escape(card.label()) %></span>
      <strong><%= Html.escape(card.value()) %></strong>
      <span class="card-link">Abrir módulo →</span>
    </a>
  <% } } %>
</div>
<%@ include file="layout-bottom.jspf" %>
