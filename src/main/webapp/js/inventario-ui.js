document.addEventListener("DOMContentLoaded", () => {
  const itemDialog = document.querySelector("[data-inventory-item-dialog]");
  const movementDialog = document.querySelector("[data-inventory-movement-dialog]");
  const itemButtons = document.querySelectorAll("[data-open-inventory-item]");
  const movementButtons = document.querySelectorAll("[data-open-inventory-movement]");

  const openDialog = (dialog) => {
    if (dialog && !dialog.open) dialog.showModal();
  };

  itemButtons.forEach((button) => {
    button.addEventListener("click", () => openDialog(itemDialog));
  });

  movementButtons.forEach((button) => {
    button.addEventListener("click", () => openDialog(movementDialog));
  });

  [itemDialog, movementDialog].forEach((dialog) => {
    if (!dialog) return;

    if (dialog.dataset.autoOpen === "true") {
      openDialog(dialog);
    }

    dialog.addEventListener("click", (event) => {
      const box = dialog.getBoundingClientRect();
      const inside = event.clientX >= box.left
        && event.clientX <= box.right
        && event.clientY >= box.top
        && event.clientY <= box.bottom;

      if (!inside) dialog.close();
    });
  });

  const tabs = Array.from(document.querySelectorAll("[data-inventory-tab]"));
  const panels = Array.from(document.querySelectorAll("[data-inventory-panel]"));

  const activateTab = (name, updateHash) => {
    tabs.forEach((tab) => {
      const active = tab.dataset.inventoryTab === name;
      tab.classList.toggle("active", active);
      tab.setAttribute("aria-selected", String(active));
    });

    panels.forEach((panel) => {
      panel.hidden = panel.dataset.inventoryPanel !== name;
    });

    if (updateHash) {
      const hash = name === "historial" ? "#historial" : "#existencias";
      history.replaceState(null, "", hash);
    }
  };

  tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      activateTab(tab.dataset.inventoryTab, true);
    });
  });

  if (window.location.hash === "#historial") {
    activateTab("historial", false);
  } else {
    activateTab("existencias", false);
  }
});
