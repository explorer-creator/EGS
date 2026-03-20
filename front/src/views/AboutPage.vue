<template>
  <div class="about-page">
    <el-card class="intro-card" shadow="hover">
      <div class="intro-header">
        <i class="el-icon-office-building"></i>
        <h2>企业介绍</h2>
      </div>
      <div class="intro-content">
        <p class="lead">
          某制造企业主要从事<strong>精密行星减速器</strong>的研发设计与生产制造。
        </p>
        <section class="intro-section">
          <h3>发展定位</h3>
          <p>
            随着机器人与自动化设备等的应用需求日益增长，精密行星减速器向<strong>小型化、轻量化</strong>方向发展，
            并持续<strong>提高传动效率、降低噪音</strong>，以满足高端装备制造对高精度传动部件的需求。
          </p>
        </section>
        <section class="intro-section">
          <h3>制造工艺</h3>
          <p>
            在精密行星减速器的制造环节，各核心零件涉及多工艺多工序：
          </p>
          <ul>
            <li><strong>毛坯制造</strong>：铸造或锻造</li>
            <li><strong>机械加工</strong>：铣削、钻孔等</li>
            <li><strong>轴类零件加工</strong>：切割、车削等</li>
            <li><strong>尺寸检测</strong>：三坐标测量等</li>
          </ul>
        </section>
        <section class="intro-section">
          <h3>生产车间</h3>
          <p>
            该企业生产车间涉及<strong>车铣磨、CNC、热处理、三坐标</strong>等多品类、多品牌的加工和检测设备，
            具备从毛坯到成品的完整加工能力，保障精密行星减速器核心零件的制造质量与交付效率。
          </p>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'AboutPage'
}
</script>

<style scoped>
.about-page {
  max-width: 800px;
  margin: 0 auto;
}
.intro-card {
  padding: 32px 40px;
}
.intro-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 2px solid #165DFF;
}
.intro-header i {
  font-size: 32px;
  color: #165DFF;
}
.intro-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}
.lead {
  font-size: 16px;
  line-height: 1.8;
  color: #606266;
  margin: 0 0 24px 0;
}
.intro-section {
  margin-bottom: 24px;
}
.intro-section h3 {
  font-size: 16px;
  color: #165DFF;
  margin: 0 0 12px 0;
  font-weight: 600;
}
.intro-section p {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  margin: 0 0 8px 0;
}
.intro-section ul {
  margin: 8px 0 0 20px;
  padding: 0;
}
.intro-section li {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  margin-bottom: 4px;
}
</style>
