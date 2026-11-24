(() => {
  'use strict';

  const dialogs = Array.from(
    document.querySelectorAll('[data-clinical-dialog]')
  );

  const findDialog = (name) =>
    dialogs.find((dialog) => dialog.dataset.clinicalDialog === name);

  document.querySelectorAll('[data-open-clinical-dialog]').forEach((button) => {
    button.addEventListener('click', () => {
      const dialog = findDialog(button.dataset.openClinicalDialog);
      if (dialog && !dialog.open) dialog.showModal();
    });
  });

  document.querySelectorAll('[data-close-clinical-dialog]').forEach((button) => {
    button.addEventListener('click', () => {
      const dialog = button.closest('dialog');
      if (dialog?.open) dialog.close();
    });
  });

  dialogs.forEach((dialog) => {
    dialog.addEventListener('click', (event) => {
      const box = dialog.getBoundingClientRect();
      const inside = event.clientX >= box.left
        && event.clientX <= box.right
        && event.clientY >= box.top
        && event.clientY <= box.bottom;

      if (!inside && dialog.open) dialog.close();
    });
  });


  const recipeViewDialog = document.querySelector('[data-recipe-view-dialog]');
  const recipeViewId = recipeViewDialog?.querySelector('[data-recipe-view-id]');
  const recipeViewDate = recipeViewDialog?.querySelector('[data-recipe-view-date]');
  const recipeViewMedications = recipeViewDialog?.querySelector('[data-recipe-view-medications]');
  const recipeViewIndications = recipeViewDialog?.querySelector('[data-recipe-view-indications]');
  const recipeViewPdf = recipeViewDialog?.querySelector('[data-recipe-view-pdf]');
  const recipeViewWhatsapp = recipeViewDialog?.querySelector('[data-recipe-view-whatsapp]');

  document.querySelectorAll('[data-open-recipe-view]').forEach((button) => {
    button.addEventListener('click', () => {
      if (!recipeViewDialog) return;

      if (recipeViewId) recipeViewId.textContent = button.dataset.recipeId || '';
      if (recipeViewDate) recipeViewDate.textContent = button.dataset.recipeDate || '';
      if (recipeViewMedications) {
        recipeViewMedications.textContent = button.dataset.recipeMedications || '—';
      }
      if (recipeViewIndications) {
        recipeViewIndications.textContent = button.dataset.recipeIndications || '—';
      }

      if (recipeViewPdf) {
        recipeViewPdf.href = button.dataset.recipePdf || '#';
      }

      if (recipeViewWhatsapp) {
        const whatsappUrl = button.dataset.recipeWhatsapp || '';
        recipeViewWhatsapp.href = whatsappUrl || '#';
        recipeViewWhatsapp.dataset.pdfUrl = button.dataset.recipePdf || '';
        recipeViewWhatsapp.hidden = !whatsappUrl;
      }

      if (!recipeViewDialog.open) recipeViewDialog.showModal();
    });
  });

  document.querySelectorAll('[data-close-recipe-view]').forEach((button) => {
    button.addEventListener('click', () => {
      if (recipeViewDialog?.open) recipeViewDialog.close();
    });
  });

  if (recipeViewDialog) {
    recipeViewDialog.addEventListener('click', (event) => {
      const box = recipeViewDialog.getBoundingClientRect();
      const inside = event.clientX >= box.left
        && event.clientX <= box.right
        && event.clientY >= box.top
        && event.clientY <= box.bottom;

      if (!inside && recipeViewDialog.open) recipeViewDialog.close();
    });
  }

  if (recipeViewWhatsapp) {
    recipeViewWhatsapp.addEventListener('click', (event) => {
      if (!recipeViewWhatsapp.href || recipeViewWhatsapp.hidden) return;

      event.preventDefault();

      const whatsappUrl = recipeViewWhatsapp.href;
      const pdfUrl = recipeViewWhatsapp.dataset.pdfUrl;
      const whatsappWindow = window.open('about:blank', '_blank');

      if (pdfUrl) {
        const download = document.createElement('a');
        download.href = pdfUrl;
        download.hidden = true;
        document.body.appendChild(download);
        download.click();
        download.remove();
      }

      if (whatsappWindow) {
        try {
          whatsappWindow.opener = null;
        } catch (_) {
          // No afecta el flujo si el navegador restringe opener.
        }
        whatsappWindow.location.href = whatsappUrl;
      } else {
        window.location.href = whatsappUrl;
      }
    });
  }
})();
