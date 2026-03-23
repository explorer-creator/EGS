<template>
  <div class="warehouse-dispatch-page">
    <el-card class="intro-card" shadow="never">
      <h2><i class="el-icon-office-building"></i> 仓库调度中心</h2>
      <p class="intro-text">
        登记<strong>货物出仓时间</strong>与流向；系统按<strong>多仓—中转—片区</strong>网络做<strong>最短路径（成本公里）</strong>推荐；
        结合在途货量估算各<strong>中转站压力</strong>与<strong>可行性得分</strong>，并支持关系图与柱状图可视化。
      </p>
    </el-card>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="出仓登记" name="out">
        <el-form :model="outForm" inline size="small" class="out-form">
          <el-form-item label="起始仓库">
            <el-select v-model="outForm.warehouseId" placeholder="选择" style="width: 160px">
              <el-option v-for="w in warehouses" :key="w" :label="w + ' ' + whLabel(w)" :value="w" />
            </el-select>
          </el-form-item>
          <el-form-item label="目的片区">
            <el-select v-model="outForm.destRegionId" placeholder="选择" style="width: 160px">
              <el-option v-for="r in regions" :key="r" :label="r + ' ' + regLabel(r)" :value="r" />
            </el-select>
          </el-form-item>
          <el-form-item label="货物编码">
            <el-input v-model="outForm.goodsCode" placeholder="SKU" style="width: 140px" />
          </el-form-item>
          <el-form-item label="重量(kg)">
            <el-input-number v-model="outForm.weightKg" :min="1" :max="99999" :step="10" />
          </el-form-item>
          <el-form-item label="出仓时间">
            <el-date-picker
              v-model="outForm.outboundTime"
              type="datetime"
              value-format="yyyy-MM-dd HH:mm:ss"
              placeholder="选择时间"
              style="width: 180px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="outLoading" @click="submitOutbound">保存出仓记录</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="recordList" border size="small" stripe v-loading="recLoading">
          <el-table-column prop="id" label="单号" width="100" />
          <el-table-column prop="warehouseId" label="仓库" width="80" />
          <el-table-column prop="goodsCode" label="货物" width="120" />
          <el-table-column prop="destRegionId" label="目的片区" width="90" />
          <el-table-column prop="weightKg" label="重量kg" width="90" />
          <el-table-column prop="outboundTime" label="出仓时间" min-width="160" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="智能调度" name="opt">
        <el-form inline size="small">
          <el-form-item label="起始仓库">
            <el-select v-model="optForm.from" style="width: 160px">
              <el-option v-for="w in warehouses" :key="'o'+w" :label="w" :value="w" />
            </el-select>
          </el-form-item>
          <el-form-item label="目的片区">
            <el-select v-model="optForm.to" style="width: 160px">
              <el-option v-for="r in regions" :key="'t'+r" :label="r" :value="r" />
            </el-select>
          </el-form-item>
          <el-form-item label="本单货重(kg)">
            <el-input-number v-model="optForm.weightKg" :min="1" :max="99999" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-cpu" :loading="optLoading" @click="runOptimize">计算最优路线与压力</el-button>
          </el-form-item>
        </el-form>
        <el-alert v-if="optError" :title="optError" type="error" show-icon :closable="false" style="margin-bottom:12px" />
        <template v-if="optResult && optResult.success">
          <el-descriptions :column="2" border size="small" class="opt-desc">
            <el-descriptions-item label="推荐路径">{{ (optResult.pathLabels || []).join(' → ') }}</el-descriptions-item>
            <el-descriptions-item label="路径节点">{{ (optResult.path || []).join(' → ') }}</el-descriptions-item>
            <el-descriptions-item label="路径成本(公里当量)">{{ optResult.totalCostKm }} km</el-descriptions-item>
            <el-descriptions-item label="参考在途时长(小时)">{{ optResult.estimateTransitHours }}</el-descriptions-item>
          </el-descriptions>
          <h4 class="sub-title">途经节点压力与可行性</h4>
          <el-table :data="optResult.nodeAnalysis" border size="small" stripe>
            <el-table-column prop="nodeId" label="节点" width="70" />
            <el-table-column prop="name" label="名称" width="120" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="pressurePct" label="压力%" width="90" />
            <el-table-column prop="feasibilityScore" label="可行性" width="90" />
            <el-table-column prop="loadKg" label="累计货量kg" width="110" />
            <el-table-column prop="note" label="说明" min-width="180" show-overflow-tooltip />
          </el-table>
        </template>
      </el-tab-pane>

      <el-tab-pane label="网络与压力可视化" name="viz">
        <el-row :gutter="16">
          <el-col :span="14">
            <div ref="graphChart" class="chart-box" />
          </el-col>
          <el-col :span="10">
            <div ref="barChart" class="chart-box" />
          </el-col>
        </el-row>
        <el-button size="small" icon="el-icon-refresh" style="margin-top: 8px" @click="loadViz">刷新</el-button>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { friendlyErrorMessage } from '@/utils/apiError'

const WH_NAMES = { W1: '华东中心仓', W2: '华南中心仓', W3: '西南中心仓' }
const REG_NAMES = { R1: '华北片区', R2: '华东片区', R3: '华中片区', R4: '华南片区', R5: '西南片区' }

export default {
  name: 'WarehouseDispatchPage',
  data() {
    return {
      activeTab: 'out',
      warehouses: ['W1', 'W2', 'W3'],
      regions: ['R1', 'R2', 'R3', 'R4', 'R5'],
      outForm: {
        warehouseId: 'W1',
        destRegionId: 'R3',
        goodsCode: 'SKU-DEMO-001',
        weightKg: 200,
        outboundTime: ''
      },
      outLoading: false,
      recLoading: false,
      recordList: [],
      optForm: { from: 'W1', to: 'R3', weightKg: 100 },
      optLoading: false,
      optError: '',
      optResult: null,
      graphChart: null,
      barChart: null
    }
  },
  mounted() {
    this.initOutTime()
    this.loadRecords()
  },
  activated() {
    this.$nextTick(() => this.loadViz())
  },
  watch: {
    activeTab(v) {
      if (v === 'viz') this.$nextTick(() => this.loadViz())
    }
  },
  beforeDestroy() {
    if (this._vizResizeBound) {
      window.removeEventListener('resize', this.onResize)
      this._vizResizeBound = false
    }
    this.graphChart?.dispose()
    this.barChart?.dispose()
  },
  methods: {
    whLabel(id) {
      return WH_NAMES[id] || ''
    },
    regLabel(id) {
      return REG_NAMES[id] || ''
    },
    initOutTime() {
      const d = new Date()
      const p = n => String(n).padStart(2, '0')
      this.outForm.outboundTime = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:00`
    },
    async loadRecords() {
      this.recLoading = true
      try {
        const res = await this.$axios.get('/api/warehouse-dispatch/records')
        const data = res.data || res
        this.recordList = data.records || []
      } catch (e) {
        if (e.response) {
          this.$message.error('加载记录失败：' + friendlyErrorMessage(e))
        }
        // 无响应时 main.js 全局已提示「请确认代理已启动」
      } finally {
        this.recLoading = false
      }
    },
    async submitOutbound() {
      this.outLoading = true
      try {
        const res = await this.$axios.post('/api/warehouse-dispatch/outbound', {
          warehouseId: this.outForm.warehouseId,
          destRegionId: this.outForm.destRegionId,
          goodsCode: this.outForm.goodsCode,
          weightKg: this.outForm.weightKg,
          outboundTime: this.outForm.outboundTime
        })
        const data = res.data || res
        if (data.success) {
          this.$message.success('出仓已记录')
          await this.loadRecords()
        } else {
          this.$message.error(data.message || '失败')
        }
      } catch (e) {
        // 出仓登记走 Java 内存会话，与顶部 xDM Cookie 无关；失败多为 8080 未启动或 CORS
        if (e.response) {
          this.$message.error('提交失败：' + friendlyErrorMessage(e))
        }
        // 无 HTTP 响应时 axios 拦截器已提示，此处不再重复弹「提交失败」
      } finally {
        this.outLoading = false
      }
    },
    async runOptimize() {
      this.optError = ''
      this.optResult = null
      this.optLoading = true
      try {
        const res = await this.$axios.get('/api/warehouse-dispatch/optimize', {
          params: {
            fromWarehouse: this.optForm.from,
            toRegion: this.optForm.to,
            goodsWeightKg: this.optForm.weightKg
          }
        })
        const data = res.data || res
        if (data.success) {
          this.optResult = data
          this.$message.success('已计算最优路径')
        } else {
          this.optError = data.message || '计算失败'
        }
      } catch (e) {
        this.optError = (e.response && e.response.data && e.response.data.message) || e.message
      } finally {
        this.optLoading = false
      }
    },
    async loadViz() {
      if (!window.echarts) return
      try {
        const [netRes, stressRes] = await Promise.all([
          this.$axios.get('/api/warehouse-dispatch/network'),
          this.$axios.get('/api/warehouse-dispatch/stress-summary')
        ])
        const net = netRes.data || netRes
        const stress = stressRes.data || stressRes
        if (!net.success || !stress.success) return

        this.graphChart?.dispose()
        this.barChart?.dispose()
        this.graphChart = null
        this.barChart = null

        const typeColor = t => {
          if (t === 'WAREHOUSE') return '#E6A23C'
          if (t === 'HUB') return '#409EFF'
          return '#67C23A'
        }
        const nodes = (net.nodes || []).map(n => {
          const st = (stress.stations || []).find(s => s.nodeId === n.id)
          const pressure = st ? st.pressurePct : 0
          return {
            id: n.id,
            name: n.name + '\n' + n.id,
            symbolSize: 28 + Math.min(22, pressure / 5),
            itemStyle: { color: typeColor(n.type), borderColor: pressure > 70 ? '#F56C6C' : '#333', borderWidth: pressure > 70 ? 2 : 0 },
            category: n.type === 'WAREHOUSE' ? 0 : n.type === 'HUB' ? 1 : 2,
            value: pressure
          }
        })
        const links = (net.links || []).map(l => ({
          source: l.source,
          target: l.target,
          lineStyle: { width: 1 + (l.costKm || 0) / 400, curveness: 0.15 },
          label: { show: false }
        }))

        const graph = window.echarts.init(this.$refs.graphChart)
        this.graphChart = graph
        graph.setOption({
          title: { text: '仓网与中转关系（节点大小∝压力）', left: 'center', textStyle: { fontSize: 14 } },
          tooltip: {},
          legend: { data: ['中心仓', '中转', '片区'], bottom: 0 },
          series: [{
            type: 'graph',
            layout: 'force',
            roam: true,
            draggable: true,
            categories: [{ name: '中心仓' }, { name: '中转' }, { name: '片区' }],
            label: { show: true, fontSize: 10 },
            force: { repulsion: 420, edgeLength: [80, 160] },
            data: nodes,
            links,
            emphasis: { focus: 'adjacency' }
          }]
        })

        const bar = window.echarts.init(this.$refs.barChart)
        this.barChart = bar
        const sts = stress.stations || []
        bar.setOption({
          title: { text: '节点压力% / 可行性', left: 'center', textStyle: { fontSize: 14 } },
          tooltip: { trigger: 'axis' },
          legend: { data: ['压力%', '可行性'], bottom: 0 },
          xAxis: { type: 'category', data: sts.map(s => s.name), axisLabel: { rotate: 35, fontSize: 10 } },
          yAxis: { type: 'value', max: 100 },
          series: [
            { name: '压力%', type: 'bar', data: sts.map(s => s.pressurePct), itemStyle: { color: '#F56C6C' } },
            { name: '可行性', type: 'bar', data: sts.map(s => s.feasibilityScore), itemStyle: { color: '#67C23A' } }
          ]
        })
        if (!this._vizResizeBound) {
          window.addEventListener('resize', this.onResize)
          this._vizResizeBound = true
        }
      } catch (e) {
        this.$message.warning('加载网络数据失败，请确认代理已启动')
      }
    },
    onResize() {
      this.graphChart?.resize()
      this.barChart?.resize()
    }
  }
}
</script>

<style scoped>
.warehouse-dispatch-page {
  max-width: 1100px;
  margin: 0 auto;
}
.intro-card {
  margin-bottom: 16px;
}
.intro-card h2 {
  margin: 0 0 8px;
  font-size: 1.2rem;
}
.intro-text {
  margin: 0;
  color: #606266;
  font-size: 0.9rem;
  line-height: 1.65;
}
.out-form {
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.opt-desc {
  margin-bottom: 12px;
}
.sub-title {
  margin: 12px 0 8px;
  font-size: 14px;
  color: #303133;
}
.chart-box {
  height: 380px;
  width: 100%;
  min-height: 260px;
}
</style>
