(() => {
  "use strict";

  const whatsapp = document.getElementById("whatsappRecipe");
  if (!whatsapp) return;

  whatsapp.addEventListener("click", (event) => {
    event.preventDefault();

    const whatsappUrl = whatsapp.href;
    const pdfUrl = whatsapp.dataset.pdfUrl;

    // Reserva la pestaña de WhatsApp dentro del mismo gesto del usuario.
    const whatsappWindow = window.open("about:blank", "_blank");

    // Descarga el mismo PDF institucional que usa el botón "Descargar PDF".
    if (pdfUrl) {
      const download = document.createElement("a");
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
})();
