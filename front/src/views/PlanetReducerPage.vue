<template>
  <div class="planet-reducer-page">
    <el-card class="principle-card" shadow="hover">
      <div class="principle-header">
        <i class="el-icon-s-operation"></i>
        <h2>行星减速器原理深度解析</h2>
        <span class="subtitle">精密机械中的「小巨人」</span>
      </div>

      <div class="principle-content">
        <p class="lead">
          行星减速器凭借「太阳轮+行星轮+齿圈」的精妙啮合，在紧凑空间内实现高效减速与扭矩放大，
          成为机器人、电动汽车等高端领域的传动核心，其 95% 的高效传动与同轴设计重塑了精密机械的动力边界。
        </p>

        <section class="principle-section">
          <h3>一、什么是行星减速器？</h3>
          <p>
            行星减速器，又称行星齿轮减速机，是一种借助「<strong>太阳轮+行星轮+齿圈</strong>」三大核心元件之间的啮合运动，
            实现速比转换、扭矩放大的精密传动装置。其得名源于其内部齿轮运动方式类似于行星围绕太阳运转的轨迹，因此形象地称为「行星减速器」。
          </p>
        </section>

        <section class="principle-section">
          <h3>二、结构组成及命名解析</h3>
          <p>典型的行星减速器主要有以下几部分组成：</p>
          <div class="structure-diagram">
            <svg viewBox="0 0 400 280" class="diagram-svg">
              <!-- 齿圈 Ring Gear -->
              <circle cx="200" cy="140" r="120" fill="none" stroke="#165DFF" stroke-width="3" stroke-dasharray="8 4" opacity="0.8" />
              <text x="200" y="95" text-anchor="middle" class="diagram-label">齿圈 Ring Gear</text>
              <!-- 行星架 Carrier -->
              <circle cx="200" cy="140" r="90" fill="none" stroke="#67C23A" stroke-width="2" opacity="0.6" />
              <!-- 行星轮 Planet Gears -->
              <circle cx="140" cy="100" r="25" fill="#E6A23C" stroke="#C69500" stroke-width="2" />
              <text x="140" y="105" text-anchor="middle" class="diagram-label-small">行星轮</text>
              <circle cx="260" cy="100" r="25" fill="#E6A23C" stroke="#C69500" stroke-width="2" />
              <text x="260" y="105" text-anchor="middle" class="diagram-label-small">行星轮</text>
              <circle cx="200" cy="195" r="25" fill="#E6A23C" stroke="#C69500" stroke-width="2" />
              <text x="200" y="200" text-anchor="middle" class="diagram-label-small">行星轮</text>
              <!-- 太阳轮 Sun Gear -->
              <circle cx="200" cy="140" r="35" fill="#F56C6C" stroke="#C45656" stroke-width="2" />
              <text x="200" y="145" text-anchor="middle" class="diagram-label-small">太阳轮</text>
              <!-- 输入轴 -->
              <line x1="200" y1="0" x2="200" y2="100" stroke="#909399" stroke-width="2" />
              <text x="220" y="55" class="diagram-label-small">输入</text>
              <!-- 输出轴 -->
              <line x1="200" y1="180" x2="200" y2="280" stroke="#909399" stroke-width="2" />
              <text x="220" y="235" class="diagram-label-small">输出</text>
            </svg>
          </div>
          <ul>
            <li><strong>太阳轮（Sun Gear）</strong>：位于中心，是主动输入齿轮；</li>
            <li><strong>行星轮（Planet Gears）</strong>：多个齿轮围绕太阳轮转动，并与其啮合；</li>
            <li><strong>齿圈（Ring Gear）</strong>：内齿轮，包裹整个系统并与行星轮啮合；</li>
            <li><strong>行星架（Carrier）</strong>：固定并承载所有行星轮，实现力的汇聚与输出；</li>
            <li><strong>输出轴</strong>：连接行星架或齿圈，根据结构不同负责输出减速后的动力。</li>
          </ul>
          <p>其结构紧凑、输出同轴、负载均匀，是高速电机与精密机械之间的理想减速接口。</p>
        </section>

        <section class="principle-section">
          <h3>三、工作原理详解：如何实现减速？</h3>
          <p>行星减速器的核心在于齿轮之间的复合运动，通过太阳轮、行星轮、齿圈三者不同的固定与转动组合，实现不同的减速方式。</p>
          <div class="workflow-box">
            <h4>标准输入：太阳轮主动、齿圈固定、行星架输出</h4>
            <ol>
              <li>电机驱动太阳轮高速旋转；</li>
              <li>多个行星轮在太阳轮带动下旋转并围绕其做公转；</li>
              <li>行星轮与固定的齿圈啮合，限制其自由旋转方向；</li>
              <li>行星架集合所有行星轮的运动，输出低速高扭矩的旋转运动。</li>
            </ol>
          </div>
          <div class="formula-box">
            <h4>减速比计算公式：</h4>
            <p class="formula">i = 1 + Zr / Zs</p>
            <p>其中：Zr 为齿圈齿数；Zs 为太阳轮齿数。</p>
            <p class="example">例如，若 Zr = 60，Zs = 20，则减速比 i = 1 + 60/20 = 4，即输入转速为输出转速的 4 倍。</p>
          </div>
        </section>

        <section class="principle-section">
          <h3>四、行星减速器的技术优势</h3>
          <el-row :gutter="16">
            <el-col :span="12">
              <div class="advantage-item">
                <i class="el-icon-box"></i>
                <h4>体积小、功率密度高</h4>
                <p>多个行星轮分担传动负载，实现更高的扭矩密度，在紧凑体积下承受更大扭矩。</p>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="advantage-item">
                <i class="el-icon-s-flag"></i>
                <h4>效率高</h4>
                <p>由于多个齿轮同时啮合，传动效率可达 95% 以上，远超蜗轮蜗杆减速器。</p>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="advantage-item">
                <i class="el-icon-connection"></i>
                <h4>同轴结构、安装灵活</h4>
                <p>输入输出轴位于同一直线上，便于集成进设备中，尤其适合机器人关节、电动驱动单元。</p>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="advantage-item">
                <i class="el-icon-bell"></i>
                <h4>稳定性好、振动小、噪音低</h4>
                <p>负载分散在多个齿轮上，传动更加平稳，且噪音控制出色。</p>
              </div>
            </el-col>
          </el-row>
        </section>

        <section class="principle-section">
          <h3>五、典型应用场景</h3>
          <ul>
            <li><strong>工业自动化</strong>：协作机器人、伺服驱动系统；</li>
            <li><strong>新能源汽车</strong>：电驱系统、电机与车轮之间的动力匹配；</li>
            <li><strong>航空航天</strong>：高可靠性传动场合；</li>
            <li><strong>光伏与风电行业</strong>：太阳能跟踪系统、风力发电偏航与变桨控制；</li>
            <li><strong>包装与印刷设备</strong>：高速、高精密驱动系统。</li>
          </ul>
        </section>

        <section class="principle-section">
          <h3>六、维护与使用建议</h3>
          <ul>
            <li>定期更换润滑油脂，防止齿轮磨损；</li>
            <li>安装时需对中精确，避免轴承受力不均；</li>
            <li>不可长期超负荷运行，易损坏行星轮轴承或齿面；</li>
            <li>若出现异响、发热，应及时停机检查。</li>
          </ul>
        </section>

        <div class="source-ref">
          <span>参考来源：</span>
          <a href="https://baijiahao.baidu.com/s?id=1835353961808829764" target="_blank" rel="noopener noreferrer">行星减速器原理深度解析（百家号）</a>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'PlanetReducerPage'
}
</script>

<style scoped>
.planet-reducer-page {
  max-width: 900px;
  margin: 0 auto;
}
.principle-card {
  padding: 32px 40px;
}
.principle-header {
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 2px solid #165DFF;
}
.principle-header h2 {
  margin: 0 0 4px 0;
  font-size: 24px;
  color: #303133;
}
.principle-header .subtitle {
  font-size: 14px;
  color: #909399;
}
.principle-header i {
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
.principle-section {
  margin-bottom: 28px;
}
.principle-section h3 {
  font-size: 16px;
  color: #165DFF;
  margin: 0 0 12px 0;
  font-weight: 600;
}
.principle-section p {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  margin: 0 0 8px 0;
}
.principle-section ul {
  margin: 8px 0 0 20px;
  padding: 0;
}
.principle-section li {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  margin-bottom: 4px;
}
.structure-diagram {
  margin: 20px 0;
  text-align: center;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
}
.diagram-svg {
  max-width: 100%;
  height: auto;
}
.diagram-label {
  font-size: 12px;
  fill: #165DFF;
  font-weight: 600;
}
.diagram-label-small {
  font-size: 11px;
  fill: #606266;
}
.workflow-box,
.formula-box {
  background: #ecf5ff;
  border-left: 4px solid #165DFF;
  padding: 16px 20px;
  margin: 16px 0;
  border-radius: 4px;
}
.workflow-box h4,
.formula-box h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #165DFF;
}
.formula {
  font-family: 'Consolas', monospace;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 8px 0;
}
.example {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
}
.advantage-item {
  background: #f9f9f9;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
}
.advantage-item i {
  font-size: 24px;
  color: #165DFF;
  margin-bottom: 8px;
}
.advantage-item h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
}
.advantage-item p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
}
.source-ref {
  margin-top: 32px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  font-size: 12px;
  color: #909399;
}
.source-ref a {
  color: #165DFF;
  text-decoration: none;
}
.source-ref a:hover {
  text-decoration: underline;
}
</style>
