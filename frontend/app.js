const form = document.getElementById("filter");
const result = document.getElementById("result");

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const params = new URLSearchParams(new FormData(form));

  const res = await fetch(
    "http://localhost:3000/report?" + params
  );

  // Expect JSON array of rows. Build a nicer HTML table with formatted columns.
  const data = await res.json();
  if (!Array.isArray(data)) {
    result.innerText = JSON.stringify(data, null, 2);
    return;
  }

  const numFmt = (v) => {
    if (v == null) return "";
    if (v === 0) return "0";
    return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(v);
  };

  const percentFmt = (v) => {
    if (!v || (v.planned == null && v.executed == null)) return "";
    const planned = v.planned;
    const executed = v.executed;
    if (planned == null || planned === 0) {
      if (!executed || executed === 0) return "0%";
      return "∞%";
    }
    const pct = (executed / planned) * 100;
    return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(pct) + "%";
  };

  const fmtDate = (v) => {
    if (!v) return "";
    try { return new Date(v).toISOString().split("T")[0]; } catch (e) { return v; }
  };

  const cols = [
    { key: "planned/date", header: "Date", fmt: fmtDate },
    { key: "planned/type", header: "Type" },
    { key: "planned/activity", header: "Activity" },
    { key: "planned/unit", header: "Unit" },
    { key: "planned", header: "Planned", fmt: numFmt },
    { key: "executed", header: "Executed", fmt: numFmt },
    { key: "%", header: "% Exec", compute: (r) => ({ planned: r.planned, executed: r.executed }), fmt: percentFmt },
  ];

  const table = document.createElement("table");
  table.style.borderCollapse = "collapse";
  table.style.width = "100%";

  const thead = table.createTHead();
  const hrow = thead.insertRow();
  cols.forEach((c) => {
    const th = document.createElement("th");
    th.innerText = c.header;
    th.style.border = "1px solid #ccc";
    th.style.padding = "6px 8px";
    th.style.textAlign = "left";
    hrow.appendChild(th);
  });

  const tbody = table.createTBody();
  data.forEach((r) => {
    const row = tbody.insertRow();
    cols.forEach((c) => {
      const cell = row.insertCell();
      const raw = c.compute ? c.compute(r) : r[c.key];
      const value = c.fmt ? c.fmt(raw) : (raw == null ? "" : raw);
      cell.innerText = value;
      cell.style.border = "1px solid #eee";
      cell.style.padding = "6px 8px";
    });
  });

  result.innerHTML = "";
  result.appendChild(table);
});

// Handle upload forms
const uploadPlanned = document.getElementById("upload-planned");
const uploadExecuted = document.getElementById("upload-executed");

uploadPlanned.addEventListener("submit", async (e) => {
  e.preventDefault();
  await handleUpload("http://localhost:3000/upload/planned", new FormData(uploadPlanned));
});

uploadExecuted.addEventListener("submit", async (e) => {
  e.preventDefault();
  await handleUpload("http://localhost:3000/upload/executed", new FormData(uploadExecuted));
});

async function handleUpload(url, formData) {
  console.log('Uploading to', url, 'with file', formData.get('file').name);
  try {
    const res = await fetch(url, {
      method: "POST",
      body: formData
    });
    const text = await res.text();
    console.log('Response:', text);
    alert(text);
    // Refresh the table after upload
    form.dispatchEvent(new Event("submit"));
  } catch (err) {
    console.error('Upload error:', err);
    alert("Erro no upload: " + err.message);
  }
}
