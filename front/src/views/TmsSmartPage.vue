<template>
  <div class="tms-page">
    <el-card class="hero" shadow="never">
      <h2><i class="el-icon-truck"></i> 智慧运输管理系统（TMS）</h2>
      <ul class="feature-list">
        <li><strong>智能调度与路径优化</strong>：综合成本权重、客户时效窗软约束与<strong>货车限行</strong>边；后端采用<strong>蚁群算法 ACO</strong> 近似求解。实际生产可先用<strong>高德/谷歌路线规划 API</strong>拉取<strong>实时路况与旅行时间矩阵</strong>，对<strong>多车多约束 VRP</strong>再调用<strong>Gurobi、CPLEX 或 OR-Tools</strong>等<strong>求解器</strong>求最优配载与路径。</li>
        <li><strong>在途可视化</strong>：<strong>OBD</strong> 油耗、急刹、发动机与冷却液；<strong>视觉 AI</strong> 估计疲劳分并分级预警。</li>
        <li><strong>ETA 精准预测</strong>：综合<strong>实时路况</strong>、<strong>天气</strong>、<strong>司机驾驶习惯</strong>等特征，调用<strong>机器学习模型部署 API</strong>（如<strong>阿里云 PAI-EAS</strong>）输出<strong>分钟级</strong>预计到达时间，便于下游月台与收货安排（见「ETA 预测」页签）。</li>
      </ul>
    </el-card>

    <el-card class="open-meteo-card" shadow="never">
      <div slot="header" class="om-header">
        <span><i class="el-icon-sunny"></i> 当前天气</span>
        <el-tag size="mini" type="success">GET /api/weather/open-meteo/current</el-tag>
      </div>
      <p class="om-lead">
        与 ETA 同源：返回当前气象编码、风速与延误系数。可输入目的地经纬度（示例：上海约 纬度 31.23、经度 121.47）。
      </p>
      <el-form inline size="small" class="om-form">
        <el-form-item label="纬度">
          <el-input-number v-model="openMeteoForm.latitude" :precision="4" :step="0.01" :min="-90" :max="90" controls-position="right" style="width: 140px" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="openMeteoForm.longitude" :precision="4" :step="0.01" :min="-180" :max="180" controls-position="right" style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" :loading="openMeteoLoading" @click="fetchOpenMeteoCurrent">查询当前天气</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="openMeteoError" :title="openMeteoError" type="error" show-icon :closable="false" class="mb8" />
      <el-descriptions v-if="openMeteoResult && openMeteoResult.success" :column="2" border size="small" class="om-desc">
        <el-descriptions-item label="天气（WMO）">{{ openMeteoResult.description }}</el-descriptions-item>
        <el-descriptions-item label="weather_code">{{ openMeteoResult.weatherCode }}</el-descriptions-item>
        <el-descriptions-item label="10m 风速 m/s">{{ openMeteoResult.windSpeedMs }}</el-descriptions-item>
        <el-descriptions-item label="延误系数">{{ openMeteoResult.delayFactor }}</el-descriptions-item>
        <el-descriptions-item v-if="openMeteoResult.latitude != null" label="纬度">{{ openMeteoResult.latitude }}</el-descriptions-item>
        <el-descriptions-item v-if="openMeteoResult.longitude != null" label="经度">{{ openMeteoResult.longitude }}</el-descriptions-item>
        <el-descriptions-item label="数据源" :span="2">{{ openMeteoResult.source }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-tabs v-model="tab" type="border-card">
      <el-tab-pane label="调度与路径优化" name="sched">
        <el-alert type="info" :closable="false" show-icon class="sched-api-hint">
          <strong>工程落地提示</strong>：单车 ETA/路况可对接<strong>高德/Google 路线 API</strong>；本页 ACO 为近似解。复杂 <strong>VRP</strong> 建议用 <strong>OR-Tools / Gurobi / CPLEX</strong> 与路况矩阵联合求解。
        </el-alert>
        <el-form inline size="small" class="sched-form">
          <el-form-item label="车型">
            <el-select v-model="schedForm.vehicleType" style="width:120px">
              <el-option label="货车(限行)" value="TRUCK" />
              <el-option label="厢式(不限)" value="VAN" />
            </el-select>
          </el-form-item>
          <el-form-item label="成本¥/km">
            <el-input-number v-model="schedForm.costPerKm" :min="0.5" :max="10" :step="0.1" />
          </el-form-item>
          <el-form-item label="时效违约¥/分">
            <el-input-number v-model="schedForm.twPenalty" :min="0" :max="5" :step="0.1" />
          </el-form-item>
          <el-form-item label="蚁群迭代">
            <el-input-number v-model="schedForm.iterations" :min="10" :max="80" :step="5" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-cpu" :loading="schedLoading" @click="runSchedule">运行优化</el-button>
          </el-form-item>
        </el-form>
        <el-alert v-if="schedError" :title="schedError" type="error" show-icon :closable="false" class="mb12" />
        <template v-if="schedResult && schedResult.success">
          <el-descriptions :column="2" border size="small" class="mb12">
            <el-descriptions-item label="算法">{{ schedResult.algorithm }}</el-descriptions-item>
            <el-descriptions-item label="车型">{{ schedResult.vehicleType }}</el-descriptions-item>
            <el-descriptions-item label="总里程 km">{{ schedResult.totalDistanceKm }}</el-descriptions-item>
            <el-descriptions-item label="综合成本">{{ schedResult.estimatedCost }}</el-descriptions-item>
            <el-descriptions-item label="推荐顺序" :span="2">{{ (schedResult.bestRouteOrder || []).join(' → ') }}</el-descriptions-item>
          </el-descriptions>
          <p class="rl-note">{{ schedResult.rlNote }}</p>
          <el-table :data="schedResult.legs" border size="small" stripe>
            <el-table-column prop="from" label="从" width="130" />
            <el-table-column prop="to" label="到" width="130" />
            <el-table-column prop="distanceKm" label="里程km" width="90" />
            <el-table-column label="限行边" width="90">
              <template slot-scope="scope">
                <el-tag v-if="scope.row.restrictedEdge" type="danger" size="mini">限行</el-tag>
                <span v-else>—</span>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-tab-pane>

      <el-tab-pane label="在途监控 OBD+视觉" name="transit">
        <el-row :gutter="12" v-loading="transitLoading">
          <el-col v-for="v in vehicles" :key="v.vehicleId" :xs="24" :md="8">
            <el-card shadow="hover" class="veh-card">
              <div class="veh-head">
                <span class="vid">{{ v.vehicleId }}</span>
                <span class="plate">{{ v.plate }}</span>
              </div>
              <div class="veh-meta">速度 {{ v.speedKmh }} km/h · GPS {{ v.lat.toFixed(3) }}, {{ v.lng.toFixed(3) }}</div>
              <el-divider content-position="left">OBD</el-divider>
              <ul class="obd-list">
                <li>百公里油耗 <b>{{ v.obd.avgFuelLPer100Km }}</b> L</li>
                <li>瞬时油耗 <b>{{ v.obd.instantFuelLPerH }}</b> L/h</li>
                <li>30min 急刹 <b>{{ v.obd.harshBrakeCount30m }}</b> 次</li>
                <li>发动机 <el-tag size="mini">{{ v.obd.engineStatus }}</el-tag> · 水温 {{ v.obd.coolantTempC }}℃</li>
                <li v-if="v.obd.dtcCodes && v.obd.dtcCodes.length">故障码 <el-tag type="warning" size="mini">{{ v.obd.dtcCodes.join(',') }}</el-tag></li>
              </ul>
              <el-divider content-position="left">视觉·疲劳</el-divider>
              <div class="fatigue">
                <span>疲劳分 {{ v.driverVision.fatigueScore }}</span>
                <el-tag :type="fatigueTag(v.driverVision.alertLevel)" size="small">{{ v.driverVision.alertLevel }}</el-tag>
              </div>
              <el-progress :percentage="v.driverVision.fatigueScore" :status="v.driverVision.fatigueScore > 75 ? 'exception' : undefined" />
              <p class="vision-meta">EAR {{ v.driverVision.eyeAspectRatio.toFixed(3) }} · 打哈欠 {{ v.driverVision.yawnCountPerMinute }}/min · {{ v.driverVision.model }}</p>
              <el-alert v-if="v.activeWarning" :title="v.activeWarning" type="warning" show-icon :closable="false" />
            </el-card>
          </el-col>
        </el-row>
        <el-button size="small" icon="el-icon-refresh" @click="loadTransit">刷新</el-button>
      </el-tab-pane>

      <el-tab-pane label="ETA 预测" name="eta">
        <el-alert type="success" :closable="false" show-icon class="eta-ml-banner">
          <template slot="title">机器学习 ETA（气象估算 + 可选高德路径）</template>
          接口 <code>/api/tms-smart/eta-ml-predict</code> 综合<strong>气象估算</strong>与<strong>高德驾车路径</strong>（需配置 <code>AMAP_KEY</code>）；未配高德时用球面距离估算并在 <code>warnings</code> 中说明。可再对接 <strong>PAI-EAS</strong> 做模型融合。
        </el-alert>
        <el-alert v-if="etaMlApiWarnings.length" type="warning" :closable="false" show-icon class="mb8">
          <template slot="title">路况降级提示</template>
          <ul class="warn-list"><li v-for="(w, i) in etaMlApiWarnings" :key="'mw'+i">{{ w }}</li></ul>
        </el-alert>

        <el-card shadow="hover" class="eta-ml-card" v-loading="etaMlLoading">
          <div slot="header" class="eta-ml-card-head">
            <span>ML 精准 ETA（特征 + 部署信息）</span>
            <el-button type="primary" size="small" icon="el-icon-cpu" :loading="etaMlLoading" @click="loadEtaMl">调用预测 API</el-button>
          </div>
          <el-descriptions v-if="etaMlDeployment" :column="2" border size="small" class="mb12">
            <el-descriptions-item label="部署平台">{{ etaMlDeployment.platform }}</el-descriptions-item>
            <el-descriptions-item label="调用方式">{{ etaMlDeployment.invokeStyle }}</el-descriptions-item>
            <el-descriptions-item label="模型">{{ etaMlDeployment.modelName }} · {{ etaMlDeployment.modelVersion }}</el-descriptions-item>
            <el-descriptions-item label="目标 MAE">{{ etaMlMaeTarget }} 分钟</el-descriptions-item>
            <el-descriptions-item label="集成说明" :span="2">{{ etaMlDeployment.integrationNote }}</el-descriptions-item>
          </el-descriptions>
          <p v-if="etaMlDisclaimer" class="eta-ml-disclaimer">{{ etaMlDisclaimer }}</p>
          <el-table :data="etaMlPredictions" border size="small" stripe max-height="320">
            <el-table-column type="expand">
              <template slot-scope="props">
                <div class="eta-expand" v-if="props.row.features">
                  <p><strong>路况</strong>：拥堵指数 {{ props.row.features.trafficCongestionIndex }} · {{ props.row.features.trafficSource }}</p>
                  <p><strong>天气</strong>：{{ props.row.features.weather }} · 影响系数 {{ props.row.features.weatherImpactFactor }} · {{ props.row.features.weatherSource }}</p>
                  <p><strong>驾驶习惯</strong>：得分 {{ props.row.features.driverHabitScore }} · {{ props.row.features.driverHabitNote }} · 风格乘子 {{ props.row.features.driverStyleMultiplier }}</p>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="tripId" label="运单" width="125" />
            <el-table-column prop="origin" label="起点" width="95" />
            <el-table-column prop="destination" label="终点" width="95" />
            <el-table-column prop="baselineDurationMin" label="基准(min)" width="88" />
            <el-table-column label="路况指数" width="88">
              <template slot-scope="scope">{{ scope.row.features && scope.row.features.trafficCongestionIndex }}</template>
            </el-table-column>
            <el-table-column label="天气" width="100">
              <template slot-scope="scope">{{ scope.row.features && (scope.row.features.weatherDescription || scope.row.features.weather) }}</template>
            </el-table-column>
            <el-table-column label="习惯分" width="72">
              <template slot-scope="scope">{{ scope.row.features && scope.row.features.driverHabitScore }}</template>
            </el-table-column>
            <el-table-column prop="remainingMinutes" label="剩余(min)" width="88" />
            <el-table-column prop="predictedEta" label="预计到达" min-width="135" />
            <el-table-column prop="confidenceBandMinutes" label="±分钟" width="72" />
            <el-table-column prop="downstreamHint" label="下游" min-width="120" show-overflow-tooltip />
          </el-table>
        </el-card>

        <el-alert v-if="etaRuleWarnings.length" type="warning" :closable="false" show-icon class="mb8">
          <template slot="title">统计 ETA 路况提示</template>
          <ul class="warn-list"><li v-for="(w, i) in etaRuleWarnings" :key="'rw'+i">{{ w }}</li></ul>
        </el-alert>
        <el-divider content-position="left">统计 / 规则 ETA（对照）</el-divider>
        <el-row :gutter="16" v-loading="etaLoading">
          <el-col :span="16">
            <el-table :data="etaTrips" border size="small" stripe>
              <el-table-column prop="tripId" label="运单" width="130" />
              <el-table-column prop="origin" label="起点" width="100" />
              <el-table-column prop="destination" label="终点" width="100" />
              <el-table-column prop="baselineDurationMin" label="基准(min)" width="95" />
              <el-table-column prop="trafficDelayFactor" label="路况因子" width="90" />
              <el-table-column prop="historicalMaeMinutes" label="历史MAE" width="85" />
              <el-table-column prop="predictedEta" label="预计到达" min-width="150" />
              <el-table-column prop="etaConfidenceBandMin" label="±分钟带" width="85" />
            </el-table>
            <p class="eta-note">与上方同源：天气为气象估算；路况优先高德，否则见 warnings。</p>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover" v-if="etaSummary">
              <div slot="header">预测摘要</div>
              <p>历史 MAE：<b>{{ etaSummary.maeVsHistoryMinutes }}</b> 分钟</p>
              <p>{{ etaSummary.realtimeTrafficBlend }}</p>
              <p class="target">{{ etaSummary.targetErrorBand }}</p>
            </el-card>
          </el-col>
        </el-row>
        <div ref="etaChart" class="eta-chart" />
        <el-button size="small" icon="el-icon-refresh" @click="refreshEtaAll">刷新全部 ETA</el-button>
      </el-tab-pane>

      <el-tab-pane label="AI 延误分析 · 溯源" name="aiDelay">
        <el-alert type="info" :closable="false" show-icon class="ai-banner">
          <template slot="title">TMS + 小机器人 + 千问</template>
          选择运单后拉取<strong>与 ETA 页同源</strong>的天气、路况（高德或估算）与在途车辆快照，由<strong>千问</strong>生成延误解释；未配置 API Key 时使用规则合成。第二步可生成<strong>SHA-256 事件哈希</strong>用于存证。
        </el-alert>

        <el-row :gutter="16" class="ai-row">
          <el-col :xs="24" :md="10">
            <el-card shadow="hover" class="ai-card ai-card--control">
              <div slot="header" class="ai-card-head">
                <span><i class="el-icon-cpu"></i> 分析控制</span>
              </div>
              <el-form label-width="88px" size="small">
                <el-form-item label="运单">
                  <el-select
                    v-model="selectedTripId"
                    placeholder="请选择运单"
                    style="width:100%"
                    @change="onTripSelectChange"
                  >
                    <el-option
                      v-for="t in tripOptions"
                      :key="t.tripId"
                      :label="t.tripId + ' · ' + t.origin + '→' + t.destination"
                      :value="t.tripId"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="提问">
                  <el-input
                    v-model="delayQuestion"
                    type="textarea"
                    :rows="3"
                    placeholder="例如：为什么这批货可能延误？"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" icon="el-icon-magic-stick" :loading="delayLoading" @click="runDelayInsight">
                    生成智能解释
                  </el-button>
                  <el-button icon="el-icon-refresh" @click="refreshTripOptions">同步运单列表</el-button>
                </el-form-item>
              </el-form>
              <p class="ai-hint">所选运单会写入本地缓存，<strong>小机器人</strong>内询问延误相关问题时将优先使用该运单。</p>
            </el-card>

            <el-card shadow="hover" class="ai-card ai-card--trace">
              <div slot="header" class="ai-card-head">
                <span><i class="el-icon-link"></i> 可溯源（哈希）</span>
              </div>
              <p class="trace-desc">对当前运单快照生成 <strong>SHA-256</strong> 指纹，用于供应链存证。</p>
              <el-button type="success" plain icon="el-icon-key" :loading="traceLoading" @click="runTraceAnchor">
                生成事件哈希
              </el-button>
              <div v-if="traceResult && traceResult.hash" class="trace-hash-box">
                <div class="trace-hash-label">指纹（可复制）</div>
                <el-input type="textarea" :rows="3" readonly :value="traceResult.hash" class="trace-hash-input" />
                <div class="trace-meta">
                  <el-tag size="mini" type="info">{{ traceResult.hashAlgorithm }}</el-tag>
                  <span class="trace-ts">{{ formatTraceTs(traceResult.timestamp) }}</span>
                </div>
                <p class="trace-disclaimer">{{ traceResult.dataDisclaimer }}</p>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :md="14">
            <el-card shadow="hover" class="ai-card ai-card--viz" v-loading="delayLoading">
              <div slot="header" class="ai-card-head">
                <span><i class="el-icon-data-analysis"></i> 解释与上下文</span>
                <el-tag v-if="delayResult && delayResult.usedLlm" type="success" size="mini">千问</el-tag>
                <el-tag v-else-if="delayResult" type="warning" size="mini">规则合成</el-tag>
              </div>
              <div v-if="!delayResult && !delayLoading" class="ai-empty">
                <i class="el-icon-chat-dot-round"></i>
                <p>选择运单并点击「生成智能解释」查看结果</p>
              </div>
              <div v-else-if="delayResult" class="ai-result">
                <div class="ai-explain-box">
                  <h4><i class="el-icon-info"></i> 智能解释</h4>
                  <pre class="ai-explain-text">{{ delayResult.explanation }}</pre>
                </div>
                <el-row :gutter="12" class="ctx-cards" v-if="delayResult.context">
                  <el-col :span="12">
                    <div class="ctx-tile ctx-tile--sky">
                      <div class="ctx-tile-title">天气（目的地）</div>
                      <p>{{ (delayResult.context.weather && delayResult.context.weather.description) || '—' }}</p>
                      <small v-if="delayResult.context.weather && delayResult.context.weather.delayFactor">
                        延误系数 {{ delayResult.context.weather.delayFactor }} · {{ delayResult.context.weather.source }}
                      </small>
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="ctx-tile ctx-tile--road">
                      <div class="ctx-tile-title">路况</div>
                      <p>拥堵指数 {{ (delayResult.context.traffic && delayResult.context.traffic.congestionIndex) != null ? delayResult.context.traffic.congestionIndex : '—' }}</p>
                      <small>{{ delayResult.context.traffic && delayResult.context.traffic.source }}</small>
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="ctx-tile ctx-tile--eta">
                      <div class="ctx-tile-title">预计到达</div>
                      <p class="ctx-eta">{{ delayResult.context.eta && delayResult.context.eta.predictedEta }}</p>
                      <small>剩余约 {{ delayResult.context.eta && delayResult.context.eta.remainingMinutes }} 分钟</small>
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="ctx-tile ctx-tile--veh">
                      <div class="ctx-tile-title">在途车辆</div>
                      <p>{{ delayResult.context.vehicle && delayResult.context.vehicle.vehicleId }} · 疲劳 {{ delayResult.context.vehicle && delayResult.context.vehicle.fatigueScore }}</p>
                      <small>{{ delayResult.context.vehicle && delayResult.context.vehicle.note }}</small>
                    </div>
                  </el-col>
                </el-row>
                <el-collapse v-if="delayResult.context" class="ctx-json">
                  <el-collapse-item title="完整上下文 JSON" name="ctx">
                    <pre class="json-pre">{{ JSON.stringify(delayResult.context, null, 2) }}</pre>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
export default {
  name: 'TmsSmartPage',
  data() {
    return {
      tab: 'sched',
      schedForm: {
        vehicleType: 'TRUCK',
        costPerKm: 1.2,
        twPenalty: 0.5,
        iterations: 25
      },
      schedLoading: false,
      schedError: '',
      schedResult: null,
      vehicles: [],
      transitLoading: false,
      etaTrips: [],
      etaSummary: null,
      etaLoading: false,
      etaChart: null,
      etaMlPredictions: [],
      etaMlDeployment: null,
      etaMlDisclaimer: '',
      etaMlMaeTarget: '',
      etaMlLoading: false,
      etaMlApiWarnings: [],
      etaRuleWarnings: [],
      selectedTripId: '',
      delayQuestion: '为什么这批货可能延误？',
      delayLoading: false,
      delayResult: null,
      traceLoading: false,
      traceResult: null,
      openMeteoForm: { latitude: 31.23, longitude: 121.47 },
      openMeteoLoading: false,
      openMeteoResult: null,
      openMeteoError: ''
    }
  },
  computed: {
    tripOptions() {
      const ml = this.etaMlPredictions || []
      if (ml.length) {
        return ml.map((t) => ({
          tripId: t.tripId,
          origin: t.origin,
          destination: t.destination
        }))
      }
      return (this.etaTrips || []).map((t) => ({
        tripId: t.tripId,
        origin: t.origin,
        destination: t.destination
      }))
    }
  },
  mounted() {
    this.runSchedule()
    this.loadTransit()
    this.loadEta()
    this.loadEtaMl()
  },
  activated() {
    this.$nextTick(() => this.renderEtaChart())
  },
  beforeDestroy() {
    this.etaChart?.dispose()
  },
  watch: {
    tab(v) {
      if (v === 'eta') {
        this.$nextTick(() => {
          this.renderEtaChart()
          this.loadEtaMl()
        })
      }
      if (v === 'aiDelay') {
        this.syncAiTripSelection()
        this.refreshTripOptions()
      }
    },
    tripOptions: {
      handler(list) {
        if (!list || !list.length) return
        const saved = localStorage.getItem('tms_selected_trip_id')
        if (saved && list.some((x) => x.tripId === saved)) {
          this.selectedTripId = saved
        } else if (!this.selectedTripId || !list.some((x) => x.tripId === this.selectedTripId)) {
          this.selectedTripId = list[0].tripId
        }
      },
      immediate: true
    },
    etaTrips: {
      handler() {
        this.$nextTick(() => this.renderEtaChart())
      },
      deep: true
    }
  },
  methods: {
    async fetchOpenMeteoCurrent() {
      this.openMeteoError = ''
      this.openMeteoLoading = true
      try {
        const res = await this.$axios.get('/api/weather/open-meteo/current', {
          params: {
            latitude: this.openMeteoForm.latitude,
            longitude: this.openMeteoForm.longitude
          }
        })
        const d = res.data || res
        if (d.success) {
          this.openMeteoResult = d
          this.$message.success('已获取天气数据')
        } else {
          this.openMeteoResult = null
          this.openMeteoError = d.message || '查询失败'
        }
      } catch (e) {
        this.openMeteoResult = null
        this.openMeteoError =
          (e.response && e.response.data && e.response.data.message) || e.message || '请求失败（请确认 Java 代理已启动且可访问外网）'
      } finally {
        this.openMeteoLoading = false
      }
    },
    async runSchedule() {
      this.schedError = ''
      this.schedLoading = true
      try {
        const res = await this.$axios.get('/api/tms-smart/schedule-optimize', {
          params: {
            vehicleType: this.schedForm.vehicleType,
            costPerKm: this.schedForm.costPerKm,
            timeWindowPenaltyPerMin: this.schedForm.twPenalty,
            iterations: this.schedForm.iterations
          }
        })
        const d = res.data || res
        if (d.success) {
          this.schedResult = d
          this.$message.success('路径优化完成')
        } else {
          this.schedError = d.message || '失败'
          this.schedResult = null
        }
      } catch (e) {
        this.schedError = (e.response && e.response.data && e.response.data.message) || e.message || '请求失败'
      } finally {
        this.schedLoading = false
      }
    },
    async loadTransit() {
      this.transitLoading = true
      try {
        const res = await this.$axios.get('/api/tms-smart/in-transit')
        const d = res.data || res
        this.vehicles = d.vehicles || []
      } catch (e) {
        this.$message.error('在途数据加载失败')
      } finally {
        this.transitLoading = false
      }
    },
    async loadEta() {
      this.etaLoading = true
      try {
        const res = await this.$axios.get('/api/tms-smart/eta-predictions')
        const d = res.data || res
        if (d.success === false) {
          this.$message.error(d.message || 'ETA 接口返回失败')
          this.etaTrips = []
          this.etaSummary = null
          this.etaRuleWarnings = []
          return
        }
        this.etaTrips = d.trips || []
        this.etaSummary = d.summary || null
        this.etaRuleWarnings = d.warnings || []
        this.$nextTick(() => this.renderEtaChart())
      } catch (e) {
        this.$message.error('ETA 加载失败：' + (e.message || ''))
      } finally {
        this.etaLoading = false
      }
    },
    async loadEtaMl() {
      this.etaMlLoading = true
      try {
        const res = await this.$axios.get('/api/tms-smart/eta-ml-predict')
        const d = res.data || res
        if (d.success === false) {
          this.$message.error(d.message || 'ML ETA 失败')
          this.etaMlPredictions = []
          this.etaMlDeployment = null
          this.etaMlApiWarnings = []
          return
        }
        this.etaMlPredictions = d.predictions || []
        this.etaMlDeployment = d.deployment || null
        this.etaMlDisclaimer = d.dataDisclaimer || ''
        this.etaMlMaeTarget = d.maeTargetMinutes != null ? d.maeTargetMinutes : ''
        this.etaMlApiWarnings = d.warnings || []
      } catch (e) {
        this.$message.error('ML ETA 请求失败：' + (e.message || ''))
      } finally {
        this.etaMlLoading = false
      }
    },
    async refreshEtaAll() {
      await Promise.all([this.loadEta(), this.loadEtaMl()])
      this.$message.success('已刷新')
    },
    fatigueTag(level) {
      if (level === 'HIGH') return 'danger'
      if (level === 'MEDIUM') return 'warning'
      return 'success'
    },
    syncAiTripSelection() {
      const saved = localStorage.getItem('tms_selected_trip_id')
      if (saved && this.tripOptions.some((x) => x.tripId === saved)) {
        this.selectedTripId = saved
      }
    },
    onTripSelectChange(v) {
      if (v) localStorage.setItem('tms_selected_trip_id', v)
    },
    async refreshTripOptions() {
      await this.refreshEtaAll()
    },
    formatTraceTs(ts) {
      if (ts == null) return ''
      try {
        return new Date(ts).toLocaleString('zh-CN')
      } catch (e) {
        return String(ts)
      }
    },
    async runDelayInsight() {
      if (!this.selectedTripId) {
        this.$message.warning('请先选择运单')
        return
      }
      this.delayLoading = true
      this.delayResult = null
      try {
        const res = await this.$axios.post('/api/tms-smart/delay-insight', {
          tripId: this.selectedTripId,
          userQuestion: this.delayQuestion || '为什么这批货可能延误？'
        })
        const d = res.data || res
        if (d.success === false) {
          this.$message.error(d.message || '分析失败')
          return
        }
        this.delayResult = d
        if (d.message && d.usedLlm === false) this.$message.info(d.message)
        else this.$message.success('已生成解释')
      } catch (e) {
        this.$message.error((e.response && e.response.data && e.response.data.message) || e.message || '请求失败')
      } finally {
        this.delayLoading = false
      }
    },
    async runTraceAnchor() {
      const tid = this.selectedTripId || localStorage.getItem('tms_selected_trip_id') || 'T-20250316-01'
      this.traceLoading = true
      this.traceResult = null
      try {
        const res = await this.$axios.post('/api/tms-smart/trace-anchor', {
          tripId: tid,
          eventType: 'DELAY_INSIGHT_DEMO',
          note: '丝路智联 TMS 存证'
        })
        const d = res.data || res
        if (!d.success) {
          this.$message.error(d.message || '生成失败')
          return
        }
        this.traceResult = d
        this.$message.success('已生成事件哈希')
      } catch (e) {
        this.$message.error((e.response && e.response.data && e.response.data.message) || e.message || '请求失败')
      } finally {
        this.traceLoading = false
      }
    },
    renderEtaChart() {
      if (!window.echarts || !this.$refs.etaChart || !this.etaTrips.length) return
      this.etaChart?.dispose()
      this.etaChart = window.echarts.init(this.$refs.etaChart)
      const trips = this.etaTrips
      this.etaChart.setOption({
        title: { text: '基准时长 vs 路况延误后 ETA', left: 'center', textStyle: { fontSize: 13 } },
        tooltip: { trigger: 'axis' },
        legend: { data: ['基准(min)', '预测行程(min)'], bottom: 0 },
        xAxis: { type: 'category', data: trips.map(t => t.tripId) },
        yAxis: { type: 'value' },
        series: [
          { name: '基准(min)', type: 'bar', data: trips.map(t => t.baselineDurationMin), itemStyle: { color: '#909399' } },
          {
            name: '预测行程(min)',
            type: 'bar',
            data: trips.map(t => Math.round(t.baselineDurationMin * t.trafficDelayFactor * 10) / 10),
            itemStyle: { color: '#165dff' }
          }
        ]
      })
    }
  }
}
</script>

<style scoped>
.tms-page {
  max-width: 1100px;
  margin: 0 auto;
}
.hero h2 {
  margin: 0 0 12px;
  font-size: 1.25rem;
}
.open-meteo-card {
  margin-bottom: 16px;
}
.om-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.om-lead {
  margin: 0 0 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.65;
}
.om-form {
  margin-bottom: 8px;
}
.om-desc {
  margin-top: 8px;
}
.mb8 {
  margin-bottom: 8px;
}
.feature-list {
  margin: 0;
  padding-left: 1.2rem;
  color: #606266;
  line-height: 1.75;
  font-size: 0.92rem;
}
.sched-api-hint {
  margin-bottom: 12px;
  line-height: 1.65;
}
.sched-form {
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.mb12 {
  margin-bottom: 12px;
}
.rl-note {
  font-size: 12px;
  color: #909399;
  margin: 0 0 12px;
  line-height: 1.5;
}
.veh-card {
  margin-bottom: 12px;
}
.veh-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.vid {
  font-weight: 700;
}
.plate {
  font-size: 13px;
  color: #606266;
}
.veh-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
.obd-list {
  margin: 0;
  padding-left: 1.1rem;
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
}
.fatigue {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;
}
.vision-meta {
  font-size: 12px;
  color: #909399;
  margin: 8px 0 0;
}
.eta-note {
  font-size: 12px;
  color: #909399;
  margin: 6px 0 0;
}
.target {
  color: #67c23a;
  font-weight: 600;
}
.eta-chart {
  height: 240px;
  margin: 16px 0 8px;
}

.eta-ml-banner {
  margin-bottom: 14px;
  line-height: 1.65;
}
.eta-ml-banner code {
  font-size: 12px;
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
}
.eta-ml-card {
  margin-bottom: 20px;
}
.eta-ml-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.eta-ml-disclaimer {
  font-size: 12px;
  color: #e6a23c;
  margin: 0 0 12px;
  line-height: 1.5;
}
.mb12 {
  margin-bottom: 12px;
}
.eta-expand {
  padding: 8px 12px 12px;
  font-size: 12px;
  color: #606266;
  line-height: 1.7;
  background: #fafafa;
}
.eta-expand p {
  margin: 4px 0;
}
.mb8 {
  margin-bottom: 8px;
}
.warn-list {
  margin: 0;
  padding-left: 1.2rem;
  font-size: 13px;
}

/* AI 延误分析 */
.ai-banner {
  margin-bottom: 16px;
  line-height: 1.65;
}
.ai-row {
  margin-top: 4px;
}
.ai-card {
  border-radius: 12px;
  margin-bottom: 16px;
  border: 1px solid #e5e8ef;
}
.ai-card--viz {
  min-height: 420px;
}
.ai-card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  color: #1d2129;
}
.ai-card-head i {
  color: #165dff;
}
.ai-hint {
  font-size: 12px;
  color: #86909c;
  line-height: 1.6;
  margin: 12px 0 0;
}
.trace-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 12px;
}
.trace-hash-box {
  margin-top: 14px;
  padding: 12px;
  background: linear-gradient(135deg, #f0f7ff 0%, #fff 100%);
  border-radius: 8px;
  border: 1px dashed #a3c4ff;
}
.trace-hash-label {
  font-size: 12px;
  color: #86909c;
  margin-bottom: 6px;
}
.trace-hash-input >>> textarea {
  font-family: Consolas, monospace;
  font-size: 12px;
}
.trace-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.trace-ts {
  font-size: 12px;
  color: #606266;
}
.trace-disclaimer {
  font-size: 11px;
  color: #909399;
  margin: 8px 0 0;
  line-height: 1.5;
}
.ai-empty {
  text-align: center;
  padding: 48px 16px;
  color: #86909c;
}
.ai-empty i {
  font-size: 42px;
  color: #c9cdd4;
  display: block;
  margin-bottom: 12px;
}
.ai-explain-box {
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #f7f9fc;
  border-radius: 10px;
  border-left: 4px solid #165dff;
}
.ai-explain-box h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #165dff;
}
.ai-explain-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.75;
  color: #303133;
  font-family: inherit;
}
.ctx-cards {
  margin-bottom: 8px;
}
.ctx-tile {
  padding: 12px 14px;
  border-radius: 10px;
  margin-bottom: 12px;
  min-height: 92px;
  color: #303133;
  font-size: 13px;
  line-height: 1.5;
}
.ctx-tile p {
  margin: 4px 0 6px;
  font-weight: 600;
}
.ctx-tile small {
  font-size: 11px;
  color: #86909c;
}
.ctx-tile-title {
  font-size: 12px;
  color: #86909c;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.ctx-tile--sky {
  background: linear-gradient(145deg, #e8f4ff 0%, #f5fbff 100%);
  border: 1px solid #b3d8ff;
}
.ctx-tile--road {
  background: linear-gradient(145deg, #fff7e6 0%, #fffbf0 100%);
  border: 1px solid #ffd591;
}
.ctx-tile--eta {
  background: linear-gradient(145deg, #e6fffb 0%, #f0fffd 100%);
  border: 1px solid #87e8de;
}
.ctx-tile--veh {
  background: linear-gradient(145deg, #f9f0ff 0%, #fcf7ff 100%);
  border: 1px solid #d3adf7;
}
.ctx-eta {
  font-size: 16px;
  color: #165dff;
}
.ctx-json {
  margin-top: 8px;
}
.json-pre {
  margin: 0;
  font-size: 11px;
  line-height: 1.45;
  max-height: 220px;
  overflow: auto;
  background: #1e293b;
  color: #e2e8f0;
  padding: 10px;
  border-radius: 8px;
}
</style>
