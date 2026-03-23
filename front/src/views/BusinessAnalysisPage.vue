<template>
  <div class="business-analysis-page">
    <!-- 产品专属商业分析（从产品问答跳转时展示） -->
    <el-card v-if="currentProduct" class="product-demo-card" shadow="hover">
      <div class="product-demo-header">
        <span class="product-tag"><i class="el-icon-data-line"></i> {{ currentProduct }} · 商业分析</span>
        <el-button size="small" icon="el-icon-back" @click="goBackToProductQa">返回产品问答</el-button>
      </div>
      <div v-if="demoLoading" class="demo-loading">
        <i class="el-icon-loading"></i> 正在生成「{{ currentProduct }}」的商业分析...
      </div>
      <div v-else-if="reportLoaded" class="product-report-content">
        <!-- 商业分析文本 -->
        <div v-if="businessSummary" class="product-demo-content">
          <h4 class="report-section-title">商业分析建议</h4>
          <p v-for="(p, i) in businessSummaryParagraphs" :key="i" class="demo-paragraph">{{ p }}</p>
        </div>
        <!-- 利润敏感度图表 -->
        <div v-if="sensitivityData && !sensitivityData.error" class="report-charts">
          <h4 class="report-section-title">利润敏感度分析</h4>
          <div v-if="sensitivityData.base_profit" class="sensitivity-base">
            <el-tag type="primary">基准利润: ¥{{ sensitivityData.base_profit }}</el-tag>
            <el-tag type="info">基准收入: ¥{{ sensitivityData.base_revenue }}</el-tag>
          </div>
          <div class="chart-box">
            <div ref="sensitivityChart" class="chart-dom"></div>
          </div>
        </div>
      </div>
      <div v-else class="demo-fallback">
        <p>请先在「产品制造问答」中输入产品名称并查询；侧栏「商业分析」已并入「生成综合报告」，您仍可通过延伸链接中的「生成生产方案并导入」进入本页。</p>
      </div>
    </el-card>

    <el-card class="feature-card" shadow="hover">
      <div class="feature-header business-header">
        <i class="el-icon-data-line"></i>
        <h2>{{ currentProduct ? '商业分析能力' : '商业分析' }}</h2>
        <span class="badge">智能决策</span>
      </div>
      <p class="lead">
        利润敏感度分析、智能报价、市场进入策略、大宗商品价格、最优生产方案，助力<strong>商业决策</strong>。
      </p>

      <el-row :gutter="20" class="feature-grid">
        <el-col :span="12">
          <div class="feature-item">
            <i class="el-icon-pie-chart"></i>
            <h4>利润敏感度分析</h4>
            <p>原材料/人工/能耗变动时，预测对总利润的影响</p>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="feature-item">
            <i class="el-icon-sell"></i>
            <h4>智能报价</h4>
            <p>底价、建议价、成功率预测、客户邮件话术、绿色采购建议</p>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="feature-item">
            <i class="el-icon-trend-charts"></i>
            <h4>市场进入策略</h4>
            <p>LLM 根据市场数据与用户画像生成策略建议</p>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="feature-item">
            <i class="el-icon-goods"></i>
            <h4>大宗商品价格</h4>
            <p>铜、铝等实时市价，用于报价成本核算</p>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="feature-item">
            <i class="el-icon-document-checked"></i>
            <h4>最优生产方案</h4>
            <p>综合商业、绿色、智能检测，一键生成并导出 Excel/PDF</p>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="feature-item">
            <i class="el-icon-edit-outline"></i>
            <h4>自然语言报告</h4>
            <p>将图表数据转化为可读文字，便于汇报与决策</p>
          </div>
        </el-col>
      </el-row>

      <el-alert type="info" :closable="false" show-icon style="margin-top: 20px;">
        <template slot="title">API 能力</template>
        报价、敏感度、谈判策略、多方案对比、利润优化等均提供 RESTful API，可与设备/物料/工艺模块联动。
      </el-alert>
    </el-card>

    <!-- 最优生产方案：生成、导出、一键导入 -->
    <el-card class="plan-card" shadow="hover">
      <div slot="header" class="plan-card-header">
        <span><i class="el-icon-document-checked"></i> 最优生产方案</span>
        <span class="plan-sub">综合商业、绿色、智能检测，生成后可导出 Excel/PDF 或一键导入到设备/物料/工序/工艺管理</span>
      </div>
      <el-form :model="planForm" label-width="100px" inline size="small">
        <el-form-item label="产品名称" required>
          <el-input v-model="planForm.productName" placeholder="如：耳机、充电宝、行星减速器" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="计划产量">
          <el-input-number v-model="planForm.quantity" :min="1" :max="999999" style="width: 140px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="planLoading" icon="el-icon-magic-stick" @click="generatePlan">
            生成方案
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="lastPlan" class="plan-result">
        <el-alert type="success" :closable="false" show-icon style="margin-bottom: 12px;">
          <template slot="title">方案摘要</template>
          {{ lastPlan.summary }}
        </el-alert>
        <div class="plan-actions">
          <el-button size="small" icon="el-icon-download" @click="downloadExcel">导出 Excel</el-button>
          <el-button size="small" icon="el-icon-document" @click="downloadPdf">导出 PDF</el-button>
          <el-button type="primary" size="small" icon="el-icon-upload2" :loading="importLoading" @click="oneClickImport">
            一键导入到各管理
          </el-button>
        </div>
        <p class="plan-import-hint">
          一键导入将依次创建：设备 {{ (lastPlan.equipment || []).length }} 台、物料 {{ (lastPlan.materials || []).length }} 种、工序 {{ (lastPlan.procedures || []).length }} 道、工艺路线 {{ (lastPlan.process_routes || []).length }} 条。
        </p>
      </div>
    </el-card>
  </div>
</template>

<script>
import { config } from '../config'
import { formatXdmTime } from '../utils/xdmTime'

const TENANT_ID = config.tenantId

function buildCreateParams(entityType, data) {
  return {
    master: { id: TENANT_ID, clazz: 'Tenant' },
    params: {
      id: '',
      modifier: localStorage.getItem('xdm_modifier') || 'sysadmin 1',
      lastUpdateTime: '',
      creator: 'admin',
      rdmExtensionType: entityType,
      tenant: { id: TENANT_ID, clazz: 'Tenant' },
      ...data
    }
  }
}

export default {
  name: 'BusinessAnalysisPage',
  inject: {
    sharedNavigateParams: { default: () => ({ openProductionPlan: false, product: '' }) },
    navigateToPage: { default: null }
  },
  data() {
    return {
      currentProduct: '',
      businessSummary: '',
      sensitivityData: null,
      reportLoaded: false,
      demoLoading: false,
      sensitivityChart: null,
      planForm: {
        productName: '',
        quantity: 1000
      },
      planLoading: false,
      importLoading: false,
      lastPlan: null
    }
  },
  computed: {
    businessSummaryParagraphs() {
      if (!this.businessSummary) return []
      return this.businessSummary.split(/\n\n+/).filter(Boolean).map(s => s.trim())
    }
  },
  activated() {
    this.checkProductAndFetch()
    this.$nextTick(() => this.sensitivityChart?.resize())
  },
  mounted() {
    this.checkProductAndFetch()
  },
  beforeDestroy() {
    this.sensitivityChart?.dispose()
  },
  methods: {
    checkProductAndFetch() {
      const params = this.sharedNavigateParams || {}
      const product = (params.product || '').trim()
      if (product && product !== this.currentProduct) {
        this.currentProduct = product
        this.businessSummary = ''
        this.fetchProductDemo()
      } else if (!product) {
        this.currentProduct = ''
        this.businessSummary = ''
      }
      if (product) {
        this.planForm.productName = product
        this.planForm.quantity = 1000
      }
    },
    async fetchProductDemo() {
      if (!this.currentProduct) return
      this.demoLoading = true
      this.reportLoaded = false
      try {
        const res = await this.$axios.post('/api/product-report', { product: this.currentProduct })
        const data = res.data || res
        if (data.success) {
          this.businessSummary = data.business_summary || ''
          this.sensitivityData = data.sensitivity || null
          this.reportLoaded = true
          this.$nextTick(() => this.renderSensitivityChart())
        } else {
          this.useFallbackReport()
        }
      } catch (e) {
        this.$message.warning('接口调用失败，展示默认可视化报告')
        this.useFallbackReport()
      } finally {
        this.demoLoading = false
      }
    },
    useFallbackReport() {
      this.businessSummary = `针对「${this.currentProduct}」的商业分析：建议关注利润敏感度、智能报价、市场进入策略、大宗商品价格与最优生产方案。`
      this.sensitivityData = {
        base_profit: 25000,
        base_revenue: 120000,
        base_total_cost: 95000,
        results: [
          { factor: 'bom_cost', change_percent: 5, profit_change_percent: -8.2 },
          { factor: 'labor_cost', change_percent: 5, profit_change_percent: -4.1 },
          { factor: 'energy_cost', change_percent: 10, profit_change_percent: -3.2 },
          { factor: 'power_fluctuation', change_percent: 10, profit_change_percent: -2.5 },
          { factor: 'logistics_delay', change_percent: 20, profit_change_percent: -6.4 }
        ]
      }
      this.reportLoaded = true
      this.$nextTick(() => this.renderSensitivityChart())
    },
    factorLabel(f) {
      const map = { bom_cost: 'BOM成本', labor_cost: '人工成本', energy_cost: '能耗成本', power_fluctuation: '电力波动', logistics_delay: '物流延迟' }
      return map[f] || f
    },
    renderSensitivityChart() {
      if (!this.sensitivityData || this.sensitivityData.error || !window.echarts) return
      const results = this.sensitivityData.results || []
      if (!results.length || !this.$refs.sensitivityChart) return
      const names = results.map(r => this.factorLabel(r.factor))
      const values = results.map(r => r.profit_change_percent || 0)
      this.sensitivityChart = window.echarts.init(this.$refs.sensitivityChart)
      this.sensitivityChart.setOption({
        title: { text: '各因素变动对利润的影响 (%)', left: 'center' },
        tooltip: { trigger: 'axis', formatter: '{b}: {c}%' },
        xAxis: { type: 'category', data: names },
        yAxis: { type: 'value', name: '利润变化 %' },
        series: [{ name: '利润变化', type: 'bar', data: values, itemStyle: { color: '#165DFF' } }]
      })
    },
    goBackToProductQa() {
      const nav = this.navigateToPage
      if (nav) nav('ProductManufacturingPage')
    },
    async generatePlan() {
      const product = (this.planForm.productName || '').trim()
      if (!product) {
        this.$message.warning('请输入产品名称')
        return
      }
      this.planLoading = true
      this.lastPlan = null
      try {
        const res = await this.$axios.post('/api/production-plan/optimal', {
          product_name: product,
          quantity: this.planForm.quantity
        })
        const data = res.data || res
        if (data.success) {
          this.lastPlan = data
          if (data._fallback) {
            this.$message.warning('接口暂不可用，已展示备选方案，可一键导入')
          } else {
            this.$message.success('方案生成成功')
          }
        } else {
          this.$message.error(data.message || '生成失败')
        }
      } catch (e) {
        this.$message.error('生成失败：' + (e.response?.data?.message || e.message))
      } finally {
        this.planLoading = false
      }
    },
    async downloadExcel() {
      if (!this.lastPlan) return
      try {
        const res = await this.$axios.post('/api/production-plan/export-excel', {
          product_name: this.lastPlan.product_name,
          quantity: this.lastPlan.quantity
        }, { responseType: 'blob' })
        const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `产品生产方案_${this.lastPlan.product_name}_${new Date().toISOString().slice(0, 10)}.xlsx`
        a.click()
        URL.revokeObjectURL(url)
        this.$message.success('Excel 已下载')
      } catch (e) {
        this.$message.error('导出失败：' + (e.response?.data?.message || e.message))
      }
    },
    async downloadPdf() {
      if (!this.lastPlan) return
      try {
        const res = await this.$axios.post('/api/production-plan/export-pdf', {
          product_name: this.lastPlan.product_name,
          quantity: this.lastPlan.quantity
        }, { responseType: 'blob' })
        const blob = new Blob([res.data], { type: 'application/pdf' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `产品生产方案_${this.lastPlan.product_name}_${new Date().toISOString().slice(0, 10)}.pdf`
        a.click()
        URL.revokeObjectURL(url)
        this.$message.success('PDF 已下载')
      } catch (e) {
        const msg = e.response?.headers?.['x-error-message'] || e.response?.headers?.['X-Error-Message'] || e.response?.data?.message || e.message
        this.$message.error('导出失败：' + (msg || '服务暂不可用'))
      }
    },
    async oneClickImport() {
      if (!this.lastPlan) return
      try {
        await this.$confirm(
          '将依次导入设备、物料、工序、工艺路线到对应管理模块，是否继续？',
          '一键导入',
          { type: 'info' }
        )
      } catch (e) {
        if (e !== 'cancel') throw e
        return
      }
      this.importLoading = true
      const stats = { equipment: 0, material: 0, procedure: 0, route: 0 }
      const procCodeToId = {}
      const errors = []
      const errMsg = (ex) => {
        const d = ex.response?.data
        if (typeof d === 'string') return d
        if (d?.message) return d.message
        if (d?.params?.message) return d.params.message
        if (ex.response?.status === 405) return '405 Method Not Allowed，请确认 xDM-F(8003) 已启动'
        if (ex.response?.status === 403) return '服务暂不可用，请稍后重试'
        return ex.message || '未知错误'
      }
      try {
        // 1. 导入设备
        for (const e of this.lastPlan.equipment || []) {
          try {
            const body = buildCreateParams('Equipment', {
              equipment_code: e.equipment_code,
              equipment_name: e.equipment_name,
              manufacturer: e.manufacturer || '',
              brand: e.brand || '',
              model: e.model || '',
              supplier: e.supplier || '',
              productionDate: formatXdmTime(new Date()).slice(0, 10) + ' 00:00:00',
              depreciationMethod: 'PrintinUsingTime',
              location: e.location || '',
              technical_params: e.technical_params || '',
              spare_parts_info: ''
            })
            await this.$axios.post('/api/dynamic/api/EquipmentManagement/create', body, { headers: { 'Content-Type': 'application/json;charset=UTF-8' } })
            stats.equipment++
          } catch (ex) {
            if (errors.length < 3) errors.push('设备: ' + errMsg(ex))
          }
        }

        // 2. 导入物料
        for (const m of this.lastPlan.materials || []) {
          try {
            const body = buildCreateParams('Material01', {
              material_code: m.material_code,
              material_name: m.material_name,
              model: m.model || '',
              stock_quantity: m.quantity || 0,
              supplier: m.supplier || '',
              version: '',
              description: ''
            })
            await this.$axios.post('/api/dynamic/api/MaterialManagement/create', body, { headers: { 'Content-Type': 'application/json;charset=UTF-8' } })
            stats.material++
          } catch (ex) {
            if (errors.length < 3) errors.push('物料: ' + errMsg(ex))
          }
        }

        // 3. 导入工序
        for (const p of this.lastPlan.procedures || []) {
          try {
            const body = buildCreateParams('WorkingProcedure', {
              procedure_code: p.procedure_code,
              procedure_name: p.procedure_name,
              production_steps: p.production_steps || '',
              production_inspection_equipment: p.production_inspection_equipment || '',
              operator: p.operator || '',
              start_time: '',
              end_time: '',
              description: ''
            })
            const createRes = await this.$axios.post('/api/dynamic/api/ProcedureManagement/create', body, { headers: { 'Content-Type': 'application/json;charset=UTF-8' } })
            const d = createRes.data || {}
            const id = (d.params && d.params.id) || d.id || (d.data && d.data.id)
            if (id) procCodeToId[p.procedure_code] = id
            stats.procedure++
          } catch (ex) {
            if (errors.length < 3) errors.push('工序: ' + errMsg(ex))
          }
        }

        // 4. 导入工艺路线及工序关联
        for (const r of this.lastPlan.process_routes || []) {
          try {
            const body = buildCreateParams('ProcessRoute', {
              route_code: r.route_code,
              route_name: r.route_name,
              product: r.product || this.lastPlan.product_name,
              version: r.version || '1.0',
              description: r.description || '',
              operator: '',
              operation_time: '',
              equipment_usage: ''
            })
            const createRes = await this.$axios.post('/api/dynamic/api/ProcessRoute/create', body, { headers: { 'Content-Type': 'application/json;charset=UTF-8' } })
            const d = createRes.data || {}
            const routeId = (d.params && d.params.id) || d.id || (d.data && d.data.id)
            if (routeId && r.procedure_sequence && r.procedure_sequence.length) {
              for (let i = 0; i < r.procedure_sequence.length; i++) {
                const procId = procCodeToId[r.procedure_sequence[i]]
                if (procId) {
                  try {
                    const linkBody = buildCreateParams('ProcessRouteProcedure', {
                      route_id: routeId,
                      procedure_id: procId,
                      sequence_order: i + 1
                    })
                    await this.$axios.post('/api/dynamic/api/ProcessRouteProcedure/create', linkBody, { headers: { 'Content-Type': 'application/json;charset=UTF-8' } })
                  } catch (ex) {
                    if (errors.length < 3) errors.push('工艺路线工序: ' + errMsg(ex))
                  }
                }
              }
              stats.route++
            } else if (routeId) {
              stats.route++
            }
          } catch (ex) {
            if (errors.length < 3) errors.push('工艺路线: ' + errMsg(ex))
          }
        }

        const total = stats.equipment + stats.material + stats.procedure + stats.route
        if (total > 0) {
          this.$message.success(
            `导入完成：设备 ${stats.equipment} 台、物料 ${stats.material} 种、工序 ${stats.procedure} 道、工艺路线 ${stats.route} 条。` +
            '请到各管理页面刷新查看。'
          )
        } else if (errors.length > 0) {
          this.$message.error('导入失败：' + errors[0] + (errors.length > 1 ? ' 等' : '') + '。请确认 xDM-F(8003) 已启动且鉴权通过。')
        } else {
          this.$message.warning('导入未成功，请稍后重试。')
        }
      } catch (e) {
        this.$message.error('导入失败：' + (e.response?.data?.message || e.message))
      } finally {
        this.importLoading = false
      }
    }
  }
}
</script>

<style scoped>
.business-analysis-page {
  max-width: 900px;
  margin: 0 auto;
}
.product-demo-card {
  margin-bottom: 20px;
  padding: 24px 32px;
  border-left: 4px solid #165DFF;
}
.product-demo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ecf5ff;
}
.product-tag {
  font-size: 16px;
  font-weight: 600;
  color: #165DFF;
}
.product-tag i {
  margin-right: 6px;
}
.demo-loading {
  padding: 24px;
  text-align: center;
  color: #909399;
}
.demo-loading i {
  margin-right: 8px;
}
.product-demo-content {
  line-height: 1.9;
  color: #303133;
}
.demo-paragraph {
  margin: 0 0 12px 0;
}
.demo-paragraph:last-child {
  margin-bottom: 0;
}
.demo-fallback {
  padding: 16px 0;
  color: #909399;
  font-size: 14px;
}
.feature-card {
  padding: 32px 40px;
  margin-bottom: 20px;
}
.plan-card {
  padding: 24px 32px;
}
.plan-card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.plan-card-header .plan-sub {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}
.business-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #165DFF;
}
.business-header i {
  font-size: 36px;
  color: #165DFF;
}
.business-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}
.business-header .badge {
  margin-left: auto;
  padding: 4px 12px;
  background: linear-gradient(135deg, #165DFF 0%, #0e42c2 100%);
  color: #fff;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}
.lead {
  font-size: 15px;
  line-height: 1.8;
  color: #606266;
  margin: 0 0 24px 0;
}
.feature-grid {
  margin-top: 16px;
}
.feature-item {
  padding: 16px;
  margin-bottom: 16px;
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
  border-radius: 8px;
  transition: all 0.2s;
}
.feature-item:hover {
  background: #d9ecff;
  border-color: #165DFF;
}
.feature-item i {
  font-size: 24px;
  color: #165DFF;
  margin-bottom: 8px;
}
.feature-item h4 {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #303133;
}
.feature-item p {
  margin: 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
.plan-result {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
.plan-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;
}
.plan-import-hint {
  margin: 0;
  font-size: 12px;
  color: #909399;
}
.report-section-title {
  margin: 16px 0 12px 0;
  font-size: 15px;
  color: #303133;
}
.report-charts {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ecf5ff;
}
.sensitivity-base {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}
.chart-box {
  height: 260px;
  margin-bottom: 16px;
}
.chart-dom {
  width: 100%;
  height: 100%;
}
</style>
