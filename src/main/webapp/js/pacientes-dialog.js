document.addEventListener("DOMContentLoaded", () => {
  const dialog = document.querySelector("[data-patient-dialog]");
  const openButton = document.querySelector("[data-open-patient-dialog]");

  if (!dialog) return;

  if (openButton) {
    openButton.addEventListener("click", () => {
      if (!dialog.open) dialog.showModal();
    });
  }

  if (dialog.dataset.autoOpen === "true" && !dialog.open) {
    dialog.showModal();
  }

  dialog.addEventListener("click", (event) => {
    const box = dialog.getBoundingClientRect();
    const inside = event.clientX >= box.left && event.clientX <= box.right
      && event.clientY >= box.top && event.clientY <= box.bottom;
    if (!inside) dialog.close();
  });
});
