<template>
  <div class="pb-demo">
    <PictureBookLikeShell :active-key.sync="activeKey" :nav-main="navMain" :nav-story="navStory" :footer-text="footerText" @nav-change="onNavChange">
      <div class="pb-demo__panel">
        <h2 class="pb-demo__title">{{ panelTitle }}</h2>
        <p class="pb-demo__desc">
          丝路智联 · 智慧制造：上方为常用入口，下方为扩展能力；中间为主业务区，可按模块挂载真实页面。
        </p>
        <el-alert title="说明" type="info" :closable="false" show-icon class="pb-demo__alert">
          与左侧蓝色主导航配合使用；本区独立切换状态，不替代外层「平台介绍」「贸易可视化」等菜单。
        </el-alert>
        <div class="pb-demo__cards">
          <el-card v-for="n in 3" :key="n" shadow="hover" class="pb-demo__card">
            <div slot="header">内容区 {{ n }}</div>
            <p>可将设备、物料、工序等组件按需嵌入此区域。</p>
          </el-card>
        </div>
      </div>
    </PictureBookLikeShell>
  </div>
</template>

<script>
import PictureBookLikeShell from '@/layouts/PictureBookLikeShell.vue'

const TITLES = {
  overview: '平台与展示',
  manufacture: '制造与协同',
  prefs: '偏好与设置',
  extend: '丝路扩展'
}

export default {
  name: 'PictureBookLikeShellDemoPage',
  components: { PictureBookLikeShell },
  data() {
    return {
      activeKey: 'overview',
      navMain: [
        { key: 'overview', label: '平台与展示', icon: 'el-icon-s-home' },
        { key: 'manufacture', label: '制造与协同', icon: 'el-icon-s-grid' },
        { key: 'prefs', label: '偏好与设置', icon: 'el-icon-setting' }
      ],
      navStory: [{ key: 'extend', label: '丝路扩展', icon: 'el-icon-s-promotion' }],
      footerText: '丝路智联 · 本地演示；接口与数据以实际环境为准。'
    }
  },
  computed: {
    panelTitle() {
      return TITLES[this.activeKey] || '主内容区'
    }
  },
  methods: {
    onNavChange(key) {
      if (this.$message && key) {
        this.$message.info('当前分区：' + (TITLES[key] || key))
      }
    }
  }
}
</script>

<style scoped>
.pb-demo {
  min-height: 0;
}

.pb-demo__panel {
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 12px;
  padding: 18px 20px 22px;
}

.pb-demo__title {
  margin: 0 0 10px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.03em;
}

.pb-demo__desc {
  margin: 0 0 14px;
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
}

.pb-demo__alert {
  margin-bottom: 16px;
}

.pb-demo__cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.pb-demo__card {
  border-radius: 10px !important;
}
</style>
