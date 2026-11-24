(() => {
  'use strict';

  const dialog = document.querySelector('[data-payment-dialog]');
  const openButtons = document.querySelectorAll('[data-open-payment-dialog]');
  const closeButtons = document.querySelectorAll('[data-close-payment-dialog]');

  const openDialog = () => {
    if (dialog && !dialog.open) dialog.showModal();
  };

  const closeDialog = () => {
    if (dialog?.open) dialog.close();
  };

  openButtons.forEach((button) => {
    button.addEventListener('click', openDialog);
  });

  closeButtons.forEach((button) => {
    button.addEventListener('click', closeDialog);
  });

  if (!dialog) return;

  dialog.addEventListener('click', (event) => {
    const box = dialog.getBoundingClientRect();
    const inside = event.clientX >= box.left
      && event.clientX <= box.right
      && event.clientY >= box.top
      && event.clientY <= box.bottom;

    if (!inside) closeDialog();
  });
})();
