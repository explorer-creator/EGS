<template>
  <div class="wms-smart-page">
    <el-card class="hero" shadow="never">
      <h2><i class="el-icon-box"></i> 智能仓储管理系统（WMS）</h2>
      <ul class="feature-list">
        <li><strong>动态货位规划</strong>：基于历史订单行数（周转代理指标），采用贪心策略将高周转 SKU 分配至距出库口更近的货位，估算拣货路径降幅。</li>
        <li><strong>「货到人」调度</strong>：统一展示 AGV、四向穿梭车与机械臂状态与任务队列，支持下发调度任务。</li>
        <li><strong>数字孪生监控</strong>：虚拟库区 1:1 映射拥堵度与告警，便于快速发现瓶颈与低电量设备。</li>
      </ul>
    </el-card>

    <el-tabs v-model="tab" type="border-card" @tab-click="onTab">
      <!-- 动态货位 -->
      <el-tab-pane label="动态货位规划" name="slot">
        <el-row :gutter="16">
          <el-col :span="14">
            <el-card shadow="hover">
              <div slot="header" class="card-head">
                <span>历史订单热度</span>
                <el-button type="primary" size="small" icon="el-icon-cpu" :loading="slotLoading" @click="loadSlotOpt">AI 重新计算货位</el-button>
              </div>
              <el-table :data="historyLines" size="small" border max-height="220" v-loading="histLoading">
                <el-table-column prop="sku" label="SKU" width="120" />
                <el-table-column prop="name" label="名称" />
                <el-table-column prop="orderLines" label="历史行数" width="100" sortable />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="hover">
              <div slot="header">优化指标</div>
              <div v-if="slotMetrics" class="metrics">
                <div class="metric"><span class="lab">路径指数(优化前)</span><b>{{ slotMetrics.baselinePathIndex }}</b></div>
                <div class="metric"><span class="lab">路径指数(优化后)</span><b class="ok">{{ slotMetrics.optimizedPathIndex }}</b></div>
                <div class="metric highlight">
                  <span class="lab">预估拣货路径降幅</span><b>{{ slotMetrics.estimatedPathReductionPct }}%</b>
                </div>
                <p class="hint">{{ aiNote }}</p>
                <el-button type="success" size="small" plain :loading="applyLoading" @click="applySlots">应用推荐方案</el-button>
              </div>
              <el-empty v-else description="点击「AI 重新计算货位」" :image-size="64" />
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="hover" style="margin-top:16px">
          <div slot="header">推荐货位（高周转 → 近出库口）</div>
          <el-table :data="recommendations" size="small" border v-loading="slotLoading">
            <el-table-column prop="sku" label="SKU" width="110" />
            <el-table-column prop="skuName" label="名称" width="100" />
            <el-table-column prop="turnoverIndex" label="周转指数" width="90" />
            <el-table-column prop="recommendedSlot" label="推荐货位" width="100" />
            <el-table-column prop="distanceToOutboundM" label="距出库口(m)" width="110" />
            <el-table-column prop="currentAppliedSlot" label="已应用货位" width="100" />
            <el-table-column prop="algorithm" label="策略" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 货到人 -->
      <el-tab-pane label="货到人调度" name="gtp">
        <el-row :gutter="12">
          <el-col v-for="d in devices" :key="d.id" :xs="24" :sm="12" :md="8">
            <el-card shadow="hover" class="dev-card" :class="'type-' + (d.type || '').toLowerCase()">
              <div class="dev-title">
                <span class="dev-id">{{ d.id }}</span>
                <el-tag size="mini">{{ d.typeLabel }}</el-tag>
              </div>
              <div class="dev-body">
                <div>状态：<el-tag :type="stateTag(d.state)" size="mini">{{ d.state }}</el-tag></div>
                <div class="battery"><span>电量</span><el-progress :percentage="d.batteryPct" :stroke-width="10" /></div>
                <div class="pos">坐标 (m)：{{ d.position.x }}, {{ d.position.y }}</div>
                <div class="task">任务：{{ d.currentTask }}</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="hover" style="margin-top:16px">
          <div slot="header" class="card-head">
            <span>统一任务队列</span>
            <el-form inline size="small" class="dispatch-form">
              <el-form-item label="类型">
                <el-select v-model="dispatchForm.taskType" style="width:120px">
                  <el-option label="出库" value="OUTBOUND" />
                  <el-option label="入库" value="INBOUND" />
                  <el-option label="盘点" value="COUNT" />
                </el-select>
              </el-form-item>
              <el-form-item label="SKU">
                <el-input v-model="dispatchForm.sku" placeholder="SKU" style="width:140px" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="small" :loading="dispatchLoading" @click="dispatchTask">下发任务</el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-table :data="tasks" size="small" border v-loading="gtpLoading">
            <el-table-column prop="id" label="任务号" width="90" />
            <el-table-column prop="taskType" label="类型" width="90" />
            <el-table-column prop="sku" label="SKU" width="110" />
            <el-table-column prop="from" label="起点" width="90" />
            <el-table-column prop="to" label="终点" width="110" />
            <el-table-column prop="status" label="状态" width="90" />
            <el-table-column prop="assignedTo" label="分配设备" width="100" />
            <el-table-column prop="priority" label="优先级" width="80" />
            <el-table-column prop="createdAt" label="创建时间" min-width="140" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 数字孪生 -->
      <el-tab-pane label="数字孪生监控" name="twin">
        <el-row :gutter="16">
          <el-col :span="16">
            <el-card shadow="hover" class="twin-board">
              <div slot="header" class="card-head">
                <span>库区孪生视图（拥堵热力）</span>
                <span class="twin-time">{{ twinTime }}</span>
              </div>
              <div class="twin-layout">
                <div class="outbound-pill"><i class="el-icon-position"></i> 出库口 / 拣选站</div>
                <div
                  v-for="(z, i) in twinZones"
                  :key="z.zoneId"
                  class="zone-cell"
                  :style="zoneStyle(z)"
                  @click="selectedZone = z"
                >
                  <div class="zn">{{ z.name }}</div>
                  <div class="zc">{{ z.congestionPct }}%</div>
                  <div class="zh">{{ z.health }}</div>
                </div>
              </div>
              <div class="legend">
                <span><i class="dot low"></i> 畅通</span>
                <span><i class="dot mid"></i> 关注</span>
                <span><i class="dot high"></i> 拥堵</span>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <div slot="header">实时告警</div>
              <el-timeline v-if="twinAlerts.length">
                <el-timeline-item
                  v-for="(a, idx) in twinAlerts"
                  :key="idx"
                  :type="a.level === 'WARN' ? 'warning' : 'primary'"
                  :timestamp="a.source"
                >
                  {{ a.message }}
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无告警" :image-size="48" />
            </el-card>
            <el-card shadow="never" style="margin-top:12px" v-if="selectedZone">
              <div slot="header">选中：{{ selectedZone.name }}</div>
              <p>拥堵：{{ selectedZone.congestionPct }}%</p>
              <p>吞吐：约 {{ selectedZone.throughputPerHour }} 件/小时</p>
              <p>健康：{{ selectedZone.health }}</p>
            </el-card>
          </el-col>
        </el-row>
        <div ref="twinChart" class="twin-chart" />
        <el-button size="small" icon="el-icon-refresh" @click="loadTwin">刷新孪生数据</el-button>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
export default {
  name: 'WmsSmartSystemPage',
  data() {
    return {
      tab: 'slot',
      historyLines: [],
      recommendations: [],
      slotMetrics: null,
      aiNote: '',
      histLoading: false,
      slotLoading: false,
      applyLoading: false,
      devices: [],
      tasks: [],
      gtpLoading: false,
      dispatchForm: { taskType: 'OUTBOUND', sku: 'SKU-A100' },
      dispatchLoading: false,
      twinZones: [],
      twinAlerts: [],
      twinTime: '',
      twinTimer: null,
      twinChart: null,
      selectedZone: null
    }
  },
  mounted() {
    this.loadHistory()
    this.loadSlotOpt()
    this.loadGtp()
    this.loadTwin()
    this.startTwinTimer()
  },
  activated() {
    this.startTwinTimer()
  },
  deactivated() {
    this.stopTwinTimer()
  },
  beforeDestroy() {
    this.stopTwinTimer()
    this.twinChart?.dispose()
  },
  methods: {
    onTab() {
      if (this.tab === 'twin') {
        this.$nextTick(() => this.renderTwinChart())
      }
    },
    startTwinTimer() {
      if (this.twinTimer) return
      this.twinTimer = setInterval(() => {
        if (this.tab === 'twin') this.loadTwin()
      }, 8000)
    },
    stopTwinTimer() {
      if (this.twinTimer) {
        clearInterval(this.twinTimer)
        this.twinTimer = null
      }
    },
    async loadHistory() {
      this.histLoading = true
      try {
        const res = await this.$axios.get('/api/wms-smart/history-orders')
        const d = res.data || res
        this.historyLines = d.lines || []
      } catch (e) {
        this.$message.error('加载历史数据失败')
      } finally {
        this.histLoading = false
      }
    },
    async loadSlotOpt() {
      this.slotLoading = true
      try {
        const res = await this.$axios.get('/api/wms-smart/slot-optimization')
        const d = res.data || res
        if (d.success) {
          this.recommendations = d.recommendations || []
          this.slotMetrics = d.metrics || null
          this.aiNote = d.aiNote || ''
        }
      } catch (e) {
        this.$message.error('货位优化失败')
      } finally {
        this.slotLoading = false
      }
    },
    async applySlots() {
      this.applyLoading = true
      try {
        const res = await this.$axios.post('/api/wms-smart/apply-slot-plan', {})
        const d = res.data || res
        if (d.success) {
          this.$message.success(d.message || '已应用')
          await this.loadSlotOpt()
        }
      } catch (e) {
        this.$message.error('应用失败')
      } finally {
        this.applyLoading = false
      }
    },
    async loadGtp() {
      this.gtpLoading = true
      try {
        const res = await this.$axios.get('/api/wms-smart/goods-to-person')
        const d = res.data || res
        if (d.success) {
          this.devices = d.devices || []
          this.tasks = d.tasks || []
        }
      } catch (e) {
        this.$message.error('加载货到人数据失败')
      } finally {
        this.gtpLoading = false
      }
    },
    async dispatchTask() {
      this.dispatchLoading = true
      try {
        const res = await this.$axios.post('/api/wms-smart/dispatch-task', {
          taskType: this.dispatchForm.taskType,
          sku: this.dispatchForm.sku
        })
        const d = res.data || res
        if (d.success) {
          this.$message.success('任务已入队')
          await this.loadGtp()
        }
      } catch (e) {
        this.$message.error('下发失败')
      } finally {
        this.dispatchLoading = false
      }
    },
    async loadTwin() {
      try {
        const res = await this.$axios.get('/api/wms-smart/digital-twin')
        const d = res.data || res
        if (d.success) {
          this.twinZones = d.zones || []
          this.twinAlerts = d.alerts || []
          this.twinTime = d.timestamp || ''
        }
        this.$nextTick(() => this.renderTwinChart())
      } catch (e) {
        this.$message.warning('孪生数据加载失败')
      }
    },
    zoneStyle(z) {
      const c = z.congestionPct || 0
      let bg = 'rgba(103,194,58,0.35)'
      if (c > 70) bg = 'rgba(245,108,108,0.45)'
      else if (c > 50) bg = 'rgba(230,162,60,0.4)'
      return { background: bg }
    },
    stateTag(s) {
      if (s === 'MOVING' || s === 'WORKING') return 'success'
      if (s === 'CHARGING' || s === 'WARN') return 'warning'
      return 'info'
    },
    renderTwinChart() {
      if (!window.echarts || !this.$refs.twinChart) return
      this.twinChart?.dispose()
      this.twinChart = window.echarts.init(this.$refs.twinChart)
      const z = this.twinZones
      this.twinChart.setOption({
        title: { text: '各库区拥堵%', left: 'center', textStyle: { fontSize: 13 } },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: z.map(x => x.name), axisLabel: { rotate: 25 } },
        yAxis: { type: 'value', max: 100 },
        series: [{
          type: 'bar',
          data: z.map(x => x.congestionPct),
          itemStyle: {
            color: p => (p.value > 70 ? '#F56C6C' : p.value > 50 ? '#E6A23C' : '#67C23A')
          }
        }]
      })
    }
  }
}
</script>

<style scoped>
.wms-smart-page {
  max-width: 1100px;
  margin: 0 auto;
}
.hero h2 {
  margin: 0 0 12px;
  font-size: 1.25rem;
}
.feature-list {
  margin: 0;
  padding-left: 1.2rem;
  color: #606266;
  line-height: 1.75;
  font-size: 0.92rem;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.metrics .metric {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px dashed #ebeef5;
}
.metrics .metric.highlight {
  border-bottom: none;
  margin-top: 8px;
  font-size: 1.1rem;
}
.metrics .lab { color: #909399; font-size: 13px; }
.metrics b.ok { color: #67c23a; }
.metrics .hint { font-size: 12px; color: #909399; margin: 12px 0; line-height: 1.5; }
.dev-card {
  margin-bottom: 12px;
  border-left: 4px solid #dcdfe6;
}
.dev-card.type-agv { border-left-color: #409eff; }
.dev-card.type-shuttle { border-left-color: #e6a23c; }
.dev-card.type-robot_arm { border-left-color: #67c23a; }
.dev-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.dev-id { font-weight: 600; }
.dev-body { font-size: 13px; color: #606266; line-height: 1.7; }
.dev-body .battery { margin: 8px 0; }
.dispatch-form { margin: 0; }
.twin-board .twin-layout {
  position: relative;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  min-height: 200px;
  padding: 36px 8px 8px;
  background: linear-gradient(180deg, #f0f2f5 0%, #e4e7ed 100%);
  border-radius: 8px;
}
.outbound-pill {
  position: absolute;
  top: 6px;
  left: 50%;
  transform: translateX(-50%);
  background: #165dff;
  color: #fff;
  padding: 4px 14px;
  border-radius: 16px;
  font-size: 12px;
}
.zone-cell {
  border-radius: 6px;
  padding: 10px 6px;
  text-align: center;
  cursor: pointer;
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: transform 0.15s;
}
.zone-cell:hover { transform: scale(1.02); }
.zn { font-size: 12px; font-weight: 600; }
.zc { font-size: 18px; font-weight: 700; margin: 4px 0; }
.zh { font-size: 11px; opacity: 0.85; }
.legend {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 16px;
}
.legend .dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 4px;
}
.dot.low { background: #67c23a; }
.dot.mid { background: #e6a23c; }
.dot.high { background: #f56c6c; }
.twin-time { font-size: 12px; color: #909399; }
.twin-chart { height: 220px; width: 100%; margin: 16px 0 8px; }
</style>
