<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,pe.odontosync.Role,pe.odontosync.model.UsuarioDetalle" %>
<%@ include file="layout-top.jspf" %>
<link rel="stylesheet" href="<%= Html.escape(base) %>/css/usuarios-ui.css?v=20261127-issue35-2">
<%
List<UsuarioDetalle> users = (List<UsuarioDetalle>) request.getAttribute("usuarios");
UsuarioDetalle edit = (UsuarioDetalle) request.getAttribute("editUser");
String success = (String) request.getAttribute("success");
String error = (String) request.getAttribute("error");
boolean editing = edit != null;
boolean professional = editing && (edit.role() == Role.ODONTOLOGO || edit.numeroColegiatura() != null);

int totalUsers = users == null ? 0 : users.size();
int activeUsers = 0;
int inactiveUsers = 0;
int blockedUsers = 0;

if (users != null) {
    for (UsuarioDetalle item : users) {
        if ("ACTIVO".equals(item.estado())) activeUsers++;
        else if ("INACTIVO".equals(item.estado())) inactiveUsers++;
        else if ("BLOQUEADO".equals(item.estado())) blockedUsers++;
    }
}
%>

<div class="page-heading users-page-heading">
  <div>
    <h1>Usuarios y roles</h1>
    <p>Administra las cuentas que pueden ingresar a OdontoSync.</p>
  </div>
  <div class="users-heading-actions">
    <% if (editing) { %>
      <a class="btn primary" href="<%= Html.escape(base) %>/usuarios">+ Nuevo usuario</a>
    <% } else { %>
      <button class="btn primary" type="button" data-open-user-dialog>+ Nuevo usuario</button>
    <% } %>
  </div>
</div>

<% if (success != null) { %><div class="alert success"><%= Html.escape(success) %></div><% } %>
<% if (error != null) { %><div class="alert danger"><%= Html.escape(error) %></div><% } %>

<section class="users-kpis" aria-label="Resumen de usuarios">
  <article>
    <span>Total de cuentas</span>
    <strong><%= totalUsers %></strong>
    <small>Usuarios registrados en esta clínica</small>
  </article>
  <article>
    <span>Activos</span>
    <strong><%= activeUsers %></strong>
    <small>Pueden iniciar sesión según sus permisos</small>
  </article>
  <article class="<%= inactiveUsers > 0 ? "users-kpi-muted" : "" %>">
    <span>Inactivos</span>
    <strong><%= inactiveUsers %></strong>
    <small>Cuentas deshabilitadas administrativamente</small>
  </article>
  <article class="<%= blockedUsers > 0 ? "users-kpi-warning" : "" %>">
    <span>Bloqueados</span>
    <strong><%= blockedUsers %></strong>
    <small>Requieren revisión del administrador</small>
  </article>
</section>

<section class="table-card crud-list users-list">
  <div class="table-top users-table-top">
    <div>
      <h2>Usuarios registrados</h2>
      <span><%= totalUsers %> cuenta(s) pertenecientes a <%= Html.escape(user.clinicName()) %></span>
    </div>
    <% if (!editing) { %>
      <button class="btn tiny primary" type="button" data-open-user-dialog>+ Nuevo usuario</button>
    <% } %>
  </div>

  <div class="table-scroll users-table-scroll">
    <table>
      <thead>
        <tr>
          <th>Usuario</th>
          <th>Contacto</th>
          <th>Rol</th>
          <th>Estado</th>
          <th>Colegiatura</th>
          <th>Acciones</th>
        </tr>
      </thead>
      <tbody>
      <% if (users != null) for (UsuarioDetalle item : users) { %>
        <tr class="<%= item.id() == user.id() ? "users-current-row" : "" %>">
          <td>
            <strong><%= Html.escape(item.nombres() + " " + item.apellidos()) %></strong>
            <% if (item.id() == user.id()) { %>
              <span class="users-current-badge">Cuenta actual</span>
            <% } %>
          </td>
          <td>
            <%= Html.escape(item.correo()) %>
            <% if (item.telefono() != null && !item.telefono().isBlank()) { %>
              <br><small class="table-secondary"><%= Html.escape(item.telefono()) %></small>
            <% } %>
          </td>
          <td><span class="status-pill users-role-pill"><%= Html.escape(item.role().label()) %></span></td>
          <td>
            <span class="status-pill users-status users-status-<%= item.estado().toLowerCase(java.util.Locale.ROOT) %>">
              <%= Html.escape(item.estado()) %>
            </span>
          </td>
          <td><%= Html.escape(item.numeroColegiatura() == null ? "—" : item.numeroColegiatura()) %></td>
          <td>
            <div class="row-actions users-row-actions">
              <a class="btn tiny secondary" href="<%= Html.escape(base) %>/usuarios?editar=<%= item.id() %>"><%= item.role() == Role.ODONTOLOGO || item.numeroColegiatura() != null ? "Gestionar acceso" : "Editar" %></a>
              <% if (item.id() != user.id()) { %>
              <form action="<%= Html.escape(base) %>/usuarios" method="post" data-user-reset-form
                    data-user-name="<%= Html.escape(item.nombres() + " " + item.apellidos()) %>"
                    data-user-email="<%= Html.escape(item.correo()) %>">
                <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
                <input type="hidden" name="accion" value="restablecer-password">
                <input type="hidden" name="id_usuario" value="<%= item.id() %>">
                <input type="hidden" name="alta_nonce" value="<%= Html.escape((String) request.getAttribute("altaNonce")) %>">
                <button class="btn tiny secondary" type="submit">Restablecer contraseña</button>
              </form>
              <% } %>
              <% if (item.id() != user.id() && item.role() != Role.ODONTOLOGO && item.numeroColegiatura() == null) { %>
              <form action="<%= Html.escape(base) %>/usuarios" method="post" data-user-delete-form>
                <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
                <input type="hidden" name="accion" value="eliminar">
                <input type="hidden" name="id_usuario" value="<%= item.id() %>">
                <button class="btn tiny danger-btn" type="submit">Eliminar</button>
              </form>
              <% } %>
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

<dialog class="crud-card crud-dialog users-dialog"
        data-user-dialog
        data-auto-open="<%= editing %>">
  <div class="crud-head users-dialog-head">
    <div>
      <span class="users-dialog-eyebrow">ACCESO Y ROLES</span>
      <h2><%= professional ? "Gestionar acceso" : editing ? "Editar usuario" : "Registrar usuario" %></h2>
      <p><%= editing
          ? (professional ? "Gestiona el estado de acceso del profesional." : "Modifica los datos de la cuenta seleccionada.")
          : "Crea una cuenta para administración o recepción." %></p>
    </div>
    <a class="btn secondary" href="<%= Html.escape(base) %>/usuarios">Cerrar</a>
  </div>

  <form class="crud-form users-form"
        action="<%= Html.escape(base) %>/usuarios"
        method="post"
        <%= editing ? "" : "data-institutional-form" %>
        autocomplete="off">
    <input type="hidden" name="csrf" value="<%= Html.escape(csrf) %>">
    <input type="hidden" name="accion" value="<%= editing ? "actualizar" : "crear" %>">
    <% if (editing) { %><input type="hidden" name="id_usuario" value="<%= edit.id() %>"><% } %>

    <label>Nombres
      <input name="nombres" maxlength="100" required <%= professional ? "readonly" : "" %>
             value="<%= Html.escape(editing ? edit.nombres() : "") %>">
    </label>

    <% if (editing) { %>
      <label>Apellidos
        <input name="apellidos" maxlength="100" required <%= professional ? "readonly" : "" %> value="<%= Html.escape(edit.apellidos()) %>">
      </label>
    <% } else { %>
      <label>Primer apellido
        <input name="primer_apellido" maxlength="100" required>
      </label>
      <label>Segundo apellido
        <input name="segundo_apellido" maxlength="100" placeholder="Opcional">
      </label>
    <% } %>

    <label class="span-2">Correo institucional
      <input type="email" name="correo" readonly data-institutional-email
             value="<%= Html.escape(editing ? edit.correo() : "") %>"
             placeholder="Se genera con los nombres y apellidos">
      <% if (editing) { %>
        <small>El correo de acceso se conserva al editar los datos.</small>
      <% } else { %>
        <small data-email-status role="status" aria-live="polite">Completa los nombres y apellidos.</small>
      <% } %>
    </label>

    <% if (!editing) { %>
      <input type="hidden" name="alta_nonce" value="<%= Html.escape((String) request.getAttribute("altaNonce")) %>">
      <label>DNI
        <input name="dni" type="text" inputmode="numeric" maxlength="8" minlength="8" pattern="[0-9]{8}" required autocomplete="off" placeholder="8 dígitos">
      </label>
    <% } %>
    <label>Teléfono
      <input name="telefono" maxlength="20" <%= professional ? "readonly" : "" %>
             value="<%= Html.escape(editing && edit.telefono() != null ? edit.telefono() : "") %>"
             placeholder="Opcional">
    </label>

    <label>Rol
      <select name="rol" required data-user-role>
        <% for (Role r : (professional ? List.of(edit.role()) : List.of(Role.ADMINISTRADOR, Role.RECEPCIONISTA))) {
             boolean selected = editing ? edit.role() == r : r == Role.RECEPCIONISTA; %>
          <option value="<%= r.name() %>" <%= selected ? "selected" : "" %>>
            <%= Html.escape(r.label()) %>
          </option>
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
      <% if (editing && edit.id() == user.id()) { %>
        <small>Tu propia cuenta debe conservar el rol Administrador y estado ACTIVO.</small>
      <% } %>
    </label>

    <% if (professional) { %>
      <div class="users-security-note span-2">
        <strong>Datos profesionales</strong>
        <span>Los datos personales, la colegiatura y las especialidades se gestionan desde <a href="<%= Html.escape(base) %>/odontologos">Odontólogos</a>. Para habilitar una cuenta, el perfil profesional debe estar activo.</span>
      </div>
    <% } %>

    <% if (editing) { %>
      <div class="span-2 read-only"><strong>Contraseña de acceso</strong>
        <% if (edit.id() == user.id()) { %>
          <p>Para modificar tu propia contraseña, usa <a href="<%= Html.escape(base) %>/cambiar-password">Cambiar contraseña</a>.</p>
        <% } else { %>
          <p>Para generar una nueva contraseña temporal, utiliza Restablecer contraseña en el listado. Guardar cambios no modifica la credencial.</p>
        <% } %>
      </div>
    <% } else { %>
      <div class="span-2 read-only"><strong>Contraseña temporal automática</strong><p>Se mostrará una sola vez al registrar la cuenta. El usuario deberá cambiarla después de verificar su correo.</p></div>
    <% } %>

    <div class="users-security-note span-2">
      <strong>Seguridad de la cuenta</strong>
      <span>Los permisos se determinan por rol y las contraseñas se almacenan mediante hash PBKDF2, nunca como texto plano.</span>
    </div>

    <div class="form-actions">
      <a class="btn secondary" href="<%= Html.escape(base) %>/usuarios">Cancelar</a>
      <button class="btn primary" type="submit"><%= editing ? "Guardar cambios" : "Crear usuario" %></button>
    </div>
  </form>
</dialog>

<dialog class="crud-card crud-dialog users-dialog users-reset-dialog" data-reset-confirm aria-labelledby="reset-confirm-title">
  <div class="users-dialog-head">
    <span class="users-dialog-eyebrow">SEGURIDAD DE CUENTAS</span>
    <h2 id="reset-confirm-title">Restablecer contraseña</h2>
    <p>Se generará una nueva contraseña temporal.</p>
  </div>
  <div class="users-reset-account">
    <strong data-reset-name></strong>
    <span data-reset-email></span>
  </div>
  <div class="users-reset-note">
    <strong>Esta acción revoca las credenciales anteriores.</strong>
    <p>Se invalidarán las sesiones y los códigos OTP pendientes. El usuario deberá cambiar su contraseña en el próximo ingreso. Su estado de acceso no se modificará.</p>
  </div>
  <p class="users-reset-error" data-reset-error role="alert" hidden></p>
  <div class="users-reset-actions">
    <button class="btn secondary" type="button" data-reset-cancel>Cancelar</button>
    <a class="btn secondary" href="<%= Html.escape(base) %>/usuarios" data-reset-reload hidden>Actualizar listado</a>
    <button class="btn primary" type="button" data-reset-submit>Confirmar restablecimiento</button>
  </div>
</dialog>

<dialog class="crud-card crud-dialog users-dialog users-reset-dialog" data-reset-result aria-labelledby="reset-result-title">
  <div class="users-dialog-head">
    <span class="users-dialog-eyebrow">SEGURIDAD DE CUENTAS</span>
    <h2 id="reset-result-title">Contraseña restablecida</h2>
    <p>Entrega las credenciales al titular por un canal seguro.</p>
  </div>
  <div class="alert success">La contraseña se restableció correctamente. Las credenciales anteriores ya no son válidas.</div>
  <div class="users-reset-fields">
    <label for="reset-result-email">Correo institucional</label>
    <div class="users-copy-row">
      <input id="reset-result-email" data-result-email readonly autocomplete="off" spellcheck="false">
      <button class="btn secondary" type="button" data-copy-reset="email">Copiar</button>
    </div>
    <label for="reset-result-password">Contraseña temporal</label>
    <div class="users-copy-row">
      <input id="reset-result-password" data-result-password readonly autocomplete="off" spellcheck="false">
      <button class="btn secondary" type="button" data-copy-reset="password">Copiar</button>
    </div>
  </div>
  <p class="users-reset-note">La contraseña solo se muestra en esta confirmación. Al cerrar se ocultará definitivamente. El usuario deberá validar el OTP y establecer una nueva contraseña.</p>
  <p data-reset-copy-status role="status" aria-live="polite"></p>
  <div class="users-reset-actions">
    <button class="btn primary" type="button" data-reset-done>Finalizar</button>
  </div>
</dialog>

<p class="read-only users-scope-note">
  Registra a los profesionales desde Odontólogos. Aquí se administran las cuentas de acceso de todos los roles de tu clínica.
</p>

<script src="<%= Html.escape(base) %>/js/usuarios-ui.js?v=20261127-issue35-2" defer></script>
<script src="<%= Html.escape(base) %>/js/correo-institucional-ui.js?v=20261127-1" defer></script>
<%@ include file="layout-bottom.jspf" %>
