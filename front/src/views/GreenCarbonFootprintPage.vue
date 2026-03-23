<template>
  <div class="green-demo-embed">
    <el-card class="banner" shadow="never">
      <div class="banner-row">
        <div class="banner-text">
          <h2><i class="el-icon-sunny"></i> 绿能排产 · 演示模块</h2>
          <p>
            本页独立加载队友工程 <code>greengeneratesysteam</code>（Next.js），用于<strong>峰谷电价、能耗/碳排趋势、待优化订单</strong>等演示，与主站业务数据隔离。
          </p>
        </div>
        <div class="banner-actions">
          <span class="url-label">演示地址</span>
          <el-input v-model="demoBaseUrl" size="small" class="url-input" placeholder="http://localhost:3000 或 3001" clearable />
          <el-button size="small" icon="el-icon-refresh" @click="iframeKey += 1">刷新嵌入页</el-button>
        </div>
      </div>
      <el-alert type="info" :closable="false" show-icon class="hint-alert">
        <template slot="title">如何启动</template>
        在目录 <code>EGSfb/greengeneratesysteam</code> 执行 <code>npm install</code> 与 <code>npm run dev</code>。终端会显示 <code>Local</code> 地址：<strong>3000 被占用时 Next 会自动用 3001</strong>，请把上框地址改成与终端一致。也可在 <code>front/.env.development</code> 配置 <code>VITE_GREEN_DEMO_URL</code>。
      </el-alert>
    </el-card>

    <div class="iframe-wrap">
      <iframe
        :key="iframeKey"
        class="demo-iframe"
        title="绿能排产演示"
        :src="iframeSrc"
        frameborder="0"
        allow="fullscreen"
      />
      <div v-if="demoBaseUrl" class="iframe-fallback">
        <p>若上方空白，请确认队友服务已启动且地址正确。</p>
        <el-button type="text" @click="openInNewTab">新窗口打开</el-button>
      </div>
    </div>
  </div>
</template>

<script>
/** 队友 Next 演示：优先与终端 Local 一致；3000 常被占用时 next dev 会落在 3001 */
const DEFAULT_DEMO = 'http://localhost:3001'

export default {
  name: 'GreenCarbonFootprintPage',
  data() {
    return {
      demoBaseUrl: '',
      iframeKey: 0
    }
  },
  computed: {
    iframeSrc() {
      const u = (this.demoBaseUrl || '').trim() || DEFAULT_DEMO
      try {
        return new URL(u).href
      } catch (e) {
        return DEFAULT_DEMO
      }
    }
  },
  mounted() {
    const env = typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.VITE_GREEN_DEMO_URL
    if (env && String(env).trim()) {
      this.demoBaseUrl = String(env).trim()
    } else {
      this.demoBaseUrl = DEFAULT_DEMO
    }
  },
  methods: {
    openInNewTab() {
      window.open(this.iframeSrc, '_blank', 'noopener,noreferrer')
    }
  }
}
</script>

<style scoped>
.green-demo-embed {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  min-height: 480px;
  max-width: 100%;
  padding: 12px 16px 16px;
  box-sizing: border-box;
}
.banner {
  flex-shrink: 0;
  margin-bottom: 12px;
}
.banner-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.banner-text h2 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #303133;
}
.banner-text p {
  margin: 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
.banner-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.url-label {
  font-size: 12px;
  color: #909399;
}
.url-input {
  width: 260px;
}
.hint-alert {
  margin-top: 12px;
}
.hint-alert code {
  font-size: 12px;
}
.iframe-wrap {
  position: relative;
  flex: 1;
  min-height: 0;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  background: #0f172a;
}
.demo-iframe {
  width: 100%;
  height: 100%;
  min-height: 420px;
  display: block;
  border: none;
}
.iframe-fallback {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  pointer-events: none;
}
.iframe-fallback .el-button {
  pointer-events: auto;
}
</style>
