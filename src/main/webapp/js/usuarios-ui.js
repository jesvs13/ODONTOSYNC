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

  document.querySelectorAll('[data-user-delete-form]').forEach((form) => {
    form.addEventListener('submit', (event) => {
      const confirmed = window.confirm(
        '¿Eliminar esta cuenta? Si tiene movimientos o pagos asociados, OdontoSync impedirá la eliminación y deberás cambiarla a INACTIVO.'
      );
      if (!confirmed) event.preventDefault();
    });
  });
})();
