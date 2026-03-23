<template>
  <div class="ab-hub">
    <el-card class="hero" shadow="hover">
      <h2><i class="el-icon-connection"></i> AI + 区块链集成</h2>
      <p class="lead">
        <strong>摘要上链指纹</strong>：AI 生成运单风险摘要，仅将摘要的 SHA-256 作为可审计指纹（链上交易标识）。
        <strong>预测与 SLA</strong>：延误概率超阈值时触发「合约约定」通知与链上事件（当前为<strong>内存 + 接口日志</strong>，可替换为真实合约）。
        贸易大屏已增加<strong>可信数据层</strong>标识，见菜单「贸易可视化」。
      </p>
    </el-card>

    <el-card shadow="never" class="why-chain-card">
      <div slot="header" class="why-head">
        <span><i class="el-icon-question"></i> 区块链在物流里适合做什么（不为用链而用链）</span>
      </div>
      <p class="why-lead">
        链不是替代数据库，而是为<strong>多方协作</strong>下的关键事实提供<strong>不可单方篡改的顺序与一致性</strong>；宜<strong>哈希上链、原文链下</strong>，控制成本与隐私。
      </p>
      <el-table :data="whyChainRows" border size="small" stripe class="why-table">
        <el-table-column prop="dir" label="方向" min-width="120" />
        <el-table-column prop="value" label="价值" min-width="220" />
        <el-table-column prop="silk" label="与丝路 / 跨境的关系" min-width="240" />
      </el-table>
      <el-alert type="warning" :closable="false" show-icon class="why-tip">
        <template slot="title">落地建议</template>
        <strong>联盟链 + 业务哈希上链</strong>（勿把全量业务数据上链）。先选一条「<strong>跨境一单多段</strong>」或「<strong>高价值 SKU</strong>」做溯源试点；合约与结算可先<strong>半自动 + 链上存证</strong>，再逐步合约化。
        详细说明见项目文档 <code>docs/区块链与物流-适用场景.md</code>。
      </el-alert>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="panel">
          <div slot="header"><i class="el-icon-document"></i> 运单摘要 → 链上指纹</div>
          <el-form size="small" label-width="88px">
            <el-form-item label="运单号">
              <el-input v-model="anchorForm.waybillNo" placeholder="默认虚拟运单号，可改；清空则后端自动生成" clearable />
            </el-form-item>
            <el-form-item label="附加上下文">
              <el-input v-model="anchorForm.context" type="textarea" :rows="2" placeholder="可选，参与摘要生成" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="anchorLoading" @click="submitAnchor">生成摘要并锚定哈希</el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="anchorResult" type="info" :closable="false" class="result-box">
            <p v-if="anchorResult.waybillNo" class="waybill-line">运单号：<strong>{{ anchorResult.waybillNo }}</strong></p>
            <div class="hash-row">
              <span class="hash-label">SHA-256（摘要指纹，可比对）</span>
              <el-button type="text" size="mini" icon="el-icon-document" @click="copyText(anchorResult.summarySha256)">复制</el-button>
            </div>
            <el-input type="textarea" :rows="2" readonly :value="anchorResult.summarySha256" class="hash-input" />
            <div class="hash-row">
              <span class="hash-label">链上交易标识（mockTxId）</span>
              <el-button type="text" size="mini" icon="el-icon-document" @click="copyText(anchorResult.mockChainTxId)">复制</el-button>
            </div>
            <el-input readonly :value="anchorResult.mockChainTxId" class="hash-input hash-input--single" />
            <pre class="json-pre">{{ pretty(anchorResult.aiSummary) }}</pre>
          </el-alert>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="panel">
          <div slot="header"><i class="el-icon-warning-outline"></i> 延误预测 & SLA 联动</div>
          <p class="hint">点击下方将随机生成延误概率（或手动指定）；超过阈值则写入 SLA 事件列表。</p>
          <el-form inline size="small">
            <el-form-item label="运单号">
              <el-input v-model="slaForm.waybillNo" placeholder="默认虚拟，可改" style="width:200px" clearable />
            </el-form-item>
            <el-form-item label="阈值">
              <el-input-number v-model="slaForm.threshold" :min="0.05" :max="0.95" :step="0.05" />
            </el-form-item>
            <el-form-item label="指定概率">
              <el-input-number v-model="slaForm.delayProb" :min="0" :max="1" :step="0.05" :disabled="slaRandom" />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="slaRandom">随机概率</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="slaLoading" @click="runSla">执行 SLA 评估</el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="slaOutcome" :type="slaOutcome.slaBreached ? 'error' : 'success'" :closable="false">
            <div>
              <p class="sla-summary-line">
                延误概率：<strong>{{ slaOutcome.delayProbability }}</strong> · 阈值 {{ slaOutcome.threshold }} ·
                {{ slaOutcome.slaBreached ? '已触发通知/链上事件' : '未触发' }}
              </p>
              <p v-if="slaOutcome.slaBreached && slaOutcome.chainTxHash" class="sla-hash-line">
                链上哈希：<code class="mono-inline">{{ slaOutcome.chainTxHash }}</code>
                <el-button type="text" size="mini" @click="copyText(slaOutcome.chainTxHash)">复制</el-button>
              </p>
            </div>
          </el-alert>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="row2">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="panel">
          <div slot="header">最近锚定记录</div>
          <el-button size="mini" icon="el-icon-refresh" @click="loadAnchors">刷新</el-button>
          <el-table :data="anchors" size="mini" border max-height="280" class="mt8">
            <el-table-column prop="waybillNo" label="运单" min-width="120" show-overflow-tooltip />
            <el-table-column label="摘要哈希 SHA-256" min-width="220">
              <template slot-scope="scope">
                <div class="hash-cell">
                  <span class="hash-clip" :title="scope.row.summarySha256">{{ scope.row.summarySha256 }}</span>
                  <el-button type="text" size="mini" @click="copyText(scope.row.summarySha256)">复制</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="链上标识" min-width="160">
              <template slot-scope="scope">
                <span class="hash-clip" :title="scope.row.mockChainTxId">{{ scope.row.mockChainTxId }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="anchoredAt" label="时间" width="160" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="panel">
          <div slot="header">SLA / 链上事件</div>
          <el-button size="mini" icon="el-icon-refresh" @click="loadSlaEvents">刷新</el-button>
          <el-table :data="slaEvents" size="mini" border max-height="280" class="mt8">
            <el-table-column prop="eventId" label="事件ID" width="130" />
            <el-table-column prop="type" label="类型" width="110" />
            <el-table-column prop="waybillNo" label="运单" min-width="120" show-overflow-tooltip />
            <el-table-column label="链上哈希" min-width="200">
              <template slot-scope="scope">
                <div v-if="scope.row.chainTxHash" class="hash-cell">
                  <span class="hash-clip" :title="scope.row.chainTxHash">{{ scope.row.chainTxHash }}</span>
                  <el-button type="text" size="mini" @click="copyText(scope.row.chainTxHash)">复制</el-button>
                </div>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="timestamp" label="时间" width="160" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="link-card">
      <i class="el-icon-pie-chart"></i>
      <strong>贸易可视化：</strong>打开「贸易可视化」饼图，扇区绿色描边为<strong>链上可信层</strong>，蓝色为官方统计口径。
      <el-button type="text" @click="goTrade">跳转贸易可视化</el-button>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'AiBlockchainHubPage',
  inject: ['navigateToPage'],
  data() {
    return {
      whyChainRows: [
        {
          dir: '全程溯源',
          value: '关键节点上链（出厂、质检、装箱、清关、签收等）；链上存哈希，链下存原文（降成本、控隐私）。',
          silk: '跨境电商、海外仓、高价值货；一单多段、多国参与时对齐责任与状态。'
        },
        {
          dir: '单证与协作',
          value: '提单、原产地、报关单等哈希 + 时间戳，授权共享给多方。',
          silk: '减少重复审单、纠纷举证清晰（比对哈希即可验证是否被替换）。'
        },
        {
          dir: '合约与结算',
          value: '对账、里程碑付款、押金/罚金（智能合约或链上存证结算结果）。',
          silk: '承运商、海外仓、货代之间 SLA 自动化；可先半自动 + 链上存证。'
        },
        {
          dir: '身份与权限',
          value: 'DID、设备/车辆身份等可验证凭证。',
          silk: '多机构协同时的可验证身份（偏中长期，与 IAM 结合）。'
        }
      ],
      anchorForm: { waybillNo: 'WB-VIRTUAL-DEMO-001', context: '' },
      anchorLoading: false,
      anchorResult: null,
      anchors: [],
      slaForm: { waybillNo: 'WB-VIRTUAL-DEMO-001', threshold: 0.35, delayProb: 0.5 },
      slaRandom: true,
      slaLoading: false,
      slaOutcome: null,
      slaEvents: []
    }
  },
  mounted() {
    this.loadAnchors()
    this.loadSlaEvents()
  },
  methods: {
    goTrade() {
      if (typeof this.navigateToPage === 'function') this.navigateToPage('TradeVisualizationPage')
    },
    pretty(o) {
      try {
        return JSON.stringify(o, null, 2)
      } catch (e) {
        return String(o)
      }
    },
    copyText(text) {
      const t = text == null ? '' : String(text)
      if (!t) return
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(t).then(() => this.$message.success('已复制到剪贴板')).catch(() => this._copyFallback(t))
      } else {
        this._copyFallback(t)
      }
    },
    _copyFallback(t) {
      try {
        const ta = document.createElement('textarea')
        ta.value = t
        ta.style.position = 'fixed'
        ta.style.left = '-9999px'
        document.body.appendChild(ta)
        ta.select()
        document.execCommand('copy')
        document.body.removeChild(ta)
        this.$message.success('已复制到剪贴板')
      } catch (e) {
        this.$message.warning('复制失败，请手动选择文本')
      }
    },
    /** 解析 axios 错误，403 常因 CORS 未包含当前 Vite 端口 */
    apiErrorMessage(e) {
      const status = e.response && e.response.status
      const data = e.response && e.response.data
      let detail = ''
      if (typeof data === 'string') detail = data
      else if (data && typeof data.message === 'string') detail = data.message
      else if (data && typeof data.error === 'string') detail = data.error
      const base = e.message || '网络错误'
      let msg = base + (status ? `（HTTP ${status}）` : '')
      if (detail) msg += '：' + detail
      if (status === 403) {
        msg +=
          '。若前端非 5173 端口，请在 Java 后端 CORS 中放行当前地址（如 localhost:5176）后重启代理。'
      }
      return msg
    },
    async submitAnchor() {
      this.anchorLoading = true
      this.anchorResult = null
      try {
        const body = {}
        const wb = (this.anchorForm.waybillNo || '').trim()
        if (wb) body.waybillNo = wb
        if (this.anchorForm.context) body.context = this.anchorForm.context
        const res = await this.$axios.post('/api/ai-chain/anchor-waybill', body)
        const d = res.data || {}
        if (d.success && d.record) {
          this.anchorResult = d.record
          this.$message.success('摘要已生成，哈希可比对上链记录')
          this.loadAnchors()
        } else {
          this.$message.warning('返回异常')
        }
      } catch (e) {
        this.$message.error('请求失败：' + this.apiErrorMessage(e))
      } finally {
        this.anchorLoading = false
      }
    },
    async loadAnchors() {
      try {
        const res = await this.$axios.get('/api/ai-chain/anchors', { params: { limit: 30 } })
        this.anchors = (res.data && res.data.items) || []
      } catch (e) {
        this.anchors = []
      }
    },
    async runSla() {
      this.slaLoading = true
      this.slaOutcome = null
      try {
        const body = {}
        const swb = (this.slaForm.waybillNo || '').trim()
        if (swb) body.waybillNo = swb
        body.threshold = this.slaForm.threshold
        if (!this.slaRandom && this.slaForm.delayProb != null) body.delayProb = this.slaForm.delayProb
        const res = await this.$axios.post('/api/ai-chain/sla-evaluate', body)
        this.slaOutcome = res.data || {}
        if (this.slaOutcome.slaBreached) this.$message.warning('SLA 风险：已记录链上事件')
        else this.$message.success('未超阈值')
        this.loadSlaEvents()
      } catch (e) {
        this.$message.error('请求失败：' + this.apiErrorMessage(e))
      } finally {
        this.slaLoading = false
      }
    },
    async loadSlaEvents() {
      try {
        const res = await this.$axios.get('/api/ai-chain/sla-events', { params: { limit: 40 } })
        this.slaEvents = (res.data && res.data.items) || []
      } catch (e) {
        this.slaEvents = []
      }
    }
  }
}
</script>

<style scoped>
.ab-hub {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 24px;
}
.hero {
  margin-bottom: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f0fdf4 0%, #eff6ff 50%, #faf5ff 100%);
}
.hero h2 {
  margin: 0 0 10px;
  color: #0f172a;
  font-size: 20px;
}
.lead {
  margin: 0;
  font-size: 14px;
  line-height: 1.75;
  color: #475569;
}
.panel {
  margin-bottom: 16px;
  border-radius: 10px;
}
.row2 {
  margin-bottom: 8px;
}
.hint {
  font-size: 12px;
  color: #64748b;
  margin: 0 0 10px;
}
.sla-summary-line {
  margin: 0;
  line-height: 1.6;
}
.sla-hash-line {
  margin: 10px 0 0;
  font-size: 12px;
  word-break: break-all;
}
.mono-inline {
  font-family: ui-monospace, Consolas, monospace;
  font-size: 11px;
}
.result-box {
  margin-top: 12px;
}
.mono {
  font-family: ui-monospace, monospace;
  font-size: 11px;
  word-break: break-all;
  margin-bottom: 8px;
  color: #334155;
}
.json-pre {
  margin: 8px 0 0;
  padding: 10px;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 8px;
  font-size: 11px;
  max-height: 160px;
  overflow: auto;
}
.mt8 {
  margin-top: 8px;
}
.waybill-line {
  margin: 0 0 10px;
  font-size: 13px;
  color: #334155;
}
.hash-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.hash-label {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}
.hash-input {
  margin-bottom: 10px;
}
.hash-input >>> textarea,
.hash-input >>> input {
  font-family: ui-monospace, Consolas, monospace;
  font-size: 11px;
}
.hash-input--single >>> input {
  font-size: 11px;
}
.hash-cell {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}
.hash-clip {
  flex: 1;
  min-width: 0;
  font-family: ui-monospace, Consolas, monospace;
  font-size: 10px;
  word-break: break-all;
  line-height: 1.35;
}
.muted {
  color: #94a3b8;
  font-size: 12px;
}
.ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: ui-monospace, monospace;
  font-size: 10px;
}
.link-card {
  border-radius: 10px;
  font-size: 13px;
  color: #475569;
}
.why-chain-card {
  margin-bottom: 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}
.why-head {
  font-weight: 600;
  color: #0f172a;
}
.why-lead {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.7;
  color: #475569;
}
.why-table {
  margin-bottom: 12px;
}
.why-tip {
  margin-top: 4px;
  border-radius: 8px;
}
</style>
