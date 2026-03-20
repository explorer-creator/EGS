<template>
  <div class="architecture-page">
    <el-card class="arch-card" shadow="hover">
      <div class="arch-header">
        <i class="el-icon-s-operation"></i>
        <h2>架构先进性</h2>
        <span class="subtitle">系统灵活可配置，具有较好的可扩展性</span>
      </div>

      <div class="arch-intro">
        <p class="lead">
          本系统采用<strong>前后端分离 + 配置驱动</strong>的架构，具备良好的灵活性与可扩展性，
          满足多环境部署与多租户扩展需求，便于后续功能扩展与维护。
        </p>
      </div>

      <el-collapse v-model="activeNames">
        <!-- 灵活可配置 -->
        <el-collapse-item title="一、灵活可配置" name="config">
          <h4>1. 后端配置化</h4>
          <ul>
            <li><strong>application.yml</strong>：集中管理服务端口、xDM-F 连接、CORS 等配置</li>
            <li><strong>环境变量覆盖</strong>：支持通过环境变量动态覆盖，无需修改代码即可适配不同部署环境</li>
          </ul>
          <div class="table-wrap">
            <table class="arch-table">
              <thead>
                <tr><th>配置项</th><th>环境变量</th><th>说明</th></tr>
              </thead>
              <tbody>
                <tr><td>xDM 服务地址</td><td><code>XDM_BASE_URL</code></td><td>xDM-F 后端地址</td></tr>
                <tr><td>应用 ID</td><td><code>XDM_APPLICATION_ID</code></td><td>xDM-F 鉴权应用标识</td></tr>
                <tr><td>租户 ID</td><td><code>XDM_TENANT_ID</code></td><td>多租户场景</td></tr>
                <tr><td>CORS 允许源</td><td><code>CORS_ORIGINS</code></td><td>支持逗号分隔多前端地址</td></tr>
              </tbody>
            </table>
          </div>

          <h4>2. 前端统一配置</h4>
          <ul>
            <li><strong>src/config.js</strong>：集中导出 tenantId、apiBase、applicationId</li>
            <li><strong>环境变量</strong>：Vite 支持 VITE_TENANT_ID、VITE_API_BASE、VITE_APPLICATION_ID 覆盖</li>
            <li><strong>.env.development / .env.production</strong>：按环境区分配置</li>
          </ul>
        </el-collapse-item>

        <!-- 可扩展性 -->
        <el-collapse-item title="二、可扩展性" name="extend">
          <h4>1. 后端代理层扩展</h4>
          <ul>
            <li><strong>BaseXdmProxyController</strong>：抽象基类，统一鉴权头构建、请求转发、404 容错</li>
            <li><strong>XdmProxyProperties</strong>：@ConfigurationProperties 注入配置，新增参数只需在 application.yml 扩展</li>
            <li><strong>Controller 扩展</strong>：新增业务实体时，继承 BaseXdmProxyController 并注入 XdmProxyProperties 即可，无需重复编写鉴权逻辑</li>
          </ul>

          <h4>2. 前端模块扩展</h4>
          <ul>
            <li>各业务页面通过 <code>import { config } from '../config'</code> 获取配置</li>
            <li>新增业务模块时，复用 config 与现有工具（exportCsv、parseCsv）即可快速扩展</li>
          </ul>

          <h4>3. 扩展点一览</h4>
          <div class="table-wrap">
            <table class="arch-table">
              <thead>
                <tr><th>扩展场景</th><th>实现方式</th></tr>
              </thead>
              <tbody>
                <tr><td>更换 xDM-F 地址</td><td>修改 application.yml 或设置 XDM_BASE_URL</td></tr>
                <tr><td>多租户部署</td><td>配置 XDM_TENANT_ID，前端 VITE_TENANT_ID</td></tr>
                <tr><td>新增前端部署地址</td><td>配置 CORS_ORIGINS</td></tr>
                <tr><td>新增业务实体代理</td><td>继承 BaseXdmProxyController，注入 XdmProxyProperties</td></tr>
                <tr><td>新增前端业务模块</td><td>引入 config、复用导出/导入工具</td></tr>
              </tbody>
            </table>
          </div>
        </el-collapse-item>

        <!-- 技术栈 -->
        <el-collapse-item title="三、技术栈" name="tech">
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="tech-block">
                <h4>后端</h4>
                <p>Spring Boot 3.x、RestTemplate、@ConfigurationProperties</p>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="tech-block">
                <h4>前端</h4>
                <p>Vue 2、Element UI、Vite、Axios</p>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="tech-block">
                <h4>数据建模</h4>
                <p>xDM-F 动态实体（Equipment、Material01、WorkingProcedure、ProcessRoute 等）</p>
              </div>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 赛题架构确认 -->
        <el-collapse-item title="四、赛题架构确认：数据层、业务层、展示层均通过 xDM-F API 交互" name="xdm">
          <p class="xdm-intro">
            本系统严格遵循赛题要求，<strong>数据层、业务层、展示层均通过 xDM-F API 交互</strong>，
            展示层不直接访问数据库，所有数据操作均经由 xDM-F REST API 完成。
          </p>
          <h4>1. 层级与实现</h4>
          <div class="table-wrap">
            <table class="arch-table">
              <thead>
                <tr><th>层级</th><th>实现</th><th>与 xDM-F 的交互</th></tr>
              </thead>
              <tbody>
                <tr>
                  <td><strong>展示层</strong></td>
                  <td>Vue 前端（EquipmentPage、MaterialPage、ProcedurePage、PlanPage 等）</td>
                  <td>所有数据通过 <code>$axios.post/get/delete</code> 调用 <code>/api/dynamic/api/...</code></td>
                </tr>
                <tr>
                  <td><strong>代理层</strong></td>
                  <td>Spring Boot 8080</td>
                  <td>仅转发请求到 xDM-F，无业务逻辑</td>
                </tr>
                <tr>
                  <td><strong>业务层</strong></td>
                  <td>xDM-F</td>
                  <td>提供 REST API，处理业务规则</td>
                </tr>
                <tr>
                  <td><strong>数据层</strong></td>
                  <td>xDM-F</td>
                  <td>负责数据持久化</td>
                </tr>
              </tbody>
            </table>
          </div>
          <h4>2. 数据流</h4>
          <div class="flow-diagram">
            展示层 → Vite 代理（/api → 8080）→ 代理层 → xDM-F（8003）
          </div>
          <p class="xdm-conclusion">
            <i class="el-icon-success"></i> 展示层不直接访问数据库，全部通过 xDM-F API 完成数据操作，<strong>符合赛题要求</strong>。
          </p>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'ArchitecturePage',
  data() {
    return {
      activeNames: ['config', 'extend', 'tech', 'xdm']
    }
  }
}
</script>

<style scoped>
.architecture-page {
  max-width: 860px;
  margin: 0 auto;
}
.arch-card {
  padding: 32px 40px;
}
.arch-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 2px solid #165DFF;
}
.arch-header i {
  font-size: 28px;
  color: #165DFF;
}
.arch-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}
.arch-header .subtitle {
  color: #909399;
  font-size: 13px;
  margin-left: 8px;
}
.arch-intro {
  margin-bottom: 24px;
}
.arch-intro .lead {
  font-size: 15px;
  line-height: 1.7;
  color: #606266;
  margin: 0;
}
.architecture-page h4 {
  margin: 16px 0 8px;
  font-size: 15px;
  color: #303133;
}
.architecture-page h4:first-child {
  margin-top: 0;
}
.architecture-page ul {
  margin: 0 0 12px;
  padding-left: 20px;
}
.architecture-page li {
  margin-bottom: 6px;
  line-height: 1.6;
}
.table-wrap {
  overflow-x: auto;
  margin: 12px 0;
}
.arch-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.arch-table th,
.arch-table td {
  border: 1px solid #EBEEF5;
  padding: 10px 14px;
  text-align: left;
}
.arch-table th {
  background: #F5F7FA;
  color: #303133;
  font-weight: 600;
}
.arch-table code {
  background: #F5F7FA;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}
.tech-block {
  background: #F5F7FA;
  padding: 16px;
  border-radius: 8px;
  height: 100%;
}
.tech-block h4 {
  margin: 0 0 8px;
  color: #165DFF;
}
.tech-block p {
  margin: 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
.xdm-intro {
  margin: 0 0 16px;
  font-size: 14px;
  line-height: 1.7;
  color: #606266;
}
.xdm-conclusion {
  margin: 16px 0 0;
  padding: 12px 16px;
  background: #f0f9eb;
  border-left: 4px solid #67c23a;
  color: #67c23a;
  font-size: 14px;
  line-height: 1.6;
}
.xdm-conclusion i {
  margin-right: 6px;
}
.flow-diagram {
  padding: 12px 16px;
  background: #ecf5ff;
  border-radius: 6px;
  font-family: monospace;
  font-size: 14px;
  color: #165DFF;
  text-align: center;
}
</style>
