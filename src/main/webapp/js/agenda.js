(() => {
  'use strict';

  const dentist = document.getElementById('agenda-dentist');
  const specialty = document.getElementById('agenda-specialty');
  const help = document.getElementById('agenda-specialty-help');
  if (!dentist || !specialty) return;

  // Conservamos el catálogo completo que vino del servidor y mostramos
  // únicamente las especialidades asociadas al odontólogo seleccionado.
  const catalog = Array.from(specialty.options)
    .filter((option) => option.value)
    .map((option) => ({ value: option.value, label: option.textContent }));

  function syncSpecialties() {
    const previousValue = specialty.value;
    const selectedDentist = dentist.options[dentist.selectedIndex];
    const rawIds = selectedDentist?.dataset?.specialties || '';
    const allowed = new Set(rawIds.split(',').map((id) => id.trim()).filter(Boolean));

    specialty.replaceChildren();

    const emptyOption = document.createElement('option');
    emptyOption.value = '';
    emptyOption.textContent = 'Sin especificar';
    specialty.appendChild(emptyOption);

    for (const item of catalog) {
      if (!allowed.has(item.value)) continue;
      const option = document.createElement('option');
      option.value = item.value;
      option.textContent = item.label;
      specialty.appendChild(option);
    }

    if (previousValue && allowed.has(previousValue)) {
      specialty.value = previousValue;
    } else {
      specialty.value = '';
    }

    const hasDentist = Boolean(dentist.value);
    specialty.disabled = !hasDentist || allowed.size === 0;

    if (!help) return;
    if (!hasDentist) {
      help.textContent = 'Primero selecciona un odontólogo. Solo se mostrarán sus especialidades asignadas.';
    } else if (allowed.size === 0) {
      help.textContent = 'Este odontólogo no tiene especialidades asignadas. La cita se registrará sin especificar especialidad.';
    } else {
      help.textContent = `Especialidades disponibles para este odontólogo: ${allowed.size}.`;
    }
  }

  dentist.addEventListener('change', syncSpecialties);
  syncSpecialties();
})();
