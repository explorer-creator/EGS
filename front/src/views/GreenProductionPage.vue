<template>
  <div class="green-production-page">
    <!-- 产品专属绿色生产分析（从产品问答跳转时展示） -->
    <el-card v-if="currentProduct" class="product-demo-card" shadow="hover">
      <div class="product-demo-header">
        <span class="product-tag"><i class="el-icon-sunny"></i> {{ currentProduct }} · 绿色生产分析</span>
        <el-button size="small" icon="el-icon-back" @click="goBackToProductQa">返回产品问答</el-button>
      </div>
      <div v-if="demoLoading" class="demo-loading">
        <i class="el-icon-loading"></i> 正在生成「{{ currentProduct }}」的绿色生产分析...
      </div>
      <div v-else-if="reportLoaded" class="product-report-content">
        <!-- 绿色分析文本 -->
        <div v-if="greenSummary" class="product-demo-content">
          <h4 class="report-section-title">绿色生产建议</h4>
          <p v-for="(p, i) in greenSummaryParagraphs" :key="i" class="demo-paragraph">{{ p }}</p>
        </div>
        <!-- 绿色仪表盘图表 -->
        <div v-if="greenDashboard && !greenDashboard.error" class="report-charts">
          <h4 class="report-section-title">绿色仪表盘</h4>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="chart-box">
                <div ref="carbonChart" class="chart-dom"></div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="chart-box">
                <div ref="pueChart" class="chart-dom"></div>
              </div>
            </el-col>
          </el-row>
          <div v-if="greenDashboard.summary" class="dashboard-summary">
            <el-tag type="success">最新 CO₂: {{ greenDashboard.summary.latest_co2_kg }} kg</el-tag>
            <el-tag type="info">最新 PUE: {{ greenDashboard.summary.latest_pue }}</el-tag>
            <el-tag type="warning">减碳率: {{ greenDashboard.summary.reduction_pct }}%</el-tag>
          </div>
        </div>
      </div>
      <div v-else class="demo-fallback">
        <p>请先在「产品制造问答」中输入产品名称并查询；侧栏「绿能排产演示」已替代原入口，本页可通过<strong>顶部搜索「绿色生产」</strong>打开。</p>
      </div>
    </el-card>

    <!-- 通用绿色生产能力介绍 -->
    <el-card class="feature-card" shadow="hover">
      <div class="feature-header green-header">
        <i class="el-icon-sunny"></i>
        <h2>{{ currentProduct ? '绿色生产能力' : '绿色生产' }}</h2>
        <span class="badge">低碳 · 零浪费</span>
      </div>
      <p class="lead">
        智能质检 × 环保指标、绿色供应链、低碳排产、碳足迹核算，实现<strong>零缺陷、零浪费</strong>的绿色制造。
      </p>

      <el-row :gutter="20" class="feature-grid">
        <el-col :span="12">
          <div class="feature-item">
            <i class="el-icon-data-analysis"></i>
            <h4>能效监测与 PUE</h4>
            <p>实时功率数据计算能源效率，PUE 越接近 1 越优</p>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="feature-item">
            <i class="el-icon-warning-outline"></i>
            <h4>碳足迹核算</h4>
            <p>生产工时与材料重量自动换算 CO₂ 排放，生成数字碳标签</p>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="feature-item">
            <i class="el-icon-s-operation"></i>
            <h4>低碳排产优化</h4>
            <p>遗传算法调整设备启停，最大化利用绿电谷时，降低总能耗</p>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="feature-item">
            <i class="el-icon-connection"></i>
            <h4>绿色供应链评分</h4>
            <p>AI 分析供应商环保报告、ISO 14001，采购建议综合价格与碳强度</p>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="feature-item">
            <i class="el-icon-warning"></i>
            <h4>次品浪费折算</h4>
            <p>视觉检测到次品时自动折算物料/能耗浪费、等效 CO₂</p>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="feature-item">
            <i class="el-icon-view"></i>
            <h4>异常检测预警</h4>
            <p>故障苗头提前预警，建议停机，防止大批量废品</p>
          </div>
        </el-col>
      </el-row>

      <el-alert type="success" :closable="false" show-icon style="margin-top: 20px;">
        <template slot="title">绿色仪表盘</template>
        减碳趋势、PUE 趋势等数据可通过 <code>/api/green/dashboard</code> 获取，支持可视化展示。
      </el-alert>
      <div class="green-carbon-link" style="margin-top: 16px;">
        <el-button type="primary" plain icon="el-icon-data-analysis" @click="goCarbonFootprint">打开「绿能排产演示（队友）」</el-button>
        <span class="link-hint">嵌入队友 Next 演示工程，需先启动 greengeneratesysteam</span>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'GreenProductionPage',
  inject: {
    sharedNavigateParams: { default: () => ({ product: '' }) },
    navigateToPage: { default: null }
  },
  data() {
    return {
      currentProduct: '',
      greenSummary: '',
      greenDashboard: null,
      reportLoaded: false,
      demoLoading: false,
      carbonChart: null,
      pueChart: null
    }
  },
  computed: {
    greenSummaryParagraphs() {
      if (!this.greenSummary) return []
      return this.greenSummary.split(/\n\n+/).filter(Boolean).map(s => s.trim())
    }
  },
  activated() {
    this.checkProductAndFetch()
    this.$nextTick(() => {
      this.carbonChart?.resize()
      this.pueChart?.resize()
    })
  },
  mounted() {
    this.checkProductAndFetch()
  },
  beforeDestroy() {
    this.carbonChart?.dispose()
    this.pueChart?.dispose()
  },
  methods: {
    checkProductAndFetch() {
      const params = this.sharedNavigateParams || {}
      const product = (params.product || '').trim()
      if (product && product !== this.currentProduct) {
        this.currentProduct = product
        this.greenSummary = ''
        this.fetchProductDemo()
      } else if (!product) {
        this.currentProduct = ''
        this.greenSummary = ''
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
          this.greenSummary = data.green_summary || ''
          this.greenDashboard = data.green_dashboard || null
          this.reportLoaded = true
          this.$nextTick(() => this.renderGreenCharts())
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
      this.greenSummary = `针对「${this.currentProduct}」的绿色生产建议：关注能效监测与 PUE、碳足迹核算、低碳排产优化、绿色供应链评分、次品浪费折算与异常检测预警。`
      const base = new Date()
      const dates = []
      const carbon = []
      const pue = []
      for (let i = 29; i >= 0; i--) {
        const d = new Date(base)
        d.setDate(d.getDate() - i)
        dates.push(d.toISOString().slice(0, 10))
        carbon.push(Math.round(1200 - i * 12 + (i % 5) * 3))
        pue.push(+(1.8 - i * 0.015).toFixed(2))
      }
      this.greenDashboard = { dates, carbon_trend: carbon, pue_trend: pue, summary: { latest_co2_kg: carbon[carbon.length - 1], latest_pue: pue[pue.length - 1], reduction_pct: 15 } }
      this.reportLoaded = true
      this.$nextTick(() => this.renderGreenCharts())
    },
    renderGreenCharts() {
      if (!this.greenDashboard || this.greenDashboard.error || !window.echarts) return
      const { dates, carbon_trend, pue_trend } = this.greenDashboard
      if (this.$refs.carbonChart) {
        this.carbonChart = window.echarts.init(this.$refs.carbonChart)
        this.carbonChart.setOption({
          title: { text: '减碳趋势', left: 'center' },
          tooltip: { trigger: 'axis' },
          xAxis: { type: 'category', data: dates },
          yAxis: { type: 'value', name: 'CO₂ (kg)' },
          series: [{ name: 'CO₂', type: 'line', data: carbon_trend, smooth: true, itemStyle: { color: '#52c41a' } }]
        })
      }
      if (this.$refs.pueChart) {
        this.pueChart = window.echarts.init(this.$refs.pueChart)
        this.pueChart.setOption({
          title: { text: 'PUE 能效趋势', left: 'center' },
          tooltip: { trigger: 'axis' },
          xAxis: { type: 'category', data: dates },
          yAxis: { type: 'value', name: 'PUE' },
          series: [{ name: 'PUE', type: 'line', data: pue_trend, smooth: true, itemStyle: { color: '#1890ff' } }]
        })
      }
    },
    goCarbonFootprint() {
      const nav = this.navigateToPage
      if (nav) nav('GreenCarbonFootprintPage')
      else this.$message.info('请从左侧菜单进入「碳足迹与绿能排产」')
    },
    goBackToProductQa() {
      const nav = this.navigateToPage
      if (nav) nav('ProductManufacturingPage')
    }
  }
}
</script>

<style scoped>
.green-production-page {
  max-width: 900px;
  margin: 0 auto;
}
.product-demo-card {
  margin-bottom: 20px;
  padding: 24px 32px;
  border-left: 4px solid #52c41a;
}
.product-demo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8f5e9;
}
.product-tag {
  font-size: 16px;
  font-weight: 600;
  color: #52c41a;
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
.link-hint {
  margin-left: 12px;
  font-size: 13px;
  color: #909399;
}
.feature-card {
  padding: 32px 40px;
}
.green-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #52c41a;
}
.green-header i {
  font-size: 36px;
  color: #52c41a;
}
.green-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}
.green-header .badge {
  margin-left: auto;
  padding: 4px 12px;
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
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
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
  transition: all 0.2s;
}
.feature-item:hover {
  background: #d9f7be;
  border-color: #52c41a;
}
.feature-item i {
  font-size: 24px;
  color: #52c41a;
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
code {
  background: #e8f5e9;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}
.report-section-title {
  margin: 16px 0 12px 0;
  font-size: 15px;
  color: #303133;
}
.report-charts {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e8f5e9;
}
.chart-box {
  height: 220px;
  margin-bottom: 16px;
}
.chart-dom {
  width: 100%;
  height: 100%;
}
.dashboard-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
</style>
