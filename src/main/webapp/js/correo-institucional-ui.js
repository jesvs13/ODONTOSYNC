(() => {
  'use strict';
  const userForm = document.querySelector('[data-institutional-form]');
  const openButtons = document.querySelectorAll('[data-open-user-dialog], [data-open-dentist-dialog]');
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

})();
