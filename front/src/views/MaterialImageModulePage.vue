<template>
  <div class="mim-page">
    <el-card shadow="hover" class="mim-hero">
      <h2><i class="el-icon-picture-outline"></i> 模块 A：关键词 → 工业配图</h2>
      <p class="mim-lead">
        根据<strong>关键词</strong>自动从网络抓取候选图，经清晰度/尺寸/去重等校验后选用；不达标时可用<strong>通义万相</strong>按工业写实风格生成（需配置
        <code>DASHSCOPE_API_KEY</code>）。适用于产品说明中的<strong>物料、原料、核心设备</strong>示意配图。
      </p>
      <el-alert type="success" :closable="false" show-icon>
        <template slot="title">当前演示模式</template>
        配图区已内置<strong>本地固定工业示意图</strong>（三坐标测量），无需启动 Python。若需爬图 / 通义万相生图，可部署 <code>EGSfb/A/server.py</code> 并扩展前端接口对接。
      </el-alert>
    </el-card>
    <MaterialGenerator />
  </div>
</template>

<script>
import MaterialGenerator from '../components/MaterialGenerator.vue'

export default {
  name: 'MaterialImageModulePage',
  components: { MaterialGenerator }
}
</script>

<style scoped>
.mim-page {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 24px;
}
.mim-hero {
  margin-bottom: 20px;
  border-radius: 12px;
}
.mim-hero h2 {
  margin: 0 0 10px;
  font-size: 18px;
  color: #0f172a;
}
.mim-lead {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.75;
  color: #475569;
}
</style>
