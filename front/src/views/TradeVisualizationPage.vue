<template>
  <div class="trade-viz-page">
    <el-card class="hero-card" shadow="hover">
      <div class="hero-inner">
        <div class="hero-text">
          <h2><i class="el-icon-pie-chart"></i> 国际贸易数据可视化</h2>
          <p class="lead">
            基于<strong>公开统计口径与行业文献</strong>整理的结构占比数据，以饼图展示我国货物贸易<strong>出口/进口</strong>的商品大类与<strong>区域分布</strong>。
            详细说明与来源索引见项目内文档 <code>docs/国际贸易大数据摘要.md</code>。
          </p>
          <el-alert type="success" :closable="false" show-icon class="trusted-alert">
            <template slot="title">可信数据层</template>
            饼图扇区已标注数据来源类型：<strong>官方统计口径</strong>（汇总）与<strong>链上可验证节点</strong>（哈希对齐）。
            {{ dataset.meta.trustedLayerNote || '' }}
          </el-alert>
        </div>
        <div class="hero-controls">
          <span class="ctrl-label">图表高度</span>
          <el-slider
            v-model="chartHeightPx"
            :min="240"
            :max="640"
            :step="20"
            show-input
            :show-tooltip="true"
            class="height-slider"
          />
          <span class="ctrl-hint">{{ chartHeightPx }} px · 窗口变化时自动适配</span>
        </div>
      </div>
    </el-card>

    <el-card class="trusted-legend-card" shadow="never">
      <div class="trusted-legend">
        <span class="tl-item"><i class="tl-dot tl-dot--official"></i> 官方统计口径（汇总）</span>
        <span class="tl-item"><i class="tl-dot tl-dot--chain"></i> 链上可验证节点（哈希 / 跨公司对齐）</span>
        <span class="tl-hint">鼠标悬停饼图扇区可查看具体来源标签</span>
      </div>
    </el-card>

    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="card-head">
            <span>出口商品大类结构（占比 %）</span>
            <el-tag size="mini" type="info">结构占比</el-tag>
          </div>
          <div class="chart-shell" :style="shellStyle">
            <div ref="pieExportCat" class="chart-dom" :style="chartDomStyle" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="card-head">
            <span>进口商品大类结构（占比 %）</span>
            <el-tag size="mini" type="info">结构占比</el-tag>
          </div>
          <div class="chart-shell" :style="shellStyle">
            <div ref="pieImportCat" class="chart-dom" :style="chartDomStyle" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="card-head">
            <span>出口区域分布（占比 %）</span>
            <el-tag size="mini" type="success">国别/区域</el-tag>
          </div>
          <div class="chart-shell" :style="shellStyle">
            <div ref="pieExportReg" class="chart-dom" :style="chartDomStyle" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="card-head">
            <span>进口区域分布（占比 %）</span>
            <el-tag size="mini" type="success">国别/区域</el-tag>
          </div>
          <div class="chart-shell" :style="shellStyle">
            <div ref="pieImportReg" class="chart-dom" :style="chartDomStyle" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="source-card" shadow="never">
      <p class="source-line">
        <strong>数据来源说明：</strong>{{ dataset.meta.description }}
      </p>
      <p class="source-links">
        可参考：<span v-for="(u, i) in dataset.meta.sources" :key="i" class="src-item">{{ u }}</span>
      </p>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import tradeData from '../data/china-trade-stats.json'

const COLOR_PALETTE = ['#165DFF', '#00B4FF', '#36D399', '#FBBF24', '#FB7185', '#A78BFA', '#64748B', '#2DD4BF']

function provenanceLabel(p) {
  if (p === 'chain_anchor_demo') return '链上可验证'
  return '官方统计口径'
}

function buildPieOption(title, rows, subtitle) {
  const safeRows = Array.isArray(rows) && rows.length > 0 ? rows : [{ name: '暂无数据', value: 100, provenance: 'official_stat', hint: '请检查 china-trade-stats.json' }]
  const data = safeRows.map((r) => {
    const isChain = r.provenance === 'chain_anchor_demo'
    return {
      name: r.name,
      value: r.value,
      provenance: r.provenance || 'official_stat',
      itemStyle: isChain
        ? {
            borderColor: '#10b981',
            borderWidth: 3,
            shadowBlur: 10,
            shadowColor: 'rgba(16, 185, 129, 0.35)'
          }
        : {
            borderColor: '#fff',
            borderWidth: 2
          }
    }
  })
  return {
    color: COLOR_PALETTE,
    title: {
      text: title,
      subtext: subtitle || '',
      left: 'center',
      top: 8,
      textStyle: { fontSize: 14, fontWeight: 600, color: '#1d2129' },
      subtextStyle: { fontSize: 11, color: '#86909c' }
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(22, 93, 255, 0.5)',
      textStyle: { color: '#e8f4ff' },
      formatter: (p) => {
        const h = safeRows.find((x) => x.name === p.name)
        const tip = h && h.hint ? `<div style="opacity:.85;font-size:11px;margin-top:4px">${h.hint}</div>` : ''
        const pv = h && h.provenance ? provenanceLabel(h.provenance) : provenanceLabel('official_stat')
        const badge =
          h && h.provenance === 'chain_anchor_demo'
            ? '<span style="display:inline-block;margin-top:6px;padding:2px 8px;border-radius:10px;background:rgba(16,185,129,.2);color:#86efac;font-size:11px">链上可信层</span>'
            : '<span style="display:inline-block;margin-top:6px;padding:2px 8px;border-radius:10px;background:rgba(22,93,255,.12);color:#93c5fd;font-size:11px">官方统计</span>'
        return `${p.marker} ${p.name}<br/>占比：<b>${p.value}%</b><br/><span style="font-size:11px;color:#94a3b8">来源：${pv}</span>${badge}${tip}`
      }
    },
    legend: {
      type: 'scroll',
      orient: 'horizontal',
      bottom: 4,
      left: 'center',
      textStyle: { fontSize: 11, color: '#4e5969' }
    },
    animation: true,
    series: [
      {
        name: title,
        type: 'pie',
        radius: ['36%', '64%'],
        center: ['50%', '52%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2,
          shadowBlur: 12,
          shadowColor: 'rgba(22, 93, 255, 0.15)'
        },
        label: {
          formatter: '{b}\n{d}%',
          fontSize: 11,
          color: '#4e5969'
        },
        labelLine: { length: 12, length2: 8 },
        emphasis: {
          scale: true,
          scaleSize: 6,
          itemStyle: {
            shadowBlur: 24,
            shadowColor: 'rgba(22, 93, 255, 0.35)'
          }
        },
        data
      }
    ]
  }
}

export default {
  name: 'TradeVisualizationPage',
  data() {
    return {
      dataset: tradeData,
      chartHeightPx: 380,
      charts: {
        exportCat: null,
        importCat: null,
        exportReg: null,
        importReg: null
      },
      _ro: null,
      _shellObservers: [],
      _initTimer: null
    }
  },
  computed: {
    shellStyle() {
      return { height: this.chartHeightPx + 'px' }
    },
    /** 与 shell 同高，避免嵌套在 el-card 内 height:100% 不生效导致 ECharts 0 高度 */
    chartDomStyle() {
      return {
        width: '100%',
        height: this.chartHeightPx + 'px',
      }
    },
  },
  watch: {
    chartHeightPx() {
      this.$nextTick(() => this.resizeAll())
    }
  },
  mounted() {
    window.addEventListener('resize', this.onWinResize)
    if (typeof ResizeObserver !== 'undefined') {
      this._ro = new ResizeObserver(() => this.resizeAll())
      const el = this.$el
      if (el) this._ro.observe(el)
    }
    this.scheduleInitCharts()
  },
  /* keep-alive 返回本页时容器常为 0×0 初始化，需延后重绘 */
  activated() {
    this.scheduleInitCharts()
  },
  beforeDestroy() {
    if (this._initTimer) {
      clearTimeout(this._initTimer)
      this._initTimer = null
    }
    this.detachShellObservers()
    window.removeEventListener('resize', this.onWinResize)
    if (this._ro) {
      try {
        if (this.$el) this._ro.unobserve(this.$el)
      } catch (e) { /* noop */ }
      this._ro = null
    }
    Object.keys(this.charts).forEach((k) => {
      const c = this.charts[k]
      if (c) {
        try {
          c.dispose()
        } catch (e) { /* noop */ }
      }
    })
  },
  methods: {
    onWinResize() {
      this.resizeAll()
    },
    resizeAll() {
      Object.keys(this.charts).forEach((k) => {
        const c = this.charts[k]
        if (c) {
          try {
            c.resize()
          } catch (e) { /* noop */ }
        }
      })
    },
    detachShellObservers() {
      if (this._shellObservers && this._shellObservers.length) {
        this._shellObservers.forEach((ro) => {
          try {
            ro.disconnect()
          } catch (e) { /* noop */ }
        })
        this._shellObservers = []
      }
    },
    attachShellObservers() {
      this.detachShellObservers()
      if (typeof ResizeObserver === 'undefined' || !this.$el) return
      this.$el.querySelectorAll('.chart-shell').forEach((shell) => {
        const ro = new ResizeObserver(() => {
          this.resizeAll()
        })
        ro.observe(shell)
        this._shellObservers.push(ro)
      })
    },
    /** 等布局稳定后再 init，避免容器 0×0 导致饼图空白 */
    scheduleInitCharts() {
      if (this._initTimer) clearTimeout(this._initTimer)
      this._initTimer = setTimeout(() => {
        this._initTimer = null
        const run = () => {
          this.initCharts()
          this.$nextTick(() => {
            this.resizeAll()
            this.attachShellObservers()
            const bad = Object.keys(this.charts).some((k) => {
              const c = this.charts[k]
              return !c || c.getWidth() < 4 || c.getHeight() < 4
            })
            if (bad) {
              setTimeout(() => {
                this.initCharts()
                this.resizeAll()
              }, 150)
            }
          })
        }
        this.$nextTick(() => {
          requestAnimationFrame(() => {
            requestAnimationFrame(run)
          })
        })
      }, 60)
    },
    initCharts() {
      const d = this.dataset
      const refs = [
        ['exportCat', this.$refs.pieExportCat, '出口商品大类', d.exportByCategory, '机电、轻工、化工等大类汇总'],
        ['importCat', this.$refs.pieImportCat, '进口商品大类', d.importByCategory, '能源、机电、农产品等大类汇总'],
        ['exportReg', this.$refs.pieExportReg, '出口区域分布', d.exportByRegion, '东盟、欧盟、美国等区域合计'],
        ['importReg', this.$refs.pieImportReg, '进口区域分布', d.importByRegion, '来源地结构示意']
      ]
      const fallbackH = this.chartHeightPx
      refs.forEach(([key, el, t, rows, sub]) => {
        if (!el) return
        if (this.charts[key]) {
          try {
            this.charts[key].dispose()
          } catch (e) { /* noop */ }
        }
        const w = Math.max(
          Math.floor(el.clientWidth || el.offsetWidth || 0),
          Math.floor(el.parentElement ? el.parentElement.clientWidth : 0),
          200
        )
        const h = Math.max(
          Math.floor(el.clientHeight || el.offsetHeight || 0),
          fallbackH,
          200
        )
        const chart = echarts.init(el, null, {
          renderer: 'canvas',
          width: w,
          height: h,
          devicePixelRatio: typeof window !== 'undefined' ? window.devicePixelRatio : 1
        })
        chart.setOption(buildPieOption(t, rows, sub), { notMerge: true })
        chart.resize({ width: w, height: h })
        this.charts[key] = chart
      })
    }
  }
}
</script>

<style scoped>
.trade-viz-page {
  max-width: 1280px;
  margin: 0 auto;
  padding-bottom: 32px;
}

.hero-card {
  margin-bottom: 20px;
  border: none;
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 45%, #f5faff 100%);
  border-radius: 12px;
  overflow: hidden;
}
.hero-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}
.hero-text h2 {
  margin: 0 0 10px;
  font-size: 22px;
  color: #165dff;
  font-weight: 600;
}
.hero-text h2 i {
  margin-right: 8px;
}
.lead {
  margin: 0;
  font-size: 14px;
  line-height: 1.75;
  color: #4e5969;
  max-width: 720px;
}
.hero-controls {
  min-width: 260px;
  flex: 0 1 320px;
}
.ctrl-label {
  display: block;
  font-size: 12px;
  color: #86909c;
  margin-bottom: 6px;
}
.height-slider {
  margin-bottom: 6px;
}
.ctrl-hint {
  font-size: 12px;
  color: #86909c;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 12px;
  border: 1px solid #e5e8ef;
  overflow: hidden;
}
.chart-card >>> .el-card__header {
  padding: 12px 16px;
  background: linear-gradient(90deg, rgba(22, 93, 255, 0.06), transparent);
  border-bottom: 1px solid #e8ecf0;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}

.chart-shell {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  transition: height 0.25s ease;
}
/* 勿用 absolute 铺满：在 el-card / flex 下易出现 0×0，ECharts 画布空白 */
.chart-dom {
  display: block;
  width: 100%;
  min-height: 200px;
  box-sizing: border-box;
}

.source-card {
  margin-top: 8px;
  background: #fafbfc;
  border: 1px dashed #c9cdd4;
}
.source-line,
.source-links {
  margin: 0 0 8px;
  font-size: 13px;
  line-height: 1.65;
  color: #4e5969;
}
.source-links {
  margin-bottom: 0;
  word-break: break-all;
}
.src-item {
  display: inline-block;
  margin-right: 12px;
  color: #165dff;
  font-size: 12px;
}

.trusted-alert {
  margin-top: 14px;
  max-width: 920px;
  border-radius: 10px;
}
.trusted-legend-card {
  margin-bottom: 16px;
  border: 1px dashed rgba(16, 185, 129, 0.45);
  background: linear-gradient(90deg, rgba(240, 253, 244, 0.6), #fff);
  border-radius: 10px;
}
.trusted-legend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 24px;
  font-size: 13px;
  color: #334155;
}
.tl-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.tl-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}
.tl-dot--official {
  background: #165dff;
  box-shadow: 0 0 0 3px rgba(22, 93, 255, 0.2);
}
.tl-dot--chain {
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.25);
}
.tl-hint {
  font-size: 12px;
  color: #64748b;
  margin-left: auto;
}

@media (max-width: 768px) {
  .hero-inner {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
