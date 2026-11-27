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

  const role = document.querySelector('[data-user-role]');
  const colegiaturaField = document.querySelector('[data-collegiatura-field]');
  const colegiaturaInput = colegiaturaField?.querySelector('input[name="colegiatura"]');

  function syncProfessionalField() {
    if (!role || !colegiaturaField || !colegiaturaInput) return;

    const isDentist = role.value === 'ODONTOLOGO';
    colegiaturaField.classList.toggle('users-field-muted', !isDentist);
    colegiaturaInput.required = isDentist;
    colegiaturaInput.setAttribute('aria-required', String(isDentist));
  }

  role?.addEventListener('change', syncProfessionalField);
  syncProfessionalField();

  const userForm = dialog?.querySelector('form.users-form');
  const firstSurname = userForm?.querySelector('[name="primer_apellido"]');
  if (firstSurname) {
    const names = userForm.querySelector('[name="nombres"]');
    const secondSurname = userForm.querySelector('[name="segundo_apellido"]');
    const email = userForm.querySelector('[data-institutional-email]');
    const status = userForm.querySelector('[data-email-status]');
    const submit = userForm.querySelector('button[type="submit"]');
    let timer;
    let revision = 0;
    let request;

    function refreshEmail() {
      const current = ++revision;
      clearTimeout(timer);
      request?.abort();
      email.value = '';
      submit.disabled = true;
      if (!names.value.trim() || !firstSurname.value.trim()) {
        status.textContent = 'Completa los nombres y el primer apellido.';
        return;
      }
      status.textContent = 'Consultando disponibilidad…';
      timer = setTimeout(async () => {
        request = new AbortController();
        const body = new URLSearchParams({
          accion: 'sugerir-correo',
          csrf: userForm.elements.csrf.value,
          nombres: names.value,
          primer_apellido: firstSurname.value,
          segundo_apellido: secondSurname.value
        });
        try {
          const response = await fetch(userForm.action, {
            method: 'POST', body, credentials: 'same-origin',
            signal: request.signal, cache: 'no-store'
          });
          const result = (await response.text()).trim();
          if (current !== revision) return;
          if (!response.ok || response.redirected || !/^[a-z0-9]+@odontosync\.pe$/.test(result)) {
            status.textContent = response.status === 400 ? result : 'No se pudo consultar el correo. Revisa la sesión e intenta nuevamente.';
            return;
          }
          email.value = result;
          status.textContent = 'Correo propuesto. Se confirmará al registrar la cuenta.';
          submit.disabled = false;
        } catch (error) {
          if (current === revision && error.name !== 'AbortError') {
            status.textContent = 'No se pudo consultar el correo. Intenta nuevamente.';
          }
        }
      }, 300);
    }
    [names, firstSurname, secondSurname].forEach(field => field.addEventListener('input', refreshEmail));
    userForm.addEventListener('submit', event => { if (!email.value) event.preventDefault(); });
    openButtons.forEach(button => button.addEventListener('click', refreshEmail));
    refreshEmail();
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
