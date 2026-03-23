/* global XLSX, html2pdf */

function buildReport(productName) {
  const name = String(productName || "").trim() || "未命名产品";
  const key = matchTemplateKey(name);
  const tpl = getTemplate(key);
  return {
    productName: name,
    templateKey: key,
    generatedAt: new Date(),
    ...tpl,
  };
}

function formatDate(d) {
  const x = d instanceof Date ? d : new Date(d);
  const p = (n) => String(n).padStart(2, "0");
  return `${x.getFullYear()}-${p(x.getMonth() + 1)}-${p(x.getDate())} ${p(x.getHours())}:${p(x.getMinutes())}`;
}

function renderFlow(container, report) {
  container.innerHTML = "";
  const track = document.createElement("div");
  track.className = "flow-track";
  report.flow.forEach((step, i) => {
    const el = document.createElement("div");
    el.className = "flow-step";
    el.innerHTML = `
      <span class="step-idx">${i + 1}</span>
      <h3>${escapeHtml(step.title)}</h3>
      <p>${escapeHtml(step.desc)}</p>
      <span class="badge-green">${escapeHtml(step.green)}</span>
    `;
    track.appendChild(el);
    if (i < report.flow.length - 1) {
      const arr = document.createElement("div");
      arr.className = "flow-arrow";
      arr.setAttribute("aria-hidden", "true");
      arr.textContent = "→";
      track.appendChild(arr);
    }
  });
  container.appendChild(track);
}

function escapeHtml(s) {
  const d = document.createElement("div");
  d.textContent = s;
  return d.innerHTML;
}

function renderReportUI(report) {
  const flowEl = document.getElementById("flow-container");
  const mainEl = document.getElementById("report-main");
  const pdfEl = document.getElementById("pdf-export-area");
  const placeholder = document.getElementById("placeholder");

  placeholder.style.display = "none";
  flowEl.closest(".panel").style.display = "block";
  mainEl.closest(".panel").style.display = "block";
  document.getElementById("export-buttons").style.display = "flex";

  renderFlow(flowEl, report);

  mainEl.innerHTML = `
    <div class="metrics-bar">
      ${report.kpis.map((k) => `
        <div class="metric">
          <div class="val">${escapeHtml(k.value)}</div>
          <div class="lbl">${escapeHtml(k.label)}</div>
          <div class="lbl" style="margin-top:4px;font-size:0.7rem;">${escapeHtml(k.hint)}</div>
        </div>`).join("")}
    </div>
    <p style="color:var(--muted);font-size:0.9rem;margin:0 0 1rem;">行业模板：<strong style="color:var(--accent);">${escapeHtml(report.industry)}</strong></p>
    <div class="report-grid">
      <div class="report-card">
        <h3>综合优化建议</h3>
        <ul>${report.recommendations.map((r) => `<li>${escapeHtml(r)}</li>`).join("")}</ul>
      </div>
      <div class="report-card">
        <h3>设备清单（节选）</h3>
        <table class="data-table">
          <thead><tr><th>设备</th><th>规格</th><th>数量</th></tr></thead>
          <tbody>
            ${report.equipment.slice(0, 8).map((e) => `<tr><td>${escapeHtml(e.name)}</td><td>${escapeHtml(e.spec)}</td><td>${escapeHtml(e.qty)}</td></tr>`).join("")}
          </tbody>
        </table>
      </div>
    </div>
    <div class="report-card" style="margin-top:1rem;">
      <h3>物料（BOM 摘要）</h3>
      <table class="data-table">
        <thead><tr><th>物料编码</th><th>名称</th><th>单位</th><th>用量说明</th><th>备注</th></tr></thead>
        <tbody>
          ${report.materials.map((m) => `<tr>
            <td>${escapeHtml(m.code)}</td>
            <td>${escapeHtml(m.name)}</td>
            <td>${escapeHtml(m.unit)}</td>
            <td>${escapeHtml(m.qty)}</td>
            <td>${escapeHtml(m.remark || "")}</td>
          </tr>`).join("")}
        </tbody>
      </table>
    </div>
    <div class="report-grid" style="margin-top:1rem;">
      <div class="report-card">
        <h3>工序与工位</h3>
        <table class="data-table">
          <thead><tr><th>序号</th><th>工序</th><th>工位</th><th>标准要点</th></tr></thead>
          <tbody>
            ${report.operations.map((o) => `<tr>
              <td>${escapeHtml(String(o.seq))}</td>
              <td>${escapeHtml(o.name)}</td>
              <td>${escapeHtml(o.workstation)}</td>
              <td>${escapeHtml(o.std)}</td>
            </tr>`).join("")}
          </tbody>
        </table>
      </div>
      <div class="report-card">
        <h3>工艺参数与控制</h3>
        <table class="data-table">
          <thead><tr><th>环节</th><th>参数</th><th>目标</th></tr></thead>
          <tbody>
            ${report.processParams.map((p) => `<tr>
              <td>${escapeHtml(p.item)}</td>
              <td>${escapeHtml(p.param)}</td>
              <td>${escapeHtml(p.target)}</td>
            </tr>`).join("")}
          </tbody>
        </table>
      </div>
    </div>
  `;

  const pdfTable = (headers, rows) => `
    <table class="data-table" style="font-size:0.72rem;margin-bottom:0.75rem;">
      <thead><tr>${headers.map((h) => `<th>${escapeHtml(h)}</th>`).join("")}</tr></thead>
      <tbody>${rows.map((cells) => `<tr>${cells.map((c) => `<td>${escapeHtml(c)}</td>`).join("")}</tr>`).join("")}</tbody>
    </table>`;

  pdfEl.innerHTML = `
    <div class="pdf-title">绿色制造 · 综合生产报告</div>
    <div class="pdf-meta">产品名称：${escapeHtml(report.productName)}　|　生成时间：${escapeHtml(formatDate(report.generatedAt))}　|　模板：${escapeHtml(report.industry)}</div>
    <p style="margin:0 0 0.75rem;font-size:0.88rem;">本报告基于行业通用绿色制造框架与典型工艺生成，供结构与合规参考；正式投产请结合工厂实际 BOM、设备台账与质量体系文件校核。</p>
    <h3 style="color:var(--accent);font-size:1rem;margin:1rem 0 0.5rem;">一、绿色生产流程概览</h3>
    <ol style="margin:0;padding-left:1.2rem;font-size:0.82rem;color:var(--muted);">
      ${report.flow.map((s, i) => `<li><strong style="color:var(--text);">${escapeHtml(s.title)}</strong> — ${escapeHtml(s.desc)}（${escapeHtml(s.green)}）</li>`).join("")}
    </ol>
    <h3 style="color:var(--accent);font-size:1rem;margin:1rem 0 0.5rem;">二、关键绩效与优化建议</h3>
    <ul style="margin:0 0 0.75rem;font-size:0.82rem;color:var(--muted);padding-left:1.2rem;">
      ${report.kpis.map((k) => `<li>${escapeHtml(k.label)}：${escapeHtml(k.value)}（${escapeHtml(k.hint)}）</li>`).join("")}
      ${report.recommendations.map((r) => `<li>${escapeHtml(r)}</li>`).join("")}
    </ul>
    <h3 style="color:var(--accent);font-size:1rem;margin:1rem 0 0.5rem;">三、设备清单</h3>
    ${pdfTable(
      ["设备名称", "规格", "数量", "备注"],
      report.equipment.map((e) => [e.name, e.spec, e.qty, e.note || ""])
    )}
    <h3 style="color:var(--accent);font-size:1rem;margin:0.5rem 0 0.5rem;">四、物料（BOM）</h3>
    ${pdfTable(
      ["编码", "名称", "单位", "用量", "备注"],
      report.materials.map((m) => [m.code, m.name, m.unit, m.qty, m.remark || ""])
    )}
    <h3 style="color:var(--accent);font-size:1rem;margin:0.5rem 0 0.5rem;">五、工序与工艺参数</h3>
    ${pdfTable(
      ["序号", "工序", "工位", "标准"],
      report.operations.map((o) => [String(o.seq), o.name, o.workstation, o.std])
    )}
    ${pdfTable(
      ["环节", "参数", "目标", "控制"],
      report.processParams.map((p) => [p.item, p.param, p.target, p.control])
    )}
    <p style="font-size:0.75rem;color:var(--muted);margin:0.5rem 0 0;">Excel 导出含「设备、物料、工序工艺、工艺参数、报告摘要、流程说明」等工作表，便于编辑与系统集成。</p>
  `;
}

let lastReport = null;

function runGenerate() {
  const input = document.getElementById("product-input");
  const name = input.value.trim();
  if (!name) {
    alert("请输入产品名称，例如：耳机");
    return;
  }
  lastReport = buildReport(name);
  renderReportUI(lastReport);
}

function exportExcel() {
  if (!lastReport || typeof XLSX === "undefined") {
    alert("请先生成报告，并确认网络可加载表格库。");
    return;
  }
  const r = lastReport;
  const wb = XLSX.utils.book_new();

  const sheetEquipment = XLSX.utils.json_to_sheet(
    r.equipment.map((e) => ({
      设备名称: e.name,
      规格型号: e.spec,
      数量: e.qty,
      备注: e.note || "",
    }))
  );
  XLSX.utils.book_append_sheet(wb, sheetEquipment, "设备");

  const sheetMat = XLSX.utils.json_to_sheet(
    r.materials.map((m) => ({
      物料编码: m.code,
      物料名称: m.name,
      单位: m.unit,
      用量: m.qty,
      供应商要求: m.supplier || "",
      备注: m.remark || "",
    }))
  );
  XLSX.utils.book_append_sheet(wb, sheetMat, "物料");

  const sheetOps = XLSX.utils.json_to_sheet(
    r.operations.map((o) => ({
      工序序号: o.seq,
      工序名称: o.name,
      工位: o.workstation,
      工艺标准: o.std,
      标准工时: o.time || "",
    }))
  );
  XLSX.utils.book_append_sheet(wb, sheetOps, "工序工艺");

  const sheetProc = XLSX.utils.json_to_sheet(
    r.processParams.map((p) => ({
      工艺环节: p.item,
      参数: p.param,
      质量目标: p.target,
      控制方式: p.control,
    }))
  );
  XLSX.utils.book_append_sheet(wb, sheetProc, "工艺参数");

  const summaryRows = [
    { 项目: "产品名称", 内容: r.productName },
    { 项目: "行业模板", 内容: r.industry },
    { 项目: "生成时间", 内容: formatDate(r.generatedAt) },
    ...r.kpis.map((k) => ({ 项目: k.label, 内容: `${k.value}（${k.hint}）` })),
    ...r.recommendations.map((t, i) => ({ 项目: `优化建议 ${i + 1}`, 内容: t })),
  ];
  const sheetSum = XLSX.utils.json_to_sheet(summaryRows);
  XLSX.utils.book_append_sheet(wb, sheetSum, "报告摘要");

  const flowText = r.flow.map((s, i) => `${i + 1}. ${s.title}：${s.desc}；绿色要点：${s.green}`).join("\n");
  const sheetFlow = XLSX.utils.aoa_to_sheet([["绿色生产流程（文字）"], [flowText]]);
  XLSX.utils.book_append_sheet(wb, sheetFlow, "流程说明");

  const safeName = r.productName.replace(/[\\/:*?"<>|]/g, "_");
  XLSX.writeFile(wb, `绿色生产报告_${safeName}.xlsx`);
}

function exportPdf() {
  if (!lastReport || typeof html2pdf === "undefined") {
    alert("请先生成报告，并确认网络可加载 PDF 库。");
    return;
  }
  const el = document.getElementById("pdf-export-area");
  const safeName = lastReport.productName.replace(/[\\/:*?"<>|]/g, "_");
  const opt = {
    margin: 10,
    filename: `绿色生产报告_${safeName}.pdf`,
    image: { type: "jpeg", quality: 0.95 },
    html2canvas: { scale: 2, useCORS: true, logging: false },
    jsPDF: { unit: "mm", format: "a4", orientation: "portrait" },
    pagebreak: { mode: ["avoid-all", "css", "legacy"] },
  };
  html2pdf().set(opt).from(el).save();
}

document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("btn-generate").addEventListener("click", runGenerate);
  document.getElementById("product-input").addEventListener("keydown", (e) => {
    if (e.key === "Enter") runGenerate();
  });
  document.getElementById("btn-excel").addEventListener("click", exportExcel);
  document.getElementById("btn-pdf").addEventListener("click", exportPdf);
});
