(() => {
  'use strict';

  const dialog = document.querySelector('[data-user-dialog]');
  const openButtons = document.querySelectorAll('[data-open-user-dialog]');

  function openDialog() {
    if (dialog && !dialog.open) dialog.showModal();
  }

  openButtons.forEach((button) => {
    button.addEventListener('click', openDialog);
  });

  if (dialog?.dataset?.autoOpen === 'true') {
    openDialog();
  }

  if (dialog) {
    dialog.addEventListener('click', (event) => {
      const box = dialog.getBoundingClientRect();
      const inside = event.clientX >= box.left
        && event.clientX <= box.right
        && event.clientY >= box.top
        && event.clientY <= box.bottom;

      if (!inside) dialog.close();
    });
  }

  const confirmDialog = document.querySelector('[data-reset-confirm]');
  const resultDialog = document.querySelector('[data-reset-result]');
  if (confirmDialog && resultDialog) {
    const submit = confirmDialog.querySelector('[data-reset-submit]');
    const cancel = confirmDialog.querySelector('[data-reset-cancel]');
    const error = confirmDialog.querySelector('[data-reset-error]');
    const reload = confirmDialog.querySelector('[data-reset-reload]');
    const email = resultDialog.querySelector('[data-result-email]');
    const password = resultDialog.querySelector('[data-result-password]');
    const copyStatus = resultDialog.querySelector('[data-reset-copy-status]');
    let selectedForm = null;
    let opener = null;
    let busy = false;
    let reloadRequired = false;
    const clearResult = () => {
      email.value = '';
      password.value = '';
      copyStatus.textContent = '';
    };
    const restoreFocus = () => { if (opener?.isConnected && !opener.disabled) opener.focus(); };
    const setBusy = (value) => {
      busy = value;
      submit.disabled = value || reloadRequired;
      cancel.disabled = value;
      submit.textContent = value ? 'Restableciendo…' : 'Confirmar restablecimiento';
      confirmDialog.setAttribute('aria-busy', String(value));
    };
    const fail = (message) => {
      reloadRequired = true;
      error.textContent = message + ' Actualiza el listado antes de volver a intentarlo.';
      error.hidden = false;
      reload.hidden = false;
      submit.disabled = true;
      document.querySelectorAll('[data-user-reset-form] button').forEach(button => { button.disabled = true; });
      reload.focus();
    };
    document.querySelectorAll('[data-user-reset-form]').forEach(form => {
      form.addEventListener('submit', event => {
        event.preventDefault();
        if (busy || reloadRequired || confirmDialog.open || resultDialog.open) return;
        selectedForm = form;
        opener = event.submitter || form.querySelector('button');
        confirmDialog.querySelector('[data-reset-name]').textContent = form.dataset.userName;
        confirmDialog.querySelector('[data-reset-email]').textContent = form.dataset.userEmail;
        error.hidden = true;
        reload.hidden = true;
        confirmDialog.showModal();
        cancel.focus();
      });
    });
    cancel.addEventListener('click', () => { if (!busy) confirmDialog.close(); });
    confirmDialog.addEventListener('cancel', event => { if (busy) event.preventDefault(); });
    confirmDialog.addEventListener('close', () => {
      selectedForm = null;
      if (!resultDialog.open) restoreFocus();
    });
    submit.addEventListener('click', async () => {
      if (busy || reloadRequired || !selectedForm) return;
      const form = selectedForm;
      const body = new URLSearchParams(new FormData(form));
      body.set('respuesta', 'modal');
      setBusy(true);
      try {
        const response = await fetch(form.action, {
          method: 'POST', body, credentials: 'same-origin', cache: 'no-store', redirect: 'error'
        });
        if (response.headers.get('X-OdontoSync-Reset') !== '1') {
          throw new Error('Respuesta no reconocida');
        }
        const fields = new URLSearchParams(await response.text());
        if (!response.ok) {
          fail(fields.get('error') || 'No se pudo confirmar el restablecimiento.');
          return;
        }
        const nextNonce = fields.get('nonce');
        const nextEmail = fields.get('correo');
        const nextPassword = fields.get('clave');
        if (!nextNonce || !nextEmail || !nextPassword) throw new Error('Respuesta incompleta');
        document.querySelectorAll('input[name="alta_nonce"]').forEach(input => { input.value = nextNonce; });
        clearResult();
        email.value = nextEmail;
        password.value = nextPassword;
        confirmDialog.close();
        resultDialog.showModal();
        resultDialog.querySelector('[data-reset-done]').focus();
      } catch (_) {
        clearResult();
        fail('No se pudo confirmar el resultado. La contraseña podría haberse restablecido.');
      } finally {
        setBusy(false);
      }
    });
    resultDialog.querySelectorAll('[data-copy-reset]').forEach(button => {
      button.addEventListener('click', async () => {
        const input = button.dataset.copyReset === 'email' ? email : password;
        if (!input.value) return;
        try {
          await navigator.clipboard.writeText(input.value);
          if (resultDialog.open) copyStatus.textContent = 'Copiado al portapapeles.';
        } catch (_) {
          if (!resultDialog.open) return;
          input.focus();
          input.select();
          copyStatus.textContent = 'Texto seleccionado. Usa Ctrl+C o la opción Copiar de tu dispositivo.';
        }
      });
    });
    resultDialog.querySelector('[data-reset-done]').addEventListener('click', () => { clearResult(); resultDialog.close(); });
    resultDialog.addEventListener('cancel', () => clearResult());
    resultDialog.addEventListener('close', () => { clearResult(); restoreFocus(); });
    window.addEventListener('pagehide', () => { clearResult(); if (resultDialog.open) resultDialog.close(); });
  }

  document.querySelectorAll('[data-user-delete-form]').forEach((form) => {
    form.addEventListener('submit', (event) => {
      const confirmed = window.confirm(
        '¿Eliminar esta cuenta? Si tiene movimientos o pagos asociados, OdontoSync impedirá la eliminación y deberás cambiarla a INACTIVO.'
      );
      if (!confirmed) event.preventDefault();
    });
  });
})();
