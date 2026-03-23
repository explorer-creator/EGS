<template>
  <div class="process-intro-page">
    <el-card class="intro-card" shadow="hover">
      <div class="intro-header">
        <i class="el-icon-s-tools"></i>
        <h2>工序与工艺介绍</h2>
        <span class="subtitle">精密行星减速器制造流程与数据建模参考</span>
      </div>

      <div class="intro-content">
        <p class="lead">
          理解工序的物理逻辑对建立数据实体（Data Entity）和关联关系至关重要。
          以下是一套完整的制造流程拆解，涵盖毛坯制造、轴类加工、机械加工与尺寸检测等核心环节。
        </p>

        <!-- 1. 毛坯制造 -->
        <section class="process-section">
          <h3><span class="section-num">1</span> 毛坯制造 (Blank Manufacturing)</h3>
          <p class="section-desc">这是零件的「出生」阶段，决定了零件的初始形状和内部组织。</p>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="process-item">
                <h4>铸造 (Casting)</h4>
                <p>将熔融金属倒入模具。适合形状复杂（如箱体、发动机缸体）的零件。</p>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="process-item">
                <h4>锻造 (Forging)</h4>
                <p>通过压力使金属坯料变形。适合承受高应力的零件（如曲轴、连杆），因为锻造能改善金属的纤维组织，提高强度。</p>
              </div>
            </el-col>
          </el-row>
        </section>

        <!-- 2. 轴类零件加工 -->
        <section class="process-section">
          <h3><span class="section-num">2</span> 轴类零件加工 (Shaft Processing)</h3>
          <p class="section-desc">轴类零件是典型的旋转体，其核心工艺是车削。</p>
          <div class="process-with-image">
            <div class="image-block">
              <img src="/img/lathe-turning.png" alt="车削加工" class="process-img" />
              <span class="img-caption">车削 (Turning)：在车床上旋转工件，用刀具切削外圆、台阶、螺纹</span>
            </div>
            <div class="process-list">
              <div class="process-item">
                <h4>切割 (Cutting/Sawing)</h4>
                <p>将长棒料按长度切成一段段坯料。</p>
              </div>
              <div class="process-item">
                <h4>车削 (Turning)</h4>
                <p>在车床上旋转工件，用刀具切削外圆、台阶、螺纹。</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 3. 机械加工 -->
        <section class="process-section">
          <h3><span class="section-num">3</span> 机械加工 (Machining)</h3>
          <p class="section-desc">这是对零件进行精细「雕琢」的过程。</p>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="process-item">
                <h4>铣削 (Milling)</h4>
                <p>刀具旋转，工件移动。用于加工平面、沟槽、齿轮齿面等。</p>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="process-item">
                <h4>钻孔 (Drilling)</h4>
                <p>在零件上加工孔位，通常后续还会配套扩孔或攻丝（做螺纹）。</p>
              </div>
            </el-col>
          </el-row>
        </section>

        <!-- 4. 尺寸检测 -->
        <section class="process-section">
          <h3><span class="section-num">4</span> 尺寸检测 (Dimensional Inspection)</h3>
          <p class="section-desc">质量控制的关键环节，确保加工出的零件符合设计图纸。</p>
          <div class="process-with-image">
            <div class="image-block">
              <img src="/img/cmm-inspection.png" alt="三坐标测量" class="process-img" />
              <span class="img-caption">三坐标测量 (CMM)：通过探测头接触工件表面，获取三维坐标数据</span>
            </div>
            <div class="process-list">
              <div class="process-item">
                <h4>三坐标测量 (CMM)</h4>
                <p>
                  通过探测头接触工件表面，获取三维坐标数据。它可以测量极其复杂的几何公差
                  （如圆柱度、位置度、轮廓度），精度通常达到微米级。
                </p>
              </div>
            </div>
          </div>
        </section>

        <!-- 工艺流程逻辑示例 -->
        <section class="process-section flow-section">
          <h3><span class="section-num">★</span> 工艺流程逻辑示例</h3>
          <p class="section-desc">对于一个典型的动力传动轴，其工序流转通常如下：</p>
          <div class="flow-diagram">
            <div class="flow-step">下料（切割）</div>
            <i class="el-icon-right flow-arrow"></i>
            <div class="flow-step">热锻（成型）</div>
            <i class="el-icon-right flow-arrow"></i>
            <div class="flow-step">粗车（定型）</div>
            <i class="el-icon-right flow-arrow"></i>
            <div class="flow-step">精铣（加工键槽）</div>
            <i class="el-icon-right flow-arrow"></i>
            <div class="flow-step">磨削（保证配合精度）</div>
            <i class="el-icon-right flow-arrow"></i>
            <div class="flow-step">三坐标检测（终检）</div>
          </div>
        </section>

        <!-- 参考资源 -->
        <section class="process-section ref-section">
          <h3>参考资源</h3>
          <p>以下权威工程学习资源有助于深入理解工艺与工位划分：</p>
          <ul>
            <li><strong>中国机械工程学会 - 机械工程知识库</strong>：包含大量标准的工艺说明</li>
            <li><strong>Visual Components</strong>：工业工艺仿真，可直观看到从铸造到检测的全自动化产线流程，对建模时的「工位」划分很有启发</li>
          </ul>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'ProcessIntroPage'
}
</script>

<style scoped>
.process-intro-page {
  max-width: 920px;
  margin: 0 auto;
}
.intro-card {
  padding: 32px 40px;
}
.intro-header {
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 2px solid #165DFF;
}
.intro-header h2 {
  margin: 0 0 4px 0;
  font-size: 24px;
  color: #303133;
}
.intro-header .subtitle {
  font-size: 13px;
  color: #909399;
}
.intro-header i {
  font-size: 28px;
  color: #165DFF;
  margin-right: 8px;
}
.lead {
  font-size: 15px;
  line-height: 1.8;
  color: #606266;
  margin: 0 0 28px 0;
}
.process-section {
  margin-bottom: 32px;
}
.process-section h3 {
  font-size: 17px;
  color: #165DFF;
  margin: 0 0 10px 0;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}
.section-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: #165DFF;
  color: #fff;
  border-radius: 6px;
  font-size: 14px;
}
.section-desc {
  font-size: 14px;
  color: #606266;
  margin: 0 0 16px 0;
  line-height: 1.7;
}
.process-item {
  background: #f9fafc;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}
.process-item h4 {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #303133;
}
.process-item p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
}
.process-with-image {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  margin-bottom: 16px;
}
.image-block {
  flex-shrink: 0;
  width: 320px;
}
.process-img {
  width: 100%;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.img-caption {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
.process-list {
  flex: 1;
}
.flow-section .flow-diagram {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 20px;
  background: linear-gradient(135deg, #ecf5ff 0%, #f0f9ff 100%);
  border-radius: 8px;
  border: 1px solid #d9ecff;
}
.flow-step {
  padding: 8px 14px;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #303133;
  border: 1px solid #165DFF;
  white-space: nowrap;
}
.flow-arrow {
  color: #165DFF;
  font-size: 16px;
}
.ref-section ul {
  margin: 8px 0 0 20px;
  padding: 0;
}
.ref-section li {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  margin-bottom: 4px;
}
@media (max-width: 768px) {
  .process-with-image {
    flex-direction: column;
  }
  .image-block {
    width: 100%;
  }
  .flow-diagram {
    flex-direction: column;
  }
}
</style>
