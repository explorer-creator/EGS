<template>
  <div class="report-export-page" ref="pageRoot">
    <el-card class="report-header-card" shadow="hover">
      <div class="report-header">
        <i class="el-icon-document-checked"></i>
        <h1>绿色生产流程 · 综合报告生成</h1>
        <p>输入产品名称，生成绿色制造流程示意、设备/物料/工序与工艺信息，并导出 Excel 与 PDF</p>
      </div>

      <div class="input-section">
        <el-form inline>
          <el-form-item label="产品名称">
            <el-input
              v-model="productName"
              placeholder="例如：耳机、手机、锂电池、棉服…"
              style="width: 280px"
              clearable
              @keyup.enter.native="generateReport"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="generating"
              icon="el-icon-magic-stick"
              @click="generateReport"
            >
              生成报告
            </el-button>
          </el-form-item>
        </el-form>
        <div v-if="lastReport" class="export-buttons">
          <el-button size="small" icon="el-icon-download" @click="exportExcel">导出 Excel</el-button>
          <el-button size="small" icon="el-icon-document" @click="exportPdf">导出 PDF</el-button>
        </div>
      </div>
    </el-card>

    <transition name="report-fade">
      <div
        v-if="lastReport"
        ref="reportSection"
        class="report-content"
      >
        <el-card class="flow-card" shadow="hover">
          <h3 class="section-title">绿色生产流程</h3>
          <div class="flow-track">
            <div
              v-for="(step, i) in lastReport.flow"
              :key="i"
              class="flow-step"
            >
              <span class="step-idx">{{ i + 1 }}</span>
              <h4>{{ step.title }}</h4>
              <p>{{ step.desc }}</p>
              <el-tag size="mini" type="success">{{ step.green }}</el-tag>
            </div>
          </div>
        </el-card>

        <el-card class="summary-card" shadow="hover">
          <h3 class="section-title">综合生产报告（最优方案摘要）</h3>
          <div class="metrics-bar">
            <div v-for="(k, i) in lastReport.kpis" :key="i" class="metric">
              <div class="val">{{ k.value }}</div>
              <div class="lbl">{{ k.label }}</div>
              <div class="lbl hint">{{ k.hint }}</div>
            </div>
          </div>
          <p class="industry-tag">行业模板：<strong>{{ lastReport.industry }}</strong></p>
          <el-row :gutter="16">
            <el-col :span="12">
              <div class="report-block">
                <h4>综合优化建议</h4>
                <ul>
                  <li v-for="(r, i) in lastReport.recommendations" :key="i">{{ r }}</li>
                </ul>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="report-block">
                <h4>设备清单（节选）</h4>
                <el-table :data="(lastReport.equipment || []).slice(0, 8)" size="small" border>
                  <el-table-column prop="name" label="设备" />
                  <el-table-column prop="spec" label="规格" />
                  <el-table-column prop="qty" label="数量" width="80" />
                </el-table>
              </div>
            </el-col>
          </el-row>
          <div class="report-block">
            <h4>物料（BOM 摘要）</h4>
            <el-table :data="lastReport.materials" size="small" border>
              <el-table-column prop="code" label="物料编码" width="100" />
              <el-table-column prop="name" label="名称" />
              <el-table-column prop="unit" label="单位" width="70" />
              <el-table-column prop="qty" label="用量说明" />
            </el-table>
          </div>
          <el-row :gutter="16">
            <el-col :span="12">
              <div class="report-block">
                <h4>工序与工位</h4>
                <el-table :data="lastReport.operations" size="small" border>
                  <el-table-column prop="seq" label="序号" width="70" />
                  <el-table-column prop="name" label="工序" />
                  <el-table-column prop="workstation" label="工位" />
                  <el-table-column prop="std" label="标准要点" show-overflow-tooltip />
                </el-table>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="report-block">
                <h4>工艺参数与控制</h4>
                <el-table :data="lastReport.processParams" size="small" border>
                  <el-table-column prop="item" label="环节" />
                  <el-table-column prop="param" label="参数" />
                  <el-table-column prop="target" label="目标" show-overflow-tooltip />
                </el-table>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <el-card class="pdf-preview-card" shadow="hover">
          <h3 class="section-title">PDF 导出内容预览</h3>
          <div class="pdf-content" ref="pdfExportArea">
            <div class="pdf-title">绿色制造 · 综合生产报告</div>
            <div class="pdf-meta">
              产品名称：{{ lastReport.productName }}　|　生成时间：{{ formatDate(lastReport.generatedAt) }}　|　模板：{{ lastReport.industry }}
            </div>
            <p class="pdf-intro">本报告基于行业通用绿色制造框架与典型工艺生成，供结构与合规参考；正式投产请结合工厂实际 BOM、设备台账与质量体系文件校核。</p>
            <h4 class="pdf-h4">一、绿色生产流程概览</h4>
            <ol>
              <li v-for="(s, i) in lastReport.flow" :key="i"><strong>{{ s.title }}</strong> — {{ s.desc }}（{{ s.green }}）</li>
            </ol>
            <h4 class="pdf-h4">二、关键绩效与优化建议</h4>
            <ul>
              <li v-for="(k, i) in lastReport.kpis" :key="'k'+i">{{ k.label }}：{{ k.value }}（{{ k.hint }}）</li>
              <li v-for="(r, i) in lastReport.recommendations" :key="'r'+i">{{ r }}</li>
            </ul>
            <h4 class="pdf-h4">三、设备清单</h4>
            <el-table :data="lastReport.equipment" size="small" border>
              <el-table-column prop="name" label="设备名称" />
              <el-table-column prop="spec" label="规格" />
              <el-table-column prop="qty" label="数量" width="80" />
              <el-table-column prop="note" label="备注" show-overflow-tooltip />
            </el-table>
            <h4 class="pdf-h4">四、物料（BOM）</h4>
            <el-table :data="lastReport.materials" size="small" border>
              <el-table-column prop="code" label="编码" width="100" />
              <el-table-column prop="name" label="名称" />
              <el-table-column prop="unit" label="单位" width="70" />
              <el-table-column prop="qty" label="用量" />
            </el-table>
            <h4 class="pdf-h4">五、工序与工艺参数</h4>
            <el-table :data="lastReport.operations" size="small" border>
              <el-table-column prop="seq" label="序号" width="70" />
              <el-table-column prop="name" label="工序" />
              <el-table-column prop="workstation" label="工位" />
              <el-table-column prop="std" label="标准" show-overflow-tooltip />
            </el-table>
            <el-table :data="lastReport.processParams" size="small" border style="margin-top:8px">
              <el-table-column prop="item" label="环节" />
              <el-table-column prop="param" label="参数" />
              <el-table-column prop="target" label="目标" />
              <el-table-column prop="control" label="控制" show-overflow-tooltip />
            </el-table>
          </div>
        </el-card>
      </div>
    </transition>

    <div v-if="!lastReport" class="placeholder-hint">
      <i class="el-icon-info"></i>
      输入产品名称并点击「生成报告」查看绿色流程与综合说明
    </div>
  </div>
</template>

<script>
import { matchTemplateKey, getTemplate } from '../utils/reportTemplates'

function formatDate(d) {
  const x = d instanceof Date ? d : new Date(d)
  const p = n => String(n).padStart(2, '0')
  return `${x.getFullYear()}-${p(x.getMonth() + 1)}-${p(x.getDate())} ${p(x.getHours())}:${p(x.getMinutes())}`
}

export default {
  name: 'ReportExportPage',
  inject: ['sharedNavigateParams'],
  data() {
    return {
      productName: '',
      generating: false,
      lastReport: null
    }
  },
  mounted() {
    this.applyNavigateProduct()
  },
  activated() {
    this.applyNavigateProduct()
  },
  methods: {
    applyNavigateProduct() {
      const p = this.sharedNavigateParams && this.sharedNavigateParams.product
      if (p && String(p).trim()) this.productName = String(p).trim()
    },
    buildReport() {
      const name = String(this.productName || '').trim() || '未命名产品'
      const key = matchTemplateKey(name)
      const tpl = getTemplate(key)
      return {
        productName: name,
        templateKey: key,
        generatedAt: new Date(),
        ...tpl
      }
    },
    generateReport() {
      const name = (this.productName || '').trim()
      if (!name) {
        this.$message.warning('请输入产品名称，例如：耳机')
        return
      }
      this.generating = true
      this.lastReport = null
      setTimeout(() => {
        this.lastReport = this.buildReport()
        this.generating = false
        this.$message.success('报告生成成功')
        this.$nextTick(() => {
          this.$refs.reportSection?.scrollIntoView({ behavior: 'smooth', block: 'start' })
        })
      }, 400)
    },
    exportExcel() {
      if (!this.lastReport) {
        this.$message.warning('请先生成报告')
        return
      }
      const r = this.lastReport
      const wb = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(
        r.equipment.map(e => ({ 设备名称: e.name, 规格型号: e.spec, 数量: e.qty, 备注: e.note || '' }))
      ), '设备')
      XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(
        r.materials.map(m => ({ 物料编码: m.code, 物料名称: m.name, 单位: m.unit, 用量: m.qty, 供应商要求: m.supplier || '', 备注: m.remark || '' }))
      ), '物料')
      XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(
        r.operations.map(o => ({ 工序序号: o.seq, 工序名称: o.name, 工位: o.workstation, 工艺标准: o.std, 标准工时: o.time || '' }))
      ), '工序工艺')
      XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(
        r.processParams.map(p => ({ 工艺环节: p.item, 参数: p.param, 质量目标: p.target, 控制方式: p.control || '' }))
      ), '工艺参数')
      const summaryRows = [
        { 项目: '产品名称', 内容: r.productName },
        { 项目: '行业模板', 内容: r.industry },
        { 项目: '生成时间', 内容: formatDate(r.generatedAt) },
        ...r.kpis.map(k => ({ 项目: k.label, 内容: `${k.value}（${k.hint}）` })),
        ...r.recommendations.map((t, i) => ({ 项目: `优化建议 ${i + 1}`, 内容: t }))
      ]
      XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(summaryRows), '报告摘要')
      const flowText = r.flow.map((s, i) => `${i + 1}. ${s.title}：${s.desc}；绿色要点：${s.green}`).join('\n')
      XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet([['绿色生产流程（文字）'], [flowText]]), '流程说明')
      const safeName = r.productName.replace(/[\\/:*?"<>|]/g, '_')
      XLSX.writeFile(wb, `绿色生产报告_${safeName}.xlsx`)
      this.$message.success('Excel 已导出')
    },
    exportPdf() {
      if (!this.lastReport) {
        this.$message.warning('请先生成报告')
        return
      }
      if (typeof window.html2pdf === 'undefined') {
        this.$message.error('PDF 库未加载，请刷新页面重试')
        return
      }
      const el = this.$refs.pdfExportArea
      if (!el) {
        this.$message.error('无法获取导出区域')
        return
      }
      const safeName = this.lastReport.productName.replace(/[\\/:*?"<>|]/g, '_')
      const opt = {
        margin: 10,
        filename: `绿色生产报告_${safeName}.pdf`,
        image: { type: 'jpeg', quality: 0.95 },
        html2canvas: { scale: 2, useCORS: true, logging: false },
        jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
        pagebreak: { mode: ['avoid-all', 'css', 'legacy'] }
      }
      window.html2pdf().set(opt).from(el).save()
      this.$message.success('PDF 已导出')
    },
    formatDate
  }
}
</script>

<style scoped>
.report-export-page {
  max-width: 1000px;
  margin: 0 auto;
}

.report-header-card {
  margin-bottom: 20px;
}

.report-header {
  text-align: center;
  margin-bottom: 20px;
}

.report-header h1 {
  font-size: 1.5rem;
  margin: 10px 0 8px;
  color: #303133;
}

.report-header p {
  color: #909399;
  font-size: 0.9rem;
  margin: 0;
}

.input-section {
  padding: 16px 0;
}

.export-buttons {
  margin-top: 12px;
}

.report-content {
  margin-top: 24px;
}

.report-fade-enter-active,
.report-fade-leave-active {
  transition: opacity 0.4s ease;
}

.report-fade-enter,
.report-fade-leave-to {
  opacity: 0;
}

.report-fade-enter-active .report-content {
  animation: slideUp 0.5s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(24px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.flow-card,
.summary-card,
.pdf-preview-card {
  margin-bottom: 20px;
}

.section-title {
  color: #52c41a;
  font-size: 1rem;
  margin: 0 0 16px;
}

.flow-track {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: stretch;
}

.flow-step {
  flex: 0 0 auto;
  min-width: 140px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}

.step-idx {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  border-radius: 50%;
  background: #52c41a;
  color: #fff;
  font-size: 0.75rem;
  font-weight: 700;
  margin-bottom: 8px;
}

.flow-step h4 {
  margin: 0 0 6px;
  font-size: 0.9rem;
}

.flow-step p {
  margin: 0 0 8px;
  font-size: 0.75rem;
  color: #909399;
  line-height: 1.4;
}

.metrics-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.metric {
  flex: 1;
  min-width: 120px;
  background: #f0f9eb;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #c2e7b0;
  text-align: center;
}

.metric .val {
  font-size: 1.2rem;
  font-weight: 700;
  color: #52c41a;
}

.metric .lbl {
  font-size: 0.75rem;
  color: #606266;
}

.metric .hint {
  margin-top: 4px;
  font-size: 0.7rem;
  color: #909399;
}

.industry-tag {
  color: #909399;
  font-size: 0.9rem;
  margin: 0 0 16px;
}

.industry-tag strong {
  color: #52c41a;
}

.report-block {
  margin-bottom: 16px;
}

.report-block h4 {
  font-size: 0.9rem;
  color: #52c41a;
  margin: 0 0 8px;
}

.report-block ul {
  margin: 0;
  padding-left: 1.2rem;
  font-size: 0.85rem;
  color: #606266;
}

.pdf-preview-card {
  background: #fafafa;
}

.pdf-content {
  padding: 8px 0;
}

.pdf-title {
  font-size: 1.1rem;
  color: #52c41a;
  margin: 0 0 8px;
}

.pdf-meta {
  font-size: 0.85rem;
  color: #909399;
  margin-bottom: 12px;
}

.pdf-intro {
  font-size: 0.88rem;
  margin: 0 0 12px;
  color: #606266;
}

.pdf-h4 {
  color: #52c41a;
  font-size: 0.95rem;
  margin: 16px 0 8px;
}

.pdf-content ol,
.pdf-content ul {
  margin: 0 0 12px;
  padding-left: 1.2rem;
  font-size: 0.85rem;
  color: #606266;
}

.placeholder-hint {
  text-align: center;
  padding: 48px 24px;
  color: #909399;
  font-size: 0.95rem;
}

.placeholder-hint i {
  margin-right: 8px;
}
</style>
