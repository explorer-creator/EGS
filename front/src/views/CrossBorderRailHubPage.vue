<template>
  <div class="crh-page">
    <!-- 顶栏：叙事 + 双语 + 操作 -->
    <MulticulturalRibbon
      :caption="t.multiculturalCaption"
    />
    <header class="crh-hero">
      <div class="crh-hero__bg" aria-hidden="true" />
      <div class="crh-hero__content">
        <div class="crh-hero__titles">
          <span class="crh-badge">{{ t.badge }}</span>
          <h1 class="crh-h1">{{ t.title }}</h1>
          <p class="crh-lead">{{ t.lead }}</p>
          <p class="crh-sub">{{ t.sub }}</p>
        </div>
        <div class="crh-hero__actions">
          <el-radio-group v-model="locale" size="small" class="crh-lang">
            <el-radio-button label="zh">中文</el-radio-button>
            <el-radio-button label="en">English</el-radio-button>
          </el-radio-group>
          <el-button type="primary" size="small" icon="el-icon-refresh" :loading="loading" @click="refreshAll">
            {{ t.btnRefresh }}
          </el-button>
          <el-button size="small" type="warning" plain icon="el-icon-video-play" @click="startPathGame">
            {{ t.btnGame }}
          </el-button>
        </div>
      </div>
      <div class="crh-kpi-strip">
        <div class="crh-kpi">
          <span class="crh-kpi__v">{{ wmsMetrics.estimatedPathReductionPct != null ? wmsMetrics.estimatedPathReductionPct + '%' : '—' }}</span>
          <span class="crh-kpi__l">{{ t.kpiWms }}</span>
        </div>
        <div class="crh-kpi">
          <span class="crh-kpi__v">{{ tmsData.totalDistanceKm != null ? tmsData.totalDistanceKm + ' km' : '—' }}</span>
          <span class="crh-kpi__l">{{ t.kpiTms }}</span>
        </div>
        <div class="crh-kpi crh-kpi--score">
          <span class="crh-kpi__v">{{ gameScore }}</span>
          <span class="crh-kpi__l">{{ t.kpiScore }}</span>
        </div>
        <div class="crh-kpi">
          <span class="crh-kpi__v">{{ railLevel }}</span>
          <span class="crh-kpi__l">{{ t.kpiRail }}</span>
        </div>
      </div>
    </header>

    <el-row :gutter="16" class="crh-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="crh-card crh-card--wms">
          <div slot="header" class="crh-card-head">
            <span><i class="el-icon-s-grid"></i> {{ t.secWms }}</span>
            <el-tag size="mini" type="success">{{ t.tagLive }}</el-tag>
          </div>
          <p class="crh-hint">{{ t.hintWms }}</p>
          <div class="crh-canvas-wrap">
            <canvas ref="wmsCanvas" class="crh-canvas" width="640" height="360" @click="onWmsCanvasClick" />
          </div>
          <div class="crh-metrics" v-if="wmsMetrics.baselinePathIndex != null">
            <span>{{ t.metricBase }} <strong>{{ wmsMetrics.baselinePathIndex }}</strong></span>
            <span>{{ t.metricOpt }} <strong>{{ wmsMetrics.optimizedPathIndex }}</strong></span>
          </div>
          <p v-if="wmsNote" class="crh-ai-note"><i class="el-icon-cpu"></i> {{ wmsNote }}</p>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="crh-card crh-card--tms">
          <div slot="header" class="crh-card-head">
            <span><i class="el-icon-position"></i> {{ t.secTms }}</span>
            <el-tag size="mini">{{ tmsData.algorithm || 'ACO' }}</el-tag>
          </div>
          <p class="crh-hint">{{ t.hintTms }}</p>
          <div class="crh-canvas-wrap">
            <canvas ref="tmsCanvas" class="crh-canvas" width="640" height="360" @click="onTmsCanvasClick" />
          </div>
          <el-table v-if="tmsLegs.length" :data="tmsLegs" size="mini" border class="crh-table" max-height="160">
            <el-table-column prop="from" :label="t.colFrom" min-width="100" />
            <el-table-column prop="to" :label="t.colTo" min-width="100" />
            <el-table-column prop="distanceKm" :label="t.colKm" width="72" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 游戏面板 -->
    <el-card v-if="gameActive" shadow="never" class="crh-card crh-card--game">
      <div slot="header" class="crh-card-head">
        <span><i class="el-icon-trophy"></i> {{ t.gameTitle }}</span>
        <el-tag :type="gameTimer <= 5 ? 'danger' : 'warning'" size="mini">{{ gameTimer }}s</el-tag>
      </div>
      <p class="crh-game-tip">{{ t.gameTip }}</p>
      <el-progress :percentage="pathProgressPct" :stroke-width="10" status="success" class="crh-progress" />
      <div class="crh-game-order">
        <span v-for="(n, i) in expectedPathNames" :key="i" class="crh-step" :class="{ done: i < gameStep }">{{ translateNode(n) }}</span>
      </div>
    </el-card>

    <!-- 叙事条：翻译辅助 -->
    <el-card shadow="never" class="crh-card crh-card--narr">
      <div class="crh-narr">
        <div class="crh-narr__icon">🚂</div>
        <div>
          <h3 class="crh-narr__h">{{ t.narrH }}</h3>
          <p class="crh-narr__p">{{ t.narrP }}</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import MulticulturalRibbon from '../components/MulticulturalRibbon.vue'

/** 中欧班列 + 海外仓协同可视化：对接 WMS slot-optimization + TMS schedule-optimize */

const I18N = {
  zh: {
    multiculturalCaption:
      '不同国家与民族的伙伴共同支撑跨境供应链：地球一侧是始发与海外仓，另一侧是欧洲分拨；人物剪影为抽象符号，旗帜代表中欧班列常见途经国。',
    badge: 'SILK RAIL OPS',
    title: '丝路星链 · 跨境仓线与班列协同',
    lead: '调用智能分拣（WMS 货位优化）与路线优化（TMS 蚁群 ACO），一体化呈现「跨境电商 + 海外仓」模式升级与中欧班列干线提效。',
    sub: '左侧：高周转 SKU → 近出库口货位；右侧：多节点巡回路径最小化。支持中英文切换与路径节拍小游戏。',
    btnRefresh: '同步双系统',
    btnGame: '丝路接力挑战',
    kpiWms: '拣货路径降幅',
    kpiTms: '优化巡回里程',
    kpiScore: '游戏得分',
    kpiRail: '班列运力档',
    secWms: '智能分拣 · 海外仓货位热力',
    secTms: '路线优化 · 干支线协同',
    tagLive: 'WMS API',
    hintWms: '圆点：出库口；色块：推荐货位（周转越高越亮）。点击画布可触发「波次脉冲」特效。',
    hintTms: '节点为 TMS 网点；发光弧线为蚁群算法输出的巡回边，列车沿路径流动。',
    metricBase: '优化前路径指数',
    metricOpt: '优化后路径指数',
    colFrom: '从',
    colTo: '到',
    colKm: 'km',
    gameTitle: '丝路接力 · 按序点亮节点',
    gameTip: '在倒计时内，按右侧路线顺序依次点击节点（从 DC 出发并回到 DC）。点错会重置连击。',
    narrH: '语言与跨境协作',
    narrP: '切换 English 可查看界面英文化；海外仓现场可与中欧班列「集结—换装—出境」节点做数据协同。'
  },
  en: {
    multiculturalCaption:
      'Partners across nations and cultures power cross-border supply chains: origin & overseas hubs on one side, European distribution on the other. Figures are abstract symbols; flags mark common China–Europe rail corridor countries.',
    badge: 'SILK RAIL OPS',
    title: 'Silk Rail Hub · Cross-border Warehousing & Rail',
    lead: 'Live wiring: WMS slot optimization (smart sorting) + TMS ACO route optimization — showcasing CBEC + overseas warehouse upgrade and China–Europe rail efficiency.',
    sub: 'Left: high-turnover SKU → slots near outbound; Right: minimized multi-stop tour. Bilingual UI + path relay mini-game.',
    btnRefresh: 'Sync both systems',
    btnGame: 'Relay challenge',
    kpiWms: 'Path reduction',
    kpiTms: 'Tour distance',
    kpiScore: 'Game score',
    kpiRail: 'Rail capacity',
    secWms: 'Smart sort · Slot heatmap',
    secTms: 'Routing · ACO tour',
    tagLive: 'WMS API',
    hintWms: 'Dot: outbound; tiles: recommended slots (brighter = higher turnover). Click canvas for pulse FX.',
    hintTms: 'Nodes = TMS stops; glowing arcs = ACO tour; train flows along the route.',
    metricBase: 'Baseline path index',
    metricOpt: 'Optimized path index',
    colFrom: 'From',
    colTo: 'To',
    colKm: 'km',
    gameTitle: 'Silk relay · Tap nodes in order',
    gameTip: 'Within the timer, tap nodes following the tour order (start/end at DC). Wrong tap resets combo.',
    narrH: 'Language & cross-border',
    narrP: 'Switch to English for UI; overseas hubs can sync with rail consolidation / border nodes.'
  }
}

const NODE_EN = {
  'DC-华东仓': 'DC East China',
  '客户A-松江': 'Customer A Songjiang',
  '客户B-昆山': 'Customer B Kunshan',
  '客户C-苏州': 'Customer C Suzhou',
  '客户D-市区': 'Customer D Urban',
  '客户E-嘉兴': 'Customer E Jiaxing'
}

export default {
  name: 'CrossBorderRailHubPage',
  components: { MulticulturalRibbon },
  data() {
    return {
      locale: 'zh',
      loading: false,
      wmsRecs: [],
      wmsMetrics: {},
      wmsNote: '',
      tmsData: {},
      tmsLegs: [],
      railLevel: 3,
      gameScore: 0,
      gameActive: false,
      gameTimer: 0,
      gameStep: 0,
      expectedPathNames: [],
      gameTimerId: null,
      wmsPulse: 0,
      tmsAnimT: 0,
      rafHandle: null,
      nodeLayout: [] // { name, x, y, r }
    }
  },
  computed: {
    t() {
      return I18N[this.locale] || I18N.zh
    },
    pathProgressPct() {
      if (!this.expectedPathNames.length) return 0
      return Math.round((this.gameStep / this.expectedPathNames.length) * 100)
    }
  },
  watch: {
    locale() {
      this.$nextTick(() => {
        this.drawWms()
        this.drawTms()
      })
    }
  },
  mounted() {
    this.buildNodeLayout()
    this.refreshAll()
    window.addEventListener('resize', this.onResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.onResize)
    if (this.gameTimerId) clearInterval(this.gameTimerId)
    if (this.rafHandle) cancelAnimationFrame(this.rafHandle)
  },
  methods: {
    translateNode(name) {
      if (this.locale === 'en' && NODE_EN[name]) return NODE_EN[name]
      return name
    },
    buildNodeLayout() {
      const cx = 320
      const cy = 200
      const names = ['DC-华东仓', '客户A-松江', '客户B-昆山', '客户C-苏州', '客户D-市区', '客户E-嘉兴']
      const r = 118
      this.nodeLayout = names.map((name, i) => {
        const ang = -Math.PI / 2 + (i / names.length) * Math.PI * 2
        return { name, x: cx + r * Math.cos(ang), y: cy + r * Math.sin(ang), r: i === 0 ? 26 : 22 }
      })
    },
    onResize() {
      this.drawWms()
      this.drawTms()
    },
    async refreshAll() {
      this.loading = true
      try {
        const [wms, tms] = await Promise.all([
          this.$axios.get('/api/wms-smart/slot-optimization').catch(() => ({ data: null })),
          this.$axios.get('/api/tms-smart/schedule-optimize', { params: { vehicleType: 'VAN', iterations: 42 } }).catch(() => ({ data: null }))
        ])
        const wd = wms.data || {}
        if (wd.success) {
          this.wmsRecs = wd.recommendations || []
          this.wmsMetrics = wd.metrics || {}
          this.wmsNote = wd.aiNote || ''
        } else {
          this.mockWms()
        }
        const td = tms.data || {}
        if (td.success) {
          this.tmsData = td
          this.tmsLegs = td.legs || []
        } else {
          this.mockTms()
        }
        this.railLevel = Math.min(10, 3 + Math.round((this.wmsMetrics.estimatedPathReductionPct || 0) / 8))
      } finally {
        this.loading = false
        this.$nextTick(() => {
          this.drawWms()
          this.drawTms()
          this.startAnimLoop()
        })
      }
    },
    mockWms() {
      this.wmsRecs = [
        { sku: 'SKU-A', skuName: 'Demo', turnoverIndex: 400, recommendedSlot: 'L-01-01', distanceToOutboundM: 4.4, slotCoord: { x: 2.4, y: 2 } },
        { sku: 'SKU-B', skuName: 'Demo', turnoverIndex: 300, recommendedSlot: 'L-01-02', distanceToOutboundM: 6.8, slotCoord: { x: 4.8, y: 2 } }
      ]
      this.wmsMetrics = { baselinePathIndex: 120000, optimizedPathIndex: 88000, estimatedPathReductionPct: 26.7 }
      this.wmsNote = this.locale === 'en' ? 'Local reference data (API unavailable).' : '本地参考数据（接口暂不可用）。'
    },
    mockTms() {
      this.tmsData = {
        totalDistanceKm: 62.4,
        algorithm: 'ACO',
        bestRouteOrder: ['DC-华东仓', '客户A-松江', '客户B-昆山', '客户C-苏州', '客户D-市区', '客户E-嘉兴', 'DC-华东仓']
      }
      this.tmsLegs = [
        { from: 'DC-华东仓', to: '客户A-松江', distanceKm: 12 },
        { from: '客户A-松江', to: '客户B-昆山', distanceKm: 10 },
        { from: '客户B-昆山', to: '客户C-苏州', distanceKm: 15 },
        { from: '客户C-苏州', to: '客户D-市区', distanceKm: 8 },
        { from: '客户D-市区', to: '客户E-嘉兴', distanceKm: 11 },
        { from: '客户E-嘉兴', to: 'DC-华东仓', distanceKm: 30 }
      ]
    },
    drawWms() {
      const canvas = this.$refs.wmsCanvas
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      const w = canvas.width
      const h = canvas.height
      ctx.clearRect(0, 0, w, h)
      const g = ctx.createLinearGradient(0, 0, w, h)
      g.addColorStop(0, '#0a1628')
      g.addColorStop(1, '#0c2740')
      ctx.fillStyle = g
      ctx.fillRect(0, 0, w, h)
      const pad = 40
      const maxX = 5 * 2.4
      const maxY = 4 * 2.0
      const sx = (x) => pad + (x / maxX) * (w - pad * 2)
      const sy = (y) => h - pad - (y / maxY) * (h - pad * 2)
      ctx.strokeStyle = 'rgba(56,189,248,0.15)'
      for (let c = 0; c <= 5; c++) {
        const gx = pad + (c / 5) * (w - pad * 2)
        ctx.beginPath()
        ctx.moveTo(gx, pad)
        ctx.lineTo(gx, h - pad)
        ctx.stroke()
      }
      for (let r = 0; r <= 4; r++) {
        const gy = pad + (r / 4) * (h - pad * 2)
        ctx.beginPath()
        ctx.moveTo(pad, gy)
        ctx.lineTo(w - pad, gy)
        ctx.stroke()
      }
      ctx.fillStyle = '#38bdf8'
      ctx.shadowColor = '#38bdf8'
      ctx.shadowBlur = 16
      ctx.beginPath()
      ctx.arc(sx(0), sy(0), 10, 0, Math.PI * 2)
      ctx.fill()
      ctx.shadowBlur = 0
      ctx.fillStyle = 'rgba(224,242,254,0.85)'
      ctx.font = '11px sans-serif'
      ctx.fillText('OUT', sx(0) - 12, sy(0) - 16)

      const maxTurn = Math.max(...this.wmsRecs.map((r) => r.turnoverIndex || 0), 1)
      this.wmsRecs.forEach((rec) => {
        const sc = rec.slotCoord || { x: 2.4, y: 2 }
        const cx = sx(sc.x)
        const cy = sy(sc.y)
        const inten = (rec.turnoverIndex || 0) / maxTurn
        const sz = 18 + inten * 14
        const hue = 180 + inten * 60
        ctx.fillStyle = `hsla(${hue}, 85%, 52%, ${0.35 + inten * 0.45})`
        ctx.strokeStyle = `hsla(${hue}, 90%, 65%, 0.9)`
        ctx.lineWidth = 2
        ctx.beginPath()
        ctx.rect(cx - sz / 2, cy - sz / 2, sz, sz)
        ctx.fill()
        ctx.stroke()
      })

      if (this.wmsPulse > 0) {
        const alpha = this.wmsPulse / 100
        ctx.strokeStyle = `rgba(251, 191, 36, ${alpha * 0.8})`
        ctx.lineWidth = 3
        ctx.beginPath()
        ctx.arc(sx(0), sy(0), 40 + (100 - this.wmsPulse) * 0.5, 0, Math.PI * 2)
        ctx.stroke()
      }
    },
    drawTms() {
      const canvas = this.$refs.tmsCanvas
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      const w = canvas.width
      const h = canvas.height
      ctx.clearRect(0, 0, w, h)
      const g = ctx.createLinearGradient(0, 0, w, h)
      g.addColorStop(0, '#1a1033')
      g.addColorStop(1, '#1e1b4b')
      ctx.fillStyle = g
      ctx.fillRect(0, 0, w, h)
      const t = this.tmsAnimT
      this.tmsLegs.forEach((leg, idx) => {
        const fromN = this.nodeLayout.find((n) => n.name === leg.from)
        const toN = this.nodeLayout.find((n) => n.name === leg.to)
        if (!fromN || !toN) return
        ctx.strokeStyle = 'rgba(251, 191, 36, 0.45)'
        ctx.lineWidth = 2
        ctx.beginPath()
        ctx.moveTo(fromN.x, fromN.y)
        ctx.lineTo(toN.x, toN.y)
        ctx.stroke()
        const prog = (t * 0.00035 + idx * 0.14) % 1
        const px = fromN.x + (toN.x - fromN.x) * prog
        const py = fromN.y + (toN.y - fromN.y) * prog
        ctx.strokeStyle = 'rgba(251, 191, 36, 0.95)'
        ctx.lineWidth = 3
        ctx.shadowColor = '#fbbf24'
        ctx.shadowBlur = 10
        ctx.beginPath()
        ctx.moveTo(fromN.x, fromN.y)
        ctx.lineTo(toN.x, toN.y)
        ctx.stroke()
        ctx.shadowBlur = 0
        ctx.fillStyle = '#fef08a'
        ctx.beginPath()
        ctx.arc(px, py, 6, 0, Math.PI * 2)
        ctx.fill()
      })

      this.nodeLayout.forEach((node) => {
        const hot = this.gameActive && this.expectedPathNames[this.gameStep] === node.name
        ctx.fillStyle = hot ? 'rgba(34,211,238,0.95)' : 'rgba(99,102,241,0.9)'
        ctx.strokeStyle = 'rgba(255,255,255,0.5)'
        ctx.lineWidth = 2
        ctx.beginPath()
        ctx.arc(node.x, node.y, node.r, 0, Math.PI * 2)
        ctx.fill()
        ctx.stroke()
        ctx.fillStyle = '#f8fafc'
        ctx.font = '600 10px sans-serif'
        const label = this.translateNode(node.name)
        ctx.fillText(label.length > 10 ? label.slice(0, 9) + '…' : label, node.x - 28, node.y + 36)
      })

      ctx.fillStyle = 'rgba(254, 243, 199, 0.9)'
      ctx.font = '11px sans-serif'
      ctx.fillText(this.locale === 'zh' ? '中欧班列干线（抽象拓扑）' : 'China–Europe rail (abstract)', 16, 24)
    },
    startAnimLoop() {
      if (this.rafHandle) cancelAnimationFrame(this.rafHandle)
      const loop = () => {
        this.tmsAnimT += 16
        if (this.wmsPulse > 0) {
          this.wmsPulse = Math.max(0, this.wmsPulse - 1.8)
          this.drawWms()
        }
        this.drawTms()
        this.rafHandle = requestAnimationFrame(loop)
      }
      this.rafHandle = requestAnimationFrame(loop)
    },
    onWmsCanvasClick() {
      this.wmsPulse = 100
      this.drawWms()
    },
    onTmsCanvasClick(e) {
      if (!this.gameActive) return
      const canvas = this.$refs.tmsCanvas
      const rect = canvas.getBoundingClientRect()
      const mx = ((e.clientX - rect.left) / rect.width) * canvas.width
      const my = ((e.clientY - rect.top) / rect.height) * canvas.height
      for (const node of this.nodeLayout) {
        const d = Math.hypot(mx - node.x, my - node.y)
        if (d <= node.r + 6) {
          this.onNodeTap(node.name)
          break
        }
      }
    },
    onNodeTap(name) {
      const expect = this.expectedPathNames[this.gameStep]
      if (name === expect) {
        this.gameStep++
        if (this.gameStep >= this.expectedPathNames.length) {
          this.gameScore += 100 + this.gameTimer * 5
          this.$message.success(this.locale === 'zh' ? '挑战完成！班列准点加分。' : 'Completed! Bonus applied.')
          this.endGame()
        }
      } else {
        this.gameStep = 0
        this.$message.warning(this.locale === 'zh' ? '顺序错误，连击重置' : 'Wrong order, combo reset')
      }
      this.drawTms()
    },
    startPathGame() {
      const order = this.tmsData.bestRouteOrder
      if (!order || !order.length) {
        this.$message.warning(this.locale === 'zh' ? '请先同步 TMS 数据' : 'Sync TMS first')
        return
      }
      this.expectedPathNames = [...order]
      this.gameStep = 0
      this.gameActive = true
      this.gameTimer = 25
      if (this.gameTimerId) clearInterval(this.gameTimerId)
      this.gameTimerId = setInterval(() => {
        this.gameTimer--
        if (this.gameTimer <= 0) {
          this.$message.info(this.locale === 'zh' ? '时间到' : 'Time up')
          this.endGame()
        }
      }, 1000)
      this.drawTms()
    },
    endGame() {
      this.gameActive = false
      if (this.gameTimerId) {
        clearInterval(this.gameTimerId)
        this.gameTimerId = null
      }
    }
  }
}
</script>

<style scoped>
.crh-page {
  min-height: 100%;
  padding-bottom: 32px;
}
.crh-hero {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 20px;
  color: #f8fafc;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.45);
}
.crh-hero__bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 60% at 20% 20%, rgba(251, 191, 36, 0.18), transparent 50%),
    radial-gradient(ellipse 70% 50% at 80% 80%, rgba(56, 189, 248, 0.2), transparent 45%),
    linear-gradient(145deg, #0f172a 0%, #1e293b 40%, #0c4a6e 100%);
}
.crh-hero__bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(
    -12deg,
    transparent,
    transparent 3px,
    rgba(255, 255, 255, 0.02) 3px,
    rgba(255, 255, 255, 0.02) 4px
  );
  pointer-events: none;
}
.crh-hero__content {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 28px 28px 20px;
  z-index: 1;
}
.crh-badge {
  display: inline-block;
  font-size: 11px;
  letter-spacing: 0.2em;
  color: #fcd34d;
  margin-bottom: 10px;
}
.crh-h1 {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0.04em;
  text-shadow: 0 2px 24px rgba(0, 0, 0, 0.35);
  line-height: 1.3;
}
.crh-lead {
  margin: 0 0 8px;
  font-size: 14px;
  line-height: 1.75;
  opacity: 0.95;
  max-width: 920px;
}
.crh-sub {
  margin: 0;
  font-size: 12px;
  opacity: 0.75;
  line-height: 1.6;
  max-width: 880px;
}
.crh-hero__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.crh-lang >>> .el-radio-button__inner {
  background: rgba(15, 23, 42, 0.5);
  border-color: rgba(255, 255, 255, 0.2);
  color: #e2e8f0;
}
.crh-kpi-strip {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 0 28px 22px;
  z-index: 1;
}
.crh-kpi {
  flex: 1;
  min-width: 120px;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
}
.crh-kpi--score {
  border-color: rgba(251, 191, 36, 0.35);
  box-shadow: 0 0 24px rgba(251, 191, 36, 0.12);
}
.crh-kpi__v {
  display: block;
  font-size: 22px;
  font-weight: 800;
  color: #fef3c7;
  font-variant-numeric: tabular-nums;
}
.crh-kpi__l {
  font-size: 11px;
  opacity: 0.75;
  letter-spacing: 0.06em;
}
.crh-row {
  margin-bottom: 8px !important;
}
.crh-card {
  border-radius: 16px !important;
  border: 1px solid rgba(148, 163, 184, 0.2) !important;
  background: rgba(255, 255, 255, 0.98) !important;
  overflow: hidden;
}
.crh-card--wms >>> .el-card__header {
  background: linear-gradient(90deg, rgba(224, 242, 254, 0.7), #fff);
}
.crh-card--tms >>> .el-card__header {
  background: linear-gradient(90deg, rgba(254, 243, 199, 0.55), #fff);
}
.crh-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.crh-hint {
  font-size: 12px;
  color: #64748b;
  margin: 0 0 10px;
  line-height: 1.55;
}
.crh-canvas-wrap {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.12);
}
.crh-canvas {
  display: block;
  width: 100%;
  height: auto;
  vertical-align: top;
  cursor: crosshair;
}
.crh-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 10px;
  font-size: 13px;
  color: #475569;
}
.crh-ai-note {
  margin: 10px 0 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}
.crh-table {
  margin-top: 10px;
}
.crh-card--game {
  margin-bottom: 16px;
  border: 1px solid rgba(251, 191, 36, 0.35) !important;
  background: linear-gradient(180deg, rgba(255, 251, 235, 0.95), #fff) !important;
}
.crh-game-tip {
  font-size: 13px;
  color: #92400e;
  margin: 0 0 12px;
}
.crh-progress {
  margin-bottom: 12px;
}
.crh-game-order {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.crh-step {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fef3c7;
  color: #78350f;
  border: 1px solid rgba(251, 191, 36, 0.5);
}
.crh-step.done {
  opacity: 0.45;
  text-decoration: line-through;
}
.crh-card--narr {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
}
.crh-narr {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.crh-narr__icon {
  font-size: 36px;
  line-height: 1;
}
.crh-narr__h {
  margin: 0 0 8px;
  font-size: 16px;
  color: #0f172a;
}
.crh-narr__p {
  margin: 0;
  font-size: 13px;
  color: #475569;
  line-height: 1.65;
}
</style>
