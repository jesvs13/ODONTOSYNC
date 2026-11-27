(() => {
  'use strict';
  // Validate before sending multipart data; the servlet/service still enforce all limits.
  const uploadForm = document.querySelector('[data-rx-upload-form]');
  if (uploadForm) {
    const input = uploadForm.querySelector('input[name="archivo"]');
    const error = uploadForm.querySelector('#rx-upload-error');
    const submit = uploadForm.querySelector('button[type="submit"]');
    const maxBytes = 10 * 1024 * 1024;
    const extensions = /\.(?:jpe?g|png|pdf)$/i;
    function validateUpload() {
      const file = input.files[0];
      let message = '';
      if (file) {
        if (file.size > maxBytes) {
          const size = (file.size / 1048576).toFixed(2);
          message = `El archivo pesa ${size} MB y supera el límite de 10 MB. Selecciona un archivo más pequeño.`;
        } else if (file.size === 0) {
          message = 'El archivo está vacío. Selecciona otro archivo.';
        } else if (!extensions.test(file.name)) {
          message = 'Formato no permitido. Selecciona un archivo JPG, JPEG, PNG o PDF.';
        }
      }
      input.setCustomValidity(message);
      if (message) input.setAttribute('aria-invalid', 'true');
      else input.removeAttribute('aria-invalid');
      error.textContent = message;
      error.hidden = !message;
      submit.disabled = Boolean(message);
      return !message;
    }
    input.addEventListener('change', validateUpload);
    uploadForm.addEventListener('submit', event => {
      if (!validateUpload()) {
        event.preventDefault();
        input.reportValidity();
      }
    });
    validateUpload();
  }

  const dialog = document.querySelector('[data-rx-view-dialog]');
  const annulDialog = document.querySelector('[data-rx-annul-dialog]');
  if (!dialog || !annulDialog) return;
  const image = dialog.querySelector('[data-rx-image]');
  const viewport = dialog.querySelector('[data-rx-viewport]');
  const stage = dialog.querySelector('[data-rx-stage]');
  const controls = dialog.querySelector('[data-rx-controls]');
  const message = dialog.querySelector('[data-rx-message]');
  const zoomText = dialog.querySelector('[data-rx-zoom]');
  const download = dialog.querySelector('[data-rx-download]');
  let zoom = 1, rotation = 0;

  function render() {
    if (!dialog.open || viewport.hidden || !image.naturalWidth) return;
    const swapped = rotation % 180 !== 0;
    const w = image.naturalWidth, h = image.naturalHeight;
    const fit = Math.min(viewport.clientWidth / (swapped ? h : w),
      viewport.clientHeight / (swapped ? w : h), 1);
    const width = w * fit * zoom, height = h * fit * zoom;
    stage.style.width = `${Math.max(viewport.clientWidth, swapped ? height : width)}px`;
    stage.style.height = `${Math.max(viewport.clientHeight, swapped ? width : height)}px`;
    image.style.width = `${width}px`;
    image.style.height = `${height}px`;
    image.style.transform = `translate(-50%, -50%) rotate(${rotation}deg)`;
    zoomText.textContent = `${Math.round(zoom * 100)} %`;
    viewport.scrollLeft = Math.max(0, (viewport.scrollWidth - viewport.clientWidth) / 2);
    viewport.scrollTop = Math.max(0, (viewport.scrollHeight - viewport.clientHeight) / 2);
  }
  function field(name, value) {
    dialog.querySelector(`[data-rx-field="${name}"]`).textContent = value || '—';
  }
  document.querySelectorAll('[data-rx-view]').forEach(button => {
    button.addEventListener('click', () => {
      const data = button.dataset;
      field('title', data.rxTitle); field('date', data.rxDate);
      field('region', data.rxRegion); field('notes', data.rxNotes);
      zoom = 1; rotation = 0;
      zoomText.textContent = '100 %';
      dialog.classList.remove('rx-expanded');
      dialog.querySelector('[data-rx-command="expand"]').textContent = 'Ampliar';
      download.href = data.rxUrl;
      const isImage = data.rxMime === 'image/png' || data.rxMime === 'image/jpeg';
      controls.hidden = !isImage; viewport.hidden = !isImage;
      image.removeAttribute('src');
      message.textContent = isImage ? 'Cargando imagen…' : 'Documento PDF. Descarga el archivo original para consultarlo.';
      dialog.showModal();
      if (isImage) image.src = `${data.rxUrl}&modo=ver`;
    });
  });
  image.addEventListener('load', () => { message.textContent = ''; render(); });
  image.addEventListener('error', () => { if (dialog.open && image.hasAttribute('src')) message.textContent = 'No se pudo cargar la imagen. Comprueba que el archivo siga disponible.'; });
  controls.addEventListener('click', event => {
    const button = event.target.closest('[data-rx-command]');
    if (!button) return;
    switch (button.dataset.rxCommand) {
      case 'in': zoom = Math.min(5, zoom * 1.25); break;
      case 'out': zoom = Math.max(0.5, zoom / 1.25); break;
      case 'rotate': rotation = (rotation + 90) % 360; break;
      case 'reset': zoom = 1; rotation = 0; break;
      case 'expand': button.textContent = dialog.classList.toggle('rx-expanded') ? 'Reducir' : 'Ampliar'; break;
    }
    render();
  });
  window.addEventListener('resize', render);
  dialog.addEventListener('close', () => { image.removeAttribute('src'); download.removeAttribute('href'); });
  document.querySelectorAll('[data-rx-annul]').forEach(button => {
    button.addEventListener('click', () => {
      annulDialog.querySelector('form').reset();
      annulDialog.querySelector('[data-rx-annul-id]').value = button.dataset.rxAnnul;
      annulDialog.showModal();
    });
  });
  document.querySelectorAll('[data-rx-close]').forEach(button => {
    button.addEventListener('click', () => button.closest('dialog').close());
  });
})();
