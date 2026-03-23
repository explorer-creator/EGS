<template>
  <section class="mg-wrapper">
    <el-card shadow="hover" class="mg-demo-card">
      <div slot="header" class="mg-demo-header">
        <span><i class="el-icon-view"></i> 参考配图与物料字段</span>
      </div>
      <el-row :gutter="20">
        <el-col :xs="24" :md="14">
          <div class="mg-demo-img-wrap">
            <img :src="demoImageUrl" alt="工业物料示意" class="mg-demo-img" />
            <span class="mg-demo-img-badge">三坐标测量（示意）</span>
          </div>
        </el-col>
        <el-col :xs="24" :md="10">
          <div class="mg-demo-fields-title">结构化物料字段</div>
          <el-descriptions :column="1" size="small" border class="mg-demo-desc">
            <el-descriptions-item v-for="row in demoStructuredRows" :key="row.label" :label="row.label">
              {{ row.value }}
            </el-descriptions-item>
          </el-descriptions>
        </el-col>
      </el-row>
    </el-card>

    <div class="mg-header">
      <div class="mg-icon-badge">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <rect x="3" y="3" width="18" height="18" rx="3" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <polyline points="21 15 16 10 5 21" />
        </svg>
      </div>
      <div>
        <h2 class="mg-title">智能物料配图（模块 A）</h2>
        <p class="mg-subtitle">关键词 → 网页候选抓取 · 校验过滤 · 通义万相生图</p>
      </div>
    </div>

    <div class="mg-chips">
      <span class="mg-chips-label">快捷词：</span>
      <el-tag v-for="t in quickTags" :key="t" size="small" effect="plain" class="mg-chip" @click.native="keyword = t">
        {{ t }}
      </el-tag>
    </div>

    <div class="mg-input-row">
      <div class="mg-input-wrap">
        <el-input
          v-model="keyword"
          placeholder="输入物料/原料/设备关键词，如：行星减速器、液压泵、CNC"
          maxlength="50"
          :disabled="loading"
          clearable
          @keyup.enter.native="handleGenerate"
        />
      </div>
      <el-button type="primary" :loading="loading" :disabled="!keyword.trim()" @click="handleGenerate">
        {{ loading ? '生成中…' : '生成配图' }}
      </el-button>
    </div>

    <el-alert v-if="errorMsg" type="error" :title="errorMsg" :closable="false" show-icon class="mg-alert" />

    <div v-if="loading && !imageUrl" class="mg-skeleton">
      <div class="mg-skeleton-shimmer" />
        <p class="mg-loading-hint">正在准备演示配图…</p>
    </div>

    <div v-if="imageUrl" class="mg-result-card">
      <div class="mg-img-container">
        <img :src="imageUrl" :alt="lastKeyword + ' 配图'" class="mg-img" @load="onImgLoad" @error="onImgError" />
        <div v-if="imgLoaded" class="mg-img-overlay">
          <span class="mg-tag">{{ lastKeyword }}</span>
          <a :href="imageUrl" :download="lastKeyword + '.jpg'" class="mg-download-btn" target="_blank" rel="noopener">下载</a>
        </div>
      </div>
      <div v-if="imgLoaded" class="mg-meta">
        <span>生成成功 · {{ lastKeyword }} · {{ elapsedTime }}s</span>
      </div>
    </div>

    <p class="mg-tips">
      💡 当前为<strong>本地固定演示图</strong>（三坐标测量机工业示意），点击「生成配图」即可展示，无需 Python 服务。若需恢复爬图/通义万相 API，可对接 <code>EGSfb/A/server.py</code>。
    </p>
  </section>
</template>

<script>
/** 本地固定工业示意：public/smart-material-cmm.png（三坐标测量机） */
const STATIC_MATERIAL_IMAGE = '/smart-material-cmm.png'

export default {
  name: 'MaterialGenerator',
  data() {
    return {
      demoImageUrl: STATIC_MATERIAL_IMAGE,
      demoStructuredRows: [
        { label: '物料编码', value: 'MAT-EGS-2025-0881' },
        { label: '物料名称', value: '316L 冷轧不锈钢板' },
        { label: '规格型号', value: '2.0 × 1250 × 2500 mm' },
        { label: '计量单位', value: '张' },
        { label: '图号/版本', value: 'DWG-SH-EGS-17 Rev.B' },
        { label: '默认仓库', value: '华东原料仓 · A区-07 货架' },
        { label: '账面库存', value: '120' },
        { label: '安全库存', value: '40' },
        { label: '质检状态', value: '合格（IQC-2025-0316）' },
        { label: '配图关键词', value: '液压泵' },
        { label: '备注', value: '—' }
      ],
      keyword: '',
      loading: false,
      errorMsg: '',
      imageUrl: '',
      imgLoaded: false,
      lastKeyword: '',
      elapsedTime: '0',
      quickTags: ['液压泵', '伺服电机', '行星齿轮', 'CNC 加工中心', '轴承钢'],
      startTs: 0
    }
  },
  methods: {
    async handleGenerate() {
      const kw = (this.keyword || '').trim()
      if (!kw || this.loading) return
      this.loading = true
      this.errorMsg = ''
      this.imageUrl = ''
      this.imgLoaded = false
      this.lastKeyword = kw
      this.startTs = Date.now()
      try {
        // 固定演示图，避免后端未启动或接口 500 导致空白
        await new Promise((r) => setTimeout(r, 280))
        this.imageUrl = STATIC_MATERIAL_IMAGE
        this.elapsedTime = ((Date.now() - this.startTs) / 1000).toFixed(1)
      } catch (e) {
        this.errorMsg = e.message || '生成失败'
      } finally {
        this.loading = false
      }
    },
    onImgLoad() {
      this.imgLoaded = true
    },
    onImgError() {
      this.errorMsg = '图片加载失败，请确认 public/smart-material-cmm.png 存在'
      this.imageUrl = ''
    }
  }
}
</script>

<style scoped>
.mg-demo-card {
  margin-bottom: 20px;
  border-radius: 14px;
  border: 1px solid #c7d2fe;
  background: linear-gradient(145deg, #f8fafc 0%, #eef2ff 100%);
}
.mg-demo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-weight: 600;
  color: #1e293b;
}
.mg-demo-img-wrap {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #0f172a;
}
.mg-demo-img {
  width: 100%;
  height: auto;
  display: block;
  vertical-align: top;
}
.mg-demo-img-badge {
  position: absolute;
  right: 10px;
  top: 10px;
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.75);
  color: #e2e8f0;
}
.mg-demo-fields-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 10px;
  letter-spacing: 0.04em;
}
.mg-demo-desc {
  border-radius: 8px;
}
.mg-wrapper {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(240, 249, 255, 0.9) 0%, #fff 100%);
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.08);
}
.mg-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}
.mg-icon-badge {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #165dff, #7c6af7);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.mg-icon-badge svg {
  width: 22px;
  height: 22px;
}
.mg-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}
.mg-subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #64748b;
}
.mg-chips {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.mg-chips-label {
  font-size: 12px;
  color: #64748b;
}
.mg-chip {
  cursor: pointer;
}
.mg-input-row {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.mg-input-wrap {
  flex: 1;
  min-width: 200px;
}
.mg-alert {
  margin-bottom: 12px;
}
.mg-skeleton {
  height: 200px;
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.mg-skeleton-shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.6), transparent);
  animation: mg-shimmer 1.2s infinite;
}
@keyframes mg-shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
.mg-loading-hint {
  position: relative;
  z-index: 1;
  font-size: 12px;
  color: #64748b;
  text-align: center;
  max-width: 420px;
  line-height: 1.5;
}
.mg-result-card {
  margin-top: 12px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}
.mg-img-container {
  position: relative;
  background: #f8fafc;
  min-height: 160px;
}
.mg-img {
  width: 100%;
  max-height: 420px;
  object-fit: contain;
  display: block;
}
.mg-img-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px 14px;
  background: linear-gradient(transparent, rgba(15, 23, 42, 0.65));
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mg-tag {
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}
.mg-download-btn {
  color: #fff;
  font-size: 12px;
  text-decoration: none;
}
.mg-meta {
  padding: 8px 12px;
  font-size: 12px;
  color: #64748b;
  border-top: 1px solid #e2e8f0;
}
.mg-tips {
  margin: 14px 0 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.6;
}
</style>
