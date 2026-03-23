<template>
  <div class="product-manufacturing-page">
    <el-card class="qa-card" shadow="hover">
      <div class="qa-header">
        <i class="el-icon-s-opportunity"></i>
        <h2>产品制造知识问答</h2>
        <span class="subtitle">输入产品名称（如耳机、充电宝等），AI 将输出原材料、设备、工序、工艺流程、物理原理，并展示核心配件图、B 站视频及优秀企业推荐</span>
      </div>

      <div class="qa-body">
        <div class="input-area">
          <el-input
            v-model="productInput"
            placeholder="请输入产品名称，例如：耳机、充电宝、锂电池、行星减速器..."
            size="large"
            clearable
            @keyup.enter.native="submitQuery"
          >
            <el-button slot="append" type="primary" :loading="loading" @click="submitQuery">
              查询
            </el-button>
          </el-input>
        </div>

        <div v-if="errorMsg" class="error-msg">
          <i class="el-icon-warning"></i> {{ errorMsg }}
        </div>

        <div v-if="loading" class="loading-area">
          <i class="el-icon-loading"></i> 正在调用千问模型生成回答...
        </div>

        <div v-else-if="lastAnswer" class="answer-area">
          <div class="answer-header">
            <span class="product-tag">{{ lastProduct }}</span>
            <span class="answer-time">回答时间：{{ answerTime }}</span>
          </div>
          <div
            class="answer-content term-selectable"
            v-html="renderedContent"
            @dblclick="onAnswerDblclick"
          ></div>
          <p class="term-hint">
            <i class="el-icon-info"></i> 提示：遇到不懂的专业名词？<strong>双击该词语</strong>可呼唤左下角小机器人进行解释
          </p>

          <div v-if="componentList.length > 0" class="component-section">
            <h4 class="component-title">
              <i class="el-icon-picture-outline"></i> 核心配件展示
            </h4>
            <p class="component-desc">该产品涉及的核心零部件外观示意</p>
            <div class="component-gallery">
              <div v-for="(item, idx) in componentList" :key="idx" class="component-card">
                <div class="component-img-wrap">
                  <img :src="item.imageUrl" :alt="item.name" @error="onImgError($event)" />
                </div>
                <div class="component-name">{{ item.name }}</div>
                <p class="component-detail">{{ item.desc }}</p>
              </div>
            </div>
          </div>

          <div v-if="videoData" class="video-section">
            <h4 class="video-title">
              <i class="el-icon-video-play"></i> 相关工艺讲解视频
            </h4>
            <p v-if="videoData.title" class="video-desc">{{ videoData.title }}</p>
            <div class="video-wrapper">
              <iframe
                :src="videoData.embedUrl"
                frameborder="0"
                allowfullscreen
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              ></iframe>
            </div>
            <a v-if="videoData.pageUrl" :href="videoData.pageUrl" target="_blank" rel="noopener" class="video-link">
              在 B 站打开观看 <i class="el-icon-right"></i>
            </a>
          </div>

          <!-- 相关链接：跳转到绿能排产演示、生成综合报告、生成生产方案 -->
          <div class="related-links-section">
            <h4 class="related-links-title">
              <i class="el-icon-link"></i> 延伸阅读
            </h4>
            <p class="related-links-desc">与「{{ lastProduct }}」相关的功能模块，可进一步探索：</p>
            <div class="related-links-btns">
              <el-button type="success" size="small" icon="el-icon-data-analysis" @click="goToGreenCarbonDemo">
                绿能排产演示
              </el-button>
              <el-button type="primary" size="small" icon="el-icon-document-checked" @click="goToReportExport">
                生成综合报告
              </el-button>
              <el-button type="warning" size="small" icon="el-icon-document-checked" @click="goToProductionPlan">
                生成生产方案并导入
              </el-button>
            </div>
          </div>

          <div v-if="companyList.length > 0" class="company-section">
            <h4 class="company-title">
              <i class="el-icon-office-building"></i> 相关优秀企业推荐
            </h4>
            <p class="company-desc">以下企业涉及该产品的核心配件或工艺，供参考对接</p>
            <el-row :gutter="16">
              <el-col v-for="(c, idx) in companyList" :key="idx" :xs="24" :sm="12" :md="8">
                <div class="company-card">
                  <div class="company-name">{{ c.name }}</div>
                  <p class="company-intro">{{ c.intro }}</p>
                  <div class="company-contact">
                    <i class="el-icon-connection"></i> {{ c.contact }}
                  </div>
                  <a v-if="c.website" :href="c.website" target="_blank" rel="noopener" class="company-link">
                    访问官网 <i class="el-icon-right"></i>
                  </a>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>

        <div v-else class="placeholder-area">
          <i class="el-icon-chat-dot-round"></i>
          <p>在上方输入产品名称并点击「查询」，获取该产品的制造相关信息</p>
          <p class="hint">示例：耳机、充电宝、锂电池、行星减速器、PCB 电路板</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
/**
 * 简单 Markdown 转 HTML：支持 **粗体**、## 标题、- 列表、换行
 */
function simpleMarkdownToHtml(text) {
  if (!text || typeof text !== 'string') return ''
  const lines = text.split('\n')
  const out = []
  let inList = false
  for (let i = 0; i < lines.length; i++) {
    let line = lines[i]
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
    if (/^###? .+/.test(line)) {
      if (inList) { out.push('</ul>'); inList = false }
      const tag = line.startsWith('###') ? 'h4' : 'h3'
      out.push('<' + tag + '>' + line.replace(/^#+ /, '') + '</' + tag + '>')
    } else if (/^- .+/.test(line) || /^\d+\.\s+.+/.test(line)) {
      if (!inList) { out.push('<ul>'); inList = true }
      const content = line.replace(/^- /, '').replace(/^\d+\.\s+/, '')
      out.push('<li>' + content + '</li>')
    } else {
      if (inList) { out.push('</ul>'); inList = false }
      if (line.trim()) out.push(line + '<br/>')
      else out.push('<br/>')
    }
  }
  if (inList) out.push('</ul>')
  return out.join('')
}

export default {
  name: 'ProductManufacturingPage',
  inject: {
    robotExplain: { default: null },
    navigateToPage: { default: null }
  },
  data() {
    return {
      productInput: '',
      loading: false,
      errorMsg: '',
      lastAnswer: '',
      lastProduct: '',
      answerTime: '',
      videoData: null,
      companyList: [],
      componentList: []
    }
  },
  computed: {
    renderedContent() {
      return simpleMarkdownToHtml(this.lastAnswer)
    }
  },
  methods: {
    async submitQuery() {
      const product = (this.productInput || '').trim()
      if (!product) {
        this.$message.warning('请输入产品名称')
        return
      }

      this.loading = true
      this.errorMsg = ''
      this.lastAnswer = ''
      this.videoData = null
      this.companyList = []
      this.componentList = []

      try {
        const res = await this.$axios.post('/api/llm/product-query', { product })
        const data = res.data || res

        if (data.success) {
          this.lastAnswer = data.content || ''
          this.lastProduct = data.product || product
          this.answerTime = new Date().toLocaleString('zh-CN')
          this.fetchVideo(product)
          this.fetchCompanies(product)
          this.fetchComponents(product)
        } else {
          this.errorMsg = data.message || '查询失败'
          this.$message.error(this.errorMsg)
        }
      } catch (e) {
        this.errorMsg = e.response?.data?.message || e.message || '网络请求失败'
        this.$message.error(this.errorMsg)
      } finally {
        this.loading = false
      }
    },
    async fetchVideo(product) {
      try {
        const res = await this.$axios.get('/api/llm/product-video', { params: { product } })
        const data = res.data || res
        if (data.success && data.embedUrl) {
          this.videoData = {
            embedUrl: data.embedUrl,
            pageUrl: data.pageUrl,
            title: data.title
          }
        }
      } catch (e) {
        // 视频获取失败不影响主流程，静默忽略
      }
    },
    async fetchCompanies(product) {
      try {
        const res = await this.$axios.get('/api/llm/product-companies', { params: { product } })
        const data = res.data || res
        if (data.success && Array.isArray(data.companies)) {
          this.companyList = data.companies
        }
      } catch (e) {
        // 企业推荐获取失败不影响主流程，静默忽略
      }
    },
    async fetchComponents(product) {
      try {
        const res = await this.$axios.get('/api/llm/product-components', { params: { product } })
        const data = res.data || res
        if (data.success && Array.isArray(data.components)) {
          this.componentList = data.components
        }
      } catch (e) {
        // 配件展示获取失败不影响主流程，静默忽略
      }
    },
    onImgError(e) {
      e.target.style.display = 'none'
      e.target.parentElement.classList.add('img-failed')
    },
    goToGreenCarbonDemo() {
      const nav = this.navigateToPage
      if (nav) nav('GreenCarbonFootprintPage', { product: this.lastProduct, lastAnswer: this.lastAnswer })
    },
    goToReportExport() {
      const nav = this.navigateToPage
      if (nav) nav('ReportExportPage', { product: this.lastProduct, lastAnswer: this.lastAnswer })
    },
    goToProductionPlan() {
      const nav = this.navigateToPage
      if (nav) nav('BusinessAnalysisPage', { openProductionPlan: true, product: this.lastProduct })
    },
    onAnswerDblclick() {
      this.$nextTick(() => {
        const sel = window.getSelection()
        const text = (sel && sel.toString ? sel.toString() : '').trim()
        if (text && text.length <= 30 && this.robotExplain) {
          this.robotExplain(text)
        }
      })
    }
  }
}
</script>

<style scoped>
.product-manufacturing-page {
  max-width: 900px;
  margin: 0 auto;
}
.qa-card {
  padding: 28px 36px;
}
.qa-header {
  margin-bottom: 24px;
  padding-bottom: 18px;
  border-bottom: 2px solid #165DFF;
}
.qa-header h2 {
  margin: 0 0 6px 0;
  font-size: 22px;
  color: #303133;
}
.qa-header .subtitle {
  font-size: 13px;
  color: #909399;
  display: block;
  margin-top: 4px;
}
.qa-header i {
  font-size: 26px;
  color: #165DFF;
  margin-right: 8px;
}
.input-area {
  margin-bottom: 20px;
}
.input-area >>> .el-input-group--append .el-input__inner {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}
.input-area >>> .el-input-group__append {
  padding: 0;
  border-radius: 0 4px 4px 0;
}
.input-area >>> .el-input-group__append .el-button {
  margin: 0;
  border-radius: 0 4px 4px 0;
  padding: 12px 24px;
}
.error-msg {
  padding: 12px 16px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 8px;
  color: #f56c6c;
  margin-bottom: 16px;
}
.error-msg i {
  margin-right: 6px;
}
.loading-area {
  padding: 40px;
  text-align: center;
  color: #909399;
  font-size: 15px;
}
.loading-area i {
  font-size: 24px;
  margin-right: 8px;
  vertical-align: middle;
}
.answer-area {
  background: #f9fafc;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 24px;
}
.answer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}
.product-tag {
  font-weight: 600;
  color: #165DFF;
  font-size: 15px;
}
.answer-time {
  font-size: 12px;
  color: #909399;
}
.answer-content {
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
}
.answer-content >>> strong {
  color: #165DFF;
}
.answer-content >>> h2, .answer-content >>> h3, .answer-content >>> h4 {
  margin: 16px 0 8px 0;
  color: #303133;
}
.answer-content >>> h2 { font-size: 18px; }
.answer-content >>> h3 { font-size: 16px; }
.answer-content >>> h4 { font-size: 15px; }
.answer-content >>> ul {
  margin: 8px 0;
  padding-left: 24px;
}
.answer-content >>> li {
  margin-bottom: 4px;
}
.term-selectable {
  user-select: text;
  cursor: text;
}
.term-hint {
  margin: 12px 0 0 0;
  padding: 8px 12px;
  background: #ecf5ff;
  border-radius: 6px;
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}
.term-hint i {
  color: #165DFF;
  margin-right: 4px;
}
.term-hint strong {
  color: #165DFF;
}
.component-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
.component-title {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #165DFF;
  display: flex;
  align-items: center;
  gap: 6px;
}
.component-title i {
  font-size: 18px;
}
.component-desc {
  margin: 0 0 16px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
.component-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.component-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}
.component-card:hover {
  box-shadow: 0 2px 12px rgba(22, 93, 255, 0.15);
}
.component-img-wrap {
  width: 100%;
  height: 140px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.component-img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.component-img-wrap.img-failed {
  background: linear-gradient(135deg, #e8f4ff 0%, #f0f9ff 100%);
  color: #165DFF;
  font-size: 48px;
  line-height: 140px;
  text-align: center;
}
.component-img-wrap.img-failed::before {
  content: '◆';
  opacity: 0.5;
}
.component-name {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  padding: 10px 12px 4px;
}
.component-detail {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin: 0;
  padding: 0 12px 12px;
}
.placeholder-area {
  padding: 60px 40px;
  text-align: center;
  color: #909399;
}
.placeholder-area i {
  font-size: 48px;
  color: #dcdfe6;
  margin-bottom: 16px;
  display: block;
}
.placeholder-area p {
  margin: 8px 0;
  font-size: 14px;
}
.placeholder-area .hint {
  font-size: 12px;
  color: #c0c4cc;
}
.video-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
.video-title {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #165DFF;
  display: flex;
  align-items: center;
  gap: 6px;
}
.video-title i {
  font-size: 18px;
}
.video-desc {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
.video-wrapper {
  position: relative;
  width: 100%;
  padding-bottom: 56.25%;
  height: 0;
  overflow: hidden;
  border-radius: 8px;
  background: #000;
}
.video-wrapper iframe {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
.video-link {
  display: inline-block;
  margin-top: 10px;
  font-size: 13px;
  color: #165DFF;
  text-decoration: none;
}
.video-link:hover {
  text-decoration: underline;
}
.video-link i {
  font-size: 12px;
  margin-left: 4px;
}
.related-links-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
.related-links-title {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #165DFF;
  display: flex;
  align-items: center;
  gap: 6px;
}
.related-links-title i {
  font-size: 18px;
}
.related-links-desc {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
.related-links-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.company-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
.company-title {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #165DFF;
  display: flex;
  align-items: center;
  gap: 6px;
}
.company-title i {
  font-size: 18px;
}
.company-desc {
  margin: 0 0 16px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
.company-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  transition: box-shadow 0.2s;
}
.company-card:hover {
  box-shadow: 0 2px 12px rgba(22, 93, 255, 0.15);
}
.company-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
  margin-bottom: 8px;
}
.company-intro {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 10px 0;
}
.company-contact {
  font-size: 12px;
  color: #165DFF;
  display: flex;
  align-items: center;
  gap: 4px;
}
.company-contact i {
  font-size: 14px;
}
.company-link {
  display: inline-block;
  margin-top: 8px;
  font-size: 12px;
  color: #165DFF;
  text-decoration: none;
}
.company-link:hover {
  text-decoration: underline;
}
.company-link i {
  font-size: 12px;
  margin-left: 2px;
}
</style>
