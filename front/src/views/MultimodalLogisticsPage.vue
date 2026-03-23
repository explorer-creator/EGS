<template>
  <div class="mm-page">
    <el-card class="hero" shadow="hover">
      <h2><i class="el-icon-camera"></i> 多模态物流报单</h2>
      <p class="hero-lead">
        支持<strong>运单截图识别</strong>（千问视觉 <code>qwen-vl</code>）与<strong>语音/自然语言报单</strong>（文本模型解析），
        将结果<strong>结构化写入 TMS / WMS 队列</strong>（后端内存，可对接业务接口）。移动端浏览器同样可拍照上传。
      </p>
      <el-alert type="info" :closable="false" show-icon>
        配置 <code>DASHSCOPE_API_KEY</code> 后可使用云端视觉与文本解析；未配置时将使用本地规则从内容中提取字段。
      </el-alert>
    </el-card>

    <el-tabs v-model="tab" type="border-card">
      <el-tab-pane label="运单截图识别" name="img">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-card shadow="hover" class="panel">
              <div slot="header">上传运单/面单照片</div>
              <el-upload
                class="mm-upload"
                drag
                action=""
                :http-request="uploadImage"
                :show-file-list="false"
                accept="image/*"
              >
                <i class="el-icon-upload"></i>
                <div class="el-upload__text">拖拽图片到此处，或 <em>点击选择</em></div>
                <div slot="tip" class="el-upload__tip">支持 jpg/png/webp，建议 &lt; 7MB</div>
              </el-upload>
              <div v-if="imagePreview" class="preview-box">
                <img :src="imagePreview" alt="preview" />
              </div>
              <el-button size="small" icon="el-icon-refresh" :loading="imgLoading" @click="reparseImage" v-if="lastImageFile">
                重新识别
              </el-button>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="语音 / 文字报单" name="voice">
        <el-card shadow="hover" class="panel">
          <div slot="header">语音转写（浏览器 Web Speech）</div>
          <p class="tip">HTTPS 或 localhost 较稳定；不支持时可直接在下方输入文字。</p>
          <el-button
            type="primary"
            :icon="voiceListening ? 'el-icon-loading' : 'el-icon-microphone'"
            :disabled="!speechSupported"
            @click="toggleVoice"
          >
            {{ voiceListening ? '点击停止' : '开始语音报单' }}
          </el-button>
          <el-alert v-if="!speechSupported" title="当前浏览器不支持语音识别" type="info" :closable="false" class="mt8" />
          <el-input
            v-model="voiceText"
            type="textarea"
            :rows="5"
            placeholder="例如：运单号 WB123，从上海发到苏州，十公斤电子产品"
            class="mt12"
          />
          <el-button type="success" class="mt12" icon="el-icon-cpu" :loading="txtLoading" @click="parseText">
            智能解析为结构化字段
          </el-button>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-card shadow="hover" class="panel form-panel" v-loading="imgLoading || txtLoading">
      <div slot="header" class="panel-head">
        <span>结构化字段（可编辑）</span>
        <el-tag v-if="parseMode" size="mini" :type="parseMode === 'vision' ? 'success' : (parseMode === 'heuristic' ? 'warning' : 'info')">{{ parseModeLabel }}</el-tag>
      </div>
      <el-form ref="formRef" :model="form" label-width="100px" size="small">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="运单号"><el-input v-model="form.waybillNo" /></el-form-item>
            <el-form-item label="发货方"><el-input v-model="form.sender" /></el-form-item>
            <el-form-item label="收货方"><el-input v-model="form.receiver" /></el-form-item>
            <el-form-item label="始发地址"><el-input v-model="form.originAddress" /></el-form-item>
            <el-form-item label="目的地址"><el-input v-model="form.destAddress" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="货物描述"><el-input v-model="form.cargoDescription" /></el-form-item>
            <el-form-item label="重量(kg)"><el-input v-model="form.weightKg" /></el-form-item>
            <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card class="commit-card" shadow="hover">
      <div slot="header">提交到业务系统</div>
      <el-button type="primary" icon="el-icon-truck" :loading="commitLoading" @click="commit('tms')">写入 TMS 队列</el-button>
      <el-button type="success" icon="el-icon-box" :loading="commitLoading" @click="commit('wms')">写入 WMS 队列</el-button>
      <el-button icon="el-icon-refresh" @click="loadRecent">刷新最近记录</el-button>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="hover">
          <div slot="header">TMS 最近提交</div>
          <el-table :data="recentTms" size="small" max-height="240" empty-text="暂无">
            <el-table-column prop="id" label="ID" width="130" show-overflow-tooltip />
            <el-table-column prop="committedAt" label="时间" width="160" />
            <el-table-column label="运单号">
              <template slot-scope="s">{{ s.row.payload && s.row.payload.waybillNo }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div slot="header">WMS 最近提交</div>
          <el-table :data="recentWms" size="small" max-height="240" empty-text="暂无">
            <el-table-column prop="id" label="ID" width="130" show-overflow-tooltip />
            <el-table-column prop="committedAt" label="时间" width="160" />
            <el-table-column label="运单号">
              <template slot-scope="s">{{ s.row.payload && s.row.payload.waybillNo }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
const emptyForm = () => ({
  waybillNo: '',
  sender: '',
  receiver: '',
  originAddress: '',
  destAddress: '',
  cargoDescription: '',
  weightKg: '',
  phone: '',
  remark: ''
})

export default {
  name: 'MultimodalLogisticsPage',
  data() {
    return {
      tab: 'img',
      form: emptyForm(),
      imgLoading: false,
      txtLoading: false,
      commitLoading: false,
      parseMode: '',
      parseMessage: '',
      imagePreview: '',
      lastImageFile: null,
      voiceText: '',
      voiceListening: false,
      recognition: null,
      recentTms: [],
      recentWms: []
    }
  },
  computed: {
    speechSupported() {
      return typeof window !== 'undefined' && (window.SpeechRecognition || window.webkitSpeechRecognition)
    },
    parseModeLabel() {
      const m = { vision: '视觉识别', text: '文本解析', heuristic: '辅助解析', demo: '辅助解析' }
      return m[this.parseMode] || this.parseMode
    }
  },
  mounted() {
    this.loadRecent()
  },
  beforeDestroy() {
    this.stopVoice()
  },
  methods: {
    applyStructured(d) {
      if (!d || typeof d !== 'object') return
      this.form = { ...emptyForm(), ...d }
    },
    async uploadImage(req) {
      const raw = req.file
      this.lastImageFile = raw
      if (raw && raw.type && raw.type.startsWith('image/')) {
        const reader = new FileReader()
        reader.onload = (e) => {
          this.imagePreview = e.target.result
        }
        reader.readAsDataURL(raw)
      }
      await this.doParseImage(raw)
    },
    async reparseImage() {
      if (this.lastImageFile) await this.doParseImage(this.lastImageFile)
    },
    async doParseImage(file) {
      if (!file) return
      this.imgLoading = true
      this.parseMessage = ''
      try {
        const fd = new FormData()
        fd.append('file', file)
        const res = await this.$axios.post('/api/logistics-multimodal/parse-image', fd, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        const d = res.data || res
        if (d.success && d.structured) {
          this.applyStructured(d.structured)
          this.parseMode = d.mode || ''
          this.parseMessage = d.message || ''
          this.$message.success('识别完成，可编辑后提交')
        } else {
          this.$message.error(d.message || '识别失败')
        }
      } catch (e) {
        this.$message.error((e.response && e.response.data && e.response.data.message) || e.message || '请求失败')
      } finally {
        this.imgLoading = false
      }
    },
    toggleVoice() {
      if (this.voiceListening) {
        this.stopVoice()
        return
      }
      const SR = window.SpeechRecognition || window.webkitSpeechRecognition
      if (!SR) return
      const r = new SR()
      r.lang = 'zh-CN'
      r.continuous = true
      r.interimResults = true
      r.onresult = (ev) => {
        let t = ''
        for (let i = 0; i < ev.results.length; i++) {
          t += ev.results[i][0].transcript
        }
        this.voiceText = t
      }
      r.onerror = () => {
        this.voiceListening = false
      }
      r.onend = () => {
        if (this.voiceListening) {
          try {
            r.start()
          } catch (e) {
            this.voiceListening = false
          }
        }
      }
      this.recognition = r
      this.voiceListening = true
      try {
        r.start()
      } catch (e) {
        this.voiceListening = false
        this.$message.warning('无法启动语音识别')
      }
    },
    stopVoice() {
      this.voiceListening = false
      if (this.recognition) {
        try {
          this.recognition.stop()
        } catch (e) { /* noop */ }
        this.recognition = null
      }
    },
    async parseText() {
      const text = (this.voiceText || '').trim()
      if (!text) {
        this.$message.warning('请先输入或说出报单内容')
        return
      }
      this.txtLoading = true
      this.parseMessage = ''
      try {
        const res = await this.$axios.post('/api/logistics-multimodal/parse-text', { text })
        const d = res.data || res
        if (d.success && d.structured) {
          this.applyStructured(d.structured)
          this.parseMode = d.mode || ''
          this.parseMessage = d.message || ''
          this.$message.success('解析完成')
        } else {
          this.$message.error(d.message || '解析失败')
        }
      } catch (e) {
        this.$message.error((e.response && e.response.data && e.response.data.message) || e.message || '请求失败')
      } finally {
        this.txtLoading = false
      }
    },
    async commit(target) {
      this.commitLoading = true
      try {
        const res = await this.$axios.post('/api/logistics-multimodal/commit', {
          target,
          record: { ...this.form }
        })
        const d = res.data || res
        if (d.success) {
          this.$message.success(d.message || '已提交')
          await this.loadRecent()
        } else {
          this.$message.error(d.message || '失败')
        }
      } catch (e) {
        this.$message.error(e.message || '提交失败')
      } finally {
        this.commitLoading = false
      }
    },
    async loadRecent() {
      try {
        const [tms, wms] = await Promise.all([
          this.$axios.get('/api/logistics-multimodal/recent', { params: { target: 'tms' } }),
          this.$axios.get('/api/logistics-multimodal/recent', { params: { target: 'wms' } })
        ])
        const a = tms.data || tms
        const b = wms.data || wms
        this.recentTms = (a.items || []).slice(0, 20)
        this.recentWms = (b.items || []).slice(0, 20)
      } catch (e) {
        /* noop */
      }
    }
  }
}
</script>

<style scoped>
.mm-page {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 32px;
}
.hero h2 {
  margin: 0 0 10px;
  font-size: 1.25rem;
  color: #165dff;
}
.hero-lead {
  margin: 0 0 12px;
  line-height: 1.75;
  color: #606266;
  font-size: 0.95rem;
}
.panel {
  margin-bottom: 12px;
}
.form-panel {
  margin-top: 4px;
}
.panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mm-upload {
  width: 100%;
}
.preview-box {
  margin: 12px 0;
  max-height: 220px;
  overflow: hidden;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}
.preview-box img {
  width: 100%;
  display: block;
  object-fit: contain;
  max-height: 220px;
  background: #fafafa;
}
.parse-msg {
  font-size: 12px;
  color: #e6a23c;
  margin: 0 0 10px;
  line-height: 1.5;
}
.commit-card {
  margin: 16px 0;
}
.tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin: 0 0 8px;
}
.mt8 {
  margin-top: 8px;
}
.mt12 {
  margin-top: 12px;
}
</style>
