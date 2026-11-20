(() => {
  "use strict";

  const ChartJs = window.Chart;
  const root = document;

  const palette = [
    "#0e7490",
    "#14b8a6",
    "#2563eb",
    "#7c3aed",
    "#f59e0b",
    "#ef4444",
    "#64748b"
  ];

  const reducedMotion = window.matchMedia
    && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const formatNumber = (value, prefix = "") => {
    const number = Number(value);
    if (!Number.isFinite(number)) return prefix + "0";
    const options = Math.abs(number) >= 1000
      ? { maximumFractionDigits: 0 }
      : { maximumFractionDigits: 2 };
    return prefix + new Intl.NumberFormat("es-PE", options).format(number);
  };

  const chartData = (canvas) => {
    const list = canvas.closest(".analytics-chart-card")
      ?.querySelector(".analytics-chart-data");

    if (!list) return [];

    return [...list.querySelectorAll("li[data-label][data-value]")]
      .map((item) => ({
        label: item.dataset.label || "",
        value: Number(item.dataset.value || 0)
      }))
      .filter((item) => Number.isFinite(item.value));
  };

  const showLibraryError = () => {
    root.querySelectorAll(".analytics-chart-wrap").forEach((wrapper) => {
      const message = document.createElement("p");
      message.className = "analytics-chart-error";
      message.textContent = "No se pudo cargar la librería de gráficos.";
      wrapper.replaceChildren(message);
    });
  };

  if (!ChartJs) {
    showLibraryError();
    return;
  }

  ChartJs.defaults.font.family =
    'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
  ChartJs.defaults.color = "#536b78";
  ChartJs.defaults.borderColor = "#e6eef2";

  const emptyStatePlugin = {
    id: "odontosyncEmptyState",
    afterDraw(chart, _args, options) {
      if (!options?.enabled) return;

      const { ctx, chartArea } = chart;
      if (!chartArea) return;

      ctx.save();
      ctx.fillStyle = "#718795";
      ctx.font = "12px system-ui, sans-serif";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText(
        "No hay datos para el período seleccionado.",
        (chartArea.left + chartArea.right) / 2,
        (chartArea.top + chartArea.bottom) / 2
      );
      ctx.restore();
    }
  };

  ChartJs.register(emptyStatePlugin);

  const baseOptions = (canvas, hasData) => {
    const prefix = canvas.dataset.valuePrefix || "";

    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: reducedMotion ? false : { duration: 550 },
      interaction: {
        mode: "nearest",
        intersect: false
      },
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          callbacks: {
            label(context) {
              const value = context.parsed?.y
                ?? context.parsed?.x
                ?? context.parsed
                ?? context.raw
                ?? 0;
              const label = context.label || context.dataset.label || "";
              return `${label ? label + ": " : ""}${formatNumber(value, prefix)}`;
            }
          }
        },
        odontosyncEmptyState: {
          enabled: !hasData
        }
      }
    };
  };

  const createBar = (canvas, data) => {
    const horizontal = canvas.dataset.indexAxis === "y";
    const hasData = data.some((item) => item.value > 0);
    const options = baseOptions(canvas, hasData);

    options.indexAxis = horizontal ? "y" : "x";
    options.scales = {
      x: {
        beginAtZero: true,
        grid: {
          display: horizontal,
          color: "#edf2f4"
        },
        ticks: {
          maxRotation: horizontal ? 0 : 45,
          minRotation: 0,
          font: { size: 10 }
        }
      },
      y: {
        beginAtZero: true,
        grid: {
          display: !horizontal,
          color: "#edf2f4"
        },
        ticks: {
          font: { size: 10 }
        }
      }
    };

    return new ChartJs(canvas, {
      type: "bar",
      data: {
        labels: data.map((item) => item.label),
        datasets: [{
          label: canvas.getAttribute("aria-label") || "Datos",
          data: data.map((item) => item.value),
          backgroundColor: data.map(
            (_item, index) => palette[index % palette.length]
          ),
          borderWidth: 0,
          borderRadius: 6,
          borderSkipped: false,
          maxBarThickness: 42
        }]
      },
      options
    });
  };

  const createLine = (canvas, data) => {
    const hasData = data.some((item) => item.value > 0);
    const options = baseOptions(canvas, hasData);

    options.scales = {
      x: {
        grid: { display: false },
        ticks: {
          maxRotation: 45,
          minRotation: 0,
          font: { size: 10 }
        }
      },
      y: {
        beginAtZero: true,
        grid: { color: "#edf2f4" },
        ticks: { font: { size: 10 } }
      }
    };

    return new ChartJs(canvas, {
      type: "line",
      data: {
        labels: data.map((item) => item.label),
        datasets: [{
          label: canvas.getAttribute("aria-label") || "Datos",
          data: data.map((item) => item.value),
          borderColor: palette[0],
          backgroundColor: "rgba(14, 116, 144, 0.12)",
          pointBackgroundColor: palette[1],
          pointBorderColor: "#ffffff",
          pointBorderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6,
          borderWidth: 2.5,
          tension: 0.35,
          fill: true
        }]
      },
      options
    });
  };

  const createCircular = (canvas, data, type) => {
    const hasData = data.some((item) => item.value > 0);
    const options = baseOptions(canvas, hasData);

    options.cutout = type === "doughnut" ? "62%" : undefined;
    options.plugins.tooltip.callbacks.label = (context) => {
      const prefix = canvas.dataset.valuePrefix || "";
      const total = context.dataset.data
        .reduce((sum, value) => sum + Number(value || 0), 0);
      const value = Number(context.raw || 0);
      const percent = total > 0 ? (value * 100 / total) : 0;
      return `${context.label}: ${formatNumber(value, prefix)} (${percent.toFixed(1)}%)`;
    };

    return new ChartJs(canvas, {
      type,
      data: {
        labels: data.map((item) => item.label),
        datasets: [{
          data: data.map((item) => item.value),
          backgroundColor: data.map(
            (_item, index) => palette[index % palette.length]
          ),
          borderColor: "#ffffff",
          borderWidth: 2,
          hoverOffset: 7
        }]
      },
      options
    });
  };

  const charts = [];

  root.querySelectorAll("canvas.analytics-chart").forEach((canvas) => {
    const data = chartData(canvas);
    const type = canvas.dataset.chartType;

    if (type === "line") {
      charts.push(createLine(canvas, data));
    } else if (type === "pie") {
      charts.push(createCircular(canvas, data, "pie"));
    } else if (type === "donut") {
      charts.push(createCircular(canvas, data, "doughnut"));
    } else {
      charts.push(createBar(canvas, data));
    }
  });

  const period = root.getElementById("dashboardPeriod");
  const dateFields = [...root.querySelectorAll(".analytics-date-field")];

  const syncDates = () => {
    const custom = period && period.value === "custom";

    dateFields.forEach((field) => {
      field.hidden = !custom;
      const input = field.querySelector("input");
      if (input) input.disabled = !custom;
    });
  };

  if (period) {
    period.addEventListener("change", syncDates);
    syncDates();
  }

  window.addEventListener("pagehide", () => {
    charts.forEach((chart) => chart.destroy());
  }, { once: true });
})();
