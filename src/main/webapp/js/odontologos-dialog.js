document.addEventListener("DOMContentLoaded", () => {
  const dialog = document.querySelector("[data-dentist-dialog]");
  const openButton = document.querySelector("[data-open-dentist-dialog]");

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


document.addEventListener("DOMContentLoaded", () => {
  const dialog = document.querySelector("[data-specialty-dialog]");
  if (!dialog) return;
  const editor = dialog.querySelector("[data-specialty-editor]");
  const name = editor.elements.namedItem("nombre_especialidad");
  const action = editor.elements.namedItem("accion");
  const id = editor.elements.namedItem("id_especialidad");
  const label = editor.querySelector("[data-specialty-editor-label]");
  const search = dialog.querySelector("[data-specialty-search]");
  const rows = [...dialog.querySelectorAll("[data-specialty-row]")];
  const empty = dialog.querySelector("[data-specialty-empty]");
  const normalize = value => value.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase().trim().replace(/\s+/g, " ");
  const filter = () => {
    const term = normalize(search.value);
    let count = 0;
    rows.forEach(row => {
      row.hidden = !normalize(row.dataset.specialtySearchText).includes(term);
      if (!row.hidden) count++;
    });
    empty.hidden = count !== 0;
  };
  const closeEditor = () => {
    editor.hidden = true;
    name.value = "";
    id.value = "";
    action.value = "crear-especialidad";
  };
  const openEditor = (editId = "", editName = "") => {
    action.value = editId ? "editar-especialidad" : "crear-especialidad";
    id.value = editId;
    label.textContent = editId ? "Editar especialidad" : "Nueva especialidad";
    name.value = editName;
    editor.hidden = false;
    name.focus();
  };
  document.querySelector("[data-open-specialty-dialog]")?.addEventListener("click", () => {
    if (!dialog.open) dialog.showModal();
  });
  dialog.querySelector("[data-close-specialty-dialog]").addEventListener("click", () => dialog.close());
  dialog.querySelector("[data-new-specialty]").addEventListener("click", () => openEditor());
  dialog.querySelector("[data-cancel-specialty]").addEventListener("click", closeEditor);
  dialog.querySelectorAll("[data-edit-specialty]").forEach(button => {
    button.addEventListener("click", () => openEditor(button.dataset.id, button.dataset.name));
  });
  search.addEventListener("input", filter);
  dialog.addEventListener("close", () => {
    closeEditor();
    search.value = "";
    filter();
    const url = new URL(window.location.href);
    if (url.searchParams.has("especialidades")) {
      url.searchParams.delete("especialidades");
      window.history.replaceState(window.history.state, "", url);
    }
  });
  // No se cierran los formularios por un clic accidental fuera del modal.
  if (dialog.dataset.autoOpen === "true" && !dialog.open) dialog.showModal();
});
