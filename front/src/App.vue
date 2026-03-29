<template>
  <div id="app">
    <!-- 未登录：显示登录/注册页 -->
    <LoginRegisterPage v-if="!currentUser" @login-success="onLoginSuccess" @guest-enter="onGuestEnter" />

    <!-- 已登录：主界面 -->
    <template v-else>
      <BackgroundDecor />
      <div class="main-content-layer app-layout">
        <!-- 欢迎烟花效果 -->
        <WelcomeFireworks v-if="showWelcome" :duration="5000" @done="showWelcome = false" />
        <!-- 左侧：纵向主导航 -->
        <aside class="layout-sidebar-left">
          <div class="sidebar-brand">
            <span class="sidebar-brand__mark">EGS</span>
            <span class="sidebar-brand__sub">智慧制造</span>
          </div>
        <el-menu
          mode="vertical"
          background-color="#165DFF"
          text-color="#fff"
          active-text-color="#ffd04b"
          class="main-menu main-menu-vertical"
          :default-active="leftMenuActive"
          :key="'menu-left-' + currentPage"
        >
        <el-menu-item index="0" @click="currentPage = 'AboutPage'">平台介绍</el-menu-item>
        <el-menu-item index="0b" @click="currentPage = 'PlanetReducerPage'">行星减速器原理</el-menu-item>
        <el-menu-item index="0c" @click="currentPage = 'ProcessIntroPage'">工序与工艺介绍</el-menu-item>
        <el-menu-item index="0d" @click="currentPage = 'ProductManufacturingPage'">产品制造问答</el-menu-item>
        <el-menu-item index="0e" @click="currentPage = 'CvInspectionPage'">视觉质检</el-menu-item>
        <el-menu-item index="green" class="menu-featured menu-featured-green" @click="currentPage = 'GreenCarbonFootprintPage'">
          <i class="el-icon-data-analysis"></i> 绿能排产演示
        </el-menu-item>
        <el-menu-item index="trade-viz" class="menu-featured menu-featured-trade" @click="currentPage = 'TradeVisualizationPage'">
          <i class="el-icon-pie-chart"></i> 贸易可视化
        </el-menu-item>
        <el-menu-item index="ai-chain" class="menu-featured menu-featured-aichain" @click="currentPage = 'AiBlockchainHubPage'">
          <i class="el-icon-link"></i> AI+链集成
        </el-menu-item>
        <el-menu-item index="rail-ops" class="menu-featured menu-featured-rail" @click="currentPage = 'CrossBorderRailHubPage'">
          <i class="el-icon-data-board"></i> 丝路班列协同
        </el-menu-item>
        <el-menu-item index="report" class="menu-featured menu-featured-report" @click="currentPage = 'ReportExportPage'">
          <i class="el-icon-document-checked"></i> 生成综合报告
        </el-menu-item>
        <el-menu-item index="material-a" class="menu-featured menu-featured-mat" @click="currentPage = 'MaterialImageModulePage'">
          <i class="el-icon-picture"></i> 智能物料配图
        </el-menu-item>
      </el-menu>
        </aside>

        <div class="layout-main">
      <MulticulturalRibbon compact />

      <!-- 页面内容区：keep-alive 保留产品问答等页面状态，跳转后返回不丢失 -->
      <div class="page-content" :class="{ 'page-content-transparent': isIntroPage }">
        <keep-alive :include="keepAlivePages">
          <component :is="currentPage" :key="currentPage" />
        </keep-alive>
      </div>
        </div>

        <aside class="layout-sidebar-right">
          <div class="sidebar-brand sidebar-brand--right">
            <span class="sidebar-brand__mark">运营</span>
            <span class="sidebar-brand__sub">工厂 · 供应链 · 物流</span>
          </div>
        <el-menu
          mode="vertical"
          background-color="#165DFF"
          text-color="#fff"
          active-text-color="#ffd04b"
          class="main-menu main-menu-vertical main-menu-side-right"
          :default-active="rightMenuActive"
          :key="'menu-right-' + currentPage"
        >
        <el-submenu index="cluster-factory" popper-class="menu-popper-factory">
          <template slot="title">
            <span class="menu-cluster-title"><i class="el-icon-cpu"></i> 智能工厂</span>
          </template>
          <el-menu-item index="1" @click="currentPage = 'EquipmentPage'">设备孪生 · 台账</el-menu-item>
          <el-menu-item index="3" @click="currentPage = 'ProcedurePage'">工序编排 · 执行</el-menu-item>
        </el-submenu>
        <el-submenu index="cluster-supply" popper-class="menu-popper-supply">
          <template slot="title">
            <span class="menu-cluster-title"><i class="el-icon-s-cooperation"></i> 智慧供应链</span>
          </template>
          <el-menu-item index="2" @click="currentPage = 'MaterialPage'">物料中枢 · 主数据</el-menu-item>
          <el-menu-item index="4" @click="currentPage = 'PlanPage'">工艺路线 · 编排</el-menu-item>
        </el-submenu>
        <el-menu-item index="logistics" class="menu-featured menu-featured-logistics" @click="currentPage = 'SmartLogisticsPage'">
          <i class="el-icon-truck"></i> 智慧物流
        </el-menu-item>
        <el-menu-item index="warehouse-dispatch" class="menu-featured menu-featured-dispatch" @click="currentPage = 'WarehouseDispatchPage'">
          <i class="el-icon-office-building"></i> 仓库调度
        </el-menu-item>
        <el-menu-item index="wms-smart" class="menu-featured menu-featured-wms" @click="currentPage = 'WmsSmartSystemPage'">
          <i class="el-icon-box"></i> 智能WMS
        </el-menu-item>
        <el-menu-item index="tms-smart" class="menu-featured menu-featured-tms" @click="currentPage = 'TmsSmartPage'">
          <i class="el-icon-position"></i> 智慧TMS
        </el-menu-item>
        <el-menu-item index="multimodal" class="menu-featured menu-featured-mm" @click="currentPage = 'MultimodalLogisticsPage'">
          <i class="el-icon-camera"></i> 多模态报单
        </el-menu-item>
        <el-menu-item index="map" @click="currentPage = 'MapTransportPage'">
          <i class="el-icon-location"></i> 运输地图
        </el-menu-item>
        <el-menu-item index="pet" class="menu-featured menu-featured-pet" @click="currentPage = 'PetExpressPage'">
          <span class="pet-menu-label">🐾 爱宠专送</span>
        </el-menu-item>
        <el-menu-item index="guide" @click="currentPage = 'GuidePage'">使用指南</el-menu-item>
        <el-menu-item index="arch" @click="currentPage = 'ArchitecturePage'">架构先进性</el-menu-item>
      </el-menu>
          <div class="right-rail-tools">
          <div class="right-rail-title">快捷</div>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索设备、物料、工序…"
            prefix-icon="el-icon-search"
            size="small"
            clearable
            class="rail-search-input"
            @keyup.enter.native="onSearch"
          />
          <el-button type="primary" size="small" icon="el-icon-search" class="rail-search-btn" @click="onSearch">搜索</el-button>
          <el-button size="small" icon="el-icon-download" class="rail-export-btn" @click="exportModelsToDisk">导出模型</el-button>
          <div class="rail-user" @click="profileDrawerVisible = true">
            <el-avatar :size="40" icon="el-icon-user-solid" />
            <div class="rail-user-text">
              <span class="rail-user-name">{{ currentUser.isGuest ? '访客' : (currentUser.username || currentUser.user_name || '用户') }}</span>
              <span class="rail-user-hint">点击账户</span>
            </div>
          </div>
          </div>
        </aside>

        <!-- 小机器人智能体：全页面可拖拽 -->
        <RobotAgent ref="robotAgent" />
        <!-- 反馈箱：右下角悬浮按钮 -->
        <FeedbackBox />
        <!-- 右侧固定入口：丝路物流贸易态势大屏（3D 国际线路 + 国内低空地图） -->
        <SilkRoadTradePanel />
      </div>

      <!-- 全局搜索结果弹窗 -->
      <el-dialog
        :title="searchLoading ? '搜索中...' : (searchResults.length === 0 ? '搜索结果' : `搜索结果（共 ${searchResults.length} 条）`)"
        :visible.sync="searchDialogVisible"
        width="640px"
        append-to-body
        :close-on-click-modal="false"
        @close="onSearchDialogClose"
      >
        <div v-loading="searchLoading" class="search-results">
          <template v-if="!searchLoading">
            <div v-if="searchResults.length === 0" class="search-empty">未找到与「{{ searchKeyword }}」相关的结果，可尝试其他关键词</div>
            <div v-else class="search-result-list">
              <div
                v-for="item in searchResults"
                :key="item.type + '-' + item.id"
                class="search-result-item"
                @click="goToSearchResult(item)"
              >
                <span class="result-type">{{ item.typeLabel }}</span>
                <span class="result-title">{{ item.title }}</span>
                <span class="result-sub" v-if="item.sub">{{ item.sub }}</span>
              </div>
            </div>
          </template>
        </div>
      </el-dialog>

      <!-- 用户信息抽屉：点击头像后显示 -->
      <el-drawer
        title="个人信息"
        :visible.sync="profileDrawerVisible"
        direction="rtl"
        size="400px"
      >
        <div class="profile-drawer">
          <div v-if="currentUser.isGuest" class="guest-tip">
            <p>您正在以访客身份浏览</p>
            <p class="tip-text">登录后可查看和修改个人信息</p>
            <el-button type="primary" @click="goToLogin">去登录</el-button>
          </div>
          <el-form :model="profileForm" label-width="100px" v-else-if="!profileEditing">
            <el-form-item label="用户名">{{ profileForm.username }}</el-form-item>
            <el-form-item label="性别">{{ profileForm.gender || '-' }}</el-form-item>
            <el-form-item label="身份职业">{{ profileForm.occupation || '-' }}</el-form-item>
            <el-form-item label="所在部门">{{ profileForm.department || '-' }}</el-form-item>
            <el-form-item label="负责业务">
              <div class="business-text">{{ profileForm.responsible_business || profileForm.responsibleBusiness || '-' }}</div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="startProfileEdit">修改信息</el-button>
              <el-button @click="logout">退出登录</el-button>
            </el-form-item>
          </el-form>
          <el-form :model="profileForm" label-width="100px" v-else ref="profileFormRef">
            <el-form-item label="用户名">{{ profileForm.username }}</el-form-item>
            <el-form-item label="性别">
              <el-select v-model="profileForm.gender" placeholder="请选择" style="width: 100%;">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
            <el-form-item label="身份职业">
              <el-input v-model="profileForm.occupation" placeholder="如：工程师" />
            </el-form-item>
            <el-form-item label="所在部门">
              <el-input v-model="profileForm.department" placeholder="如：生产部" />
            </el-form-item>
            <el-form-item label="负责业务">
              <el-input v-model="profileForm.responsible_business" type="textarea" :rows="3" placeholder="负责的业务范围" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="profileForm.newPassword" type="password" placeholder="不修改请留空" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile">保存</el-button>
              <el-button @click="cancelProfileEdit">取消</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-drawer>
    </template>
  </div>
</template>

<script>
import AboutPage from './views/AboutPage.vue'
import PlanetReducerPage from './views/PlanetReducerPage.vue'
import ProcessIntroPage from './views/ProcessIntroPage.vue'
import ProductManufacturingPage from './views/ProductManufacturingPage.vue'
import CvInspectionPage from './views/CvInspectionPage.vue'
import EquipmentPage from './views/EquipmentPage.vue'
import MaterialPage from './views/MaterialPage.vue'
import ProcedurePage from './views/ProcedurePage.vue'
import PlanPage from './views/PlanPage.vue'
import GuidePage from './views/GuidePage.vue'
import ArchitecturePage from './views/ArchitecturePage.vue'
import GreenProductionPage from './views/GreenProductionPage.vue'
import GreenCarbonFootprintPage from './views/GreenCarbonFootprintPage.vue'
import BusinessAnalysisPage from './views/BusinessAnalysisPage.vue'
import TradeVisualizationPage from './views/TradeVisualizationPage.vue'
import AiBlockchainHubPage from './views/AiBlockchainHubPage.vue'
import CrossBorderRailHubPage from './views/CrossBorderRailHubPage.vue'
import ReportExportPage from './views/ReportExportPage.vue'
import MaterialImageModulePage from './views/MaterialImageModulePage.vue'
import SmartLogisticsPage from './views/SmartLogisticsPage.vue'
import WarehouseDispatchPage from './views/WarehouseDispatchPage.vue'
import WmsSmartSystemPage from './views/WmsSmartSystemPage.vue'
import TmsSmartPage from './views/TmsSmartPage.vue'
import MultimodalLogisticsPage from './views/MultimodalLogisticsPage.vue'
import MapTransportPage from './views/MapTransportPage.vue'
import PetExpressPage from './views/PetExpressPage.vue'
import LoginRegisterPage from './views/LoginRegisterPage.vue'
import BackgroundDecor from './components/BackgroundDecor.vue'
import WelcomeFireworks from './components/WelcomeFireworks.vue'
import FeedbackBox from './components/FeedbackBox.vue'
import RobotAgent from './components/RobotAgent.vue'
import SilkRoadTradePanel from './components/SilkRoadTradePanel.vue'
import MulticulturalRibbon from './components/MulticulturalRibbon.vue'
import { config } from './config'
import { formatXdmTime } from './utils/xdmTime'
import { parseListResponse } from './utils/parseListResponse'
import { getTempEquipmentList, getTempMaterialList, getTempProcedureList, getTempRouteList } from './utils/tempStorage'

const TENANT_ID = config.tenantId
const PAGE_VO = { curPage: 1, pageSize: 50, totalRows: 0, totalPages: 0, limit: 50, offset: 0 }

/** 可搜索的页面内容：关键词 -> 页面信息，搜索时匹配关键词可跳转 */
const SEARCHABLE_PAGES = [
  { keywords: ['架构', 'xDM-F', '数据层', '业务层', '展示层', '赛题', 'API', '代理层', '符合赛题'], page: 'ArchitecturePage', typeLabel: '架构说明', title: '架构先进性', sub: '赛题架构确认：数据层、业务层、展示层均通过 xDM-F API 交互' },
  { keywords: ['指南', '使用', '帮助'], page: 'GuidePage', typeLabel: '使用指南', title: '使用指南', sub: '系统操作说明' },
  { keywords: ['企业', '介绍', '公司', '丝路', '智联', '一带一路', '海外仓', '跨境', '贸易', '态势', '大屏', '国际线路', '低空', '海外', '通道'], page: 'AboutPage', typeLabel: '平台与企业介绍', title: '丝路智联 · 平台简介', sub: '一带一路 · 国际物流 · 跨境电商与海外仓；右侧「丝路贸易态势」含智慧物流全景与 3D 国际线路' },
  { keywords: ['行星', '减速器', '原理'], page: 'PlanetReducerPage', typeLabel: '行星减速器', title: '行星减速器原理', sub: '原理介绍' },
  { keywords: ['工序', '工艺', '介绍'], page: 'ProcessIntroPage', typeLabel: '工序工艺', title: '工序与工艺介绍', sub: '工序与工艺说明' },
  { keywords: ['产品', '制造', '问答', '千问', 'LLM', '原材料', '设备', '耳机', '充电宝'], page: 'ProductManufacturingPage', typeLabel: '产品制造', title: '产品制造知识问答', sub: 'AI 输出原材料、设备、工序、工艺流程、物理原理' },
  { keywords: ['视觉', '质检', 'CV', 'PCB', '缺陷', '漏焊', '短路', '断路', '报废'], page: 'CvInspectionPage', typeLabel: '视觉质检', title: 'PCB 裸板视觉质检', sub: '缺陷识别、坐标同步、自动报废' },
  { keywords: ['绿色', '生产', '低碳', '碳足迹', 'PUE', '供应链', '环保'], page: 'GreenProductionPage', typeLabel: '绿色生产', title: '绿色生产', sub: '能效监测、碳足迹、绿色供应链、零浪费' },
  { keywords: ['绿色排产', '绿能', '峰谷电', '演示', '队友', 'greengeneratesysteam', 'Next', '绿能排产'], page: 'GreenCarbonFootprintPage', typeLabel: '绿能排产演示', title: '绿能排产演示', sub: '嵌入队友 Next 演示' },
  { keywords: ['贸易', '进出口', '海关', '出口', '进口', '饼图', '可视化', '东盟', '欧盟', '一带一路', '货物贸易'], page: 'TradeVisualizationPage', typeLabel: '贸易可视化', title: '国际贸易数据可视化', sub: '进出口与区域结构饼图' },
  { keywords: ['区块链', '链上', '哈希', 'SLA', '智能合约', '锚定', '可信', '审计', 'AI+链'], page: 'AiBlockchainHubPage', typeLabel: 'AI+链集成', title: 'AI 与区块链集成', sub: '摘要上链指纹、SLA 预测与贸易可信层' },
  { keywords: ['丝路', '班列', '中欧', '海外仓', '跨境', '电商', '协同', '分拣', '路线优化', '可视化', '游戏', '翻译'], page: 'CrossBorderRailHubPage', typeLabel: '丝路班列协同', title: '跨境仓线与班列协同大屏', sub: 'WMS 分拣 + TMS 路径优化，双语与丝路接力挑战' },
  { keywords: ['综合报告', '生成报告', '商业', '分析', '报价', '敏感度', '利润', '市场策略', 'Excel', 'PDF', '设备', '物料', '工序', '工艺'], page: 'ReportExportPage', typeLabel: '生成综合报告', title: '生成综合报告', sub: '绿色流程与设备/物料/工序，导出 Excel 与 PDF' },
  { keywords: ['配图', '物料图', '关键词', '抓取', '万相', '通义', '原料图', '设备图', '模块A'], page: 'MaterialImageModulePage', typeLabel: '智能物料配图', title: '关键词工业配图（模块A）', sub: '爬取+AI 生图，需本地 Python 服务' },
  { keywords: ['智慧物流', '物流', '仓储', 'WMS', '分拣', '路径', 'AGV', '库位', '入库', '出库', 'OCR', '视觉识别', 'YOLO', '运筹优化', '预测分析', 'NLP', '自然语言', '物流AI', '千问', 'Whisper', '语音调度', '意图识别', 'VRP', 'OR-Tools', 'Gurobi', '高德', '路线规划', '需求预测', '时序', 'Forecast', 'Prophet', '单量预测', '3D', '沙盘', '数字孪生', '人工智能'], page: 'SmartLogisticsPage', typeLabel: '智慧物流', title: '智慧物流', sub: 'WMS、分拣、A*、AI+3D 沙盘、需求预测与AI接口映射' },
  { keywords: ['仓库调度', '出仓', '中转', '干线', '最优路线', '压力', '可行性', 'Dijkstra'], page: 'WarehouseDispatchPage', typeLabel: '仓库调度', title: '仓库调度', sub: '出仓记录、最短路径、中转压力与可视化' },
  { keywords: ['智能WMS', 'WMS', '货位', '货到人', 'AGV', '穿梭车', '机械臂', '数字孪生', '周转率', '拣货路径', '仓储管理'], page: 'WmsSmartSystemPage', typeLabel: '智能WMS', title: '智能仓储管理系统', sub: '动态货位、货到人调度、数字孪生监控' },
  { keywords: ['智慧TMS', 'TMS', '运输管理', '配载', '路径优化', '蚁群', 'ETA', 'OBD', '疲劳驾驶', '在途', '限行', '时效窗', 'VRP', 'OR-Tools', 'Gurobi', '高德', '谷歌地图', 'PAI-EAS', '精准ETA', '机器学习'], page: 'TmsSmartPage', typeLabel: '智慧TMS', title: '智慧运输管理系统', sub: 'ACO、ML-ETA(PAI-EAS形态)、OBD与路况' },
  { keywords: ['多模态', '截图', '运单', 'OCR', '语音', '报单', '拍照', '面单', '上传图片'], page: 'MultimodalLogisticsPage', typeLabel: '多模态报单', title: '多模态物流报单', sub: '运单截图识别、语音报单、写入 TMS/WMS' },
  { keywords: ['地图', '运输地图', '高德', '距离', '路线', '厂房', '导航', '运时', '城际', '城市'], page: 'MapTransportPage', typeLabel: '运输地图', title: '运输地图', sub: '城际运时预估、设施地图、驾车路线' },
  { keywords: ['爱宠', '宠物', '12306', '铁路', '火车', '托运', '车次'], page: 'PetExpressPage', typeLabel: '爱宠专送', title: '爱宠专送', sub: '铁路参考车次、历时与费用估算' },
  { keywords: ['智能工厂', '设备孪生', '设备台账', '设备管理', '产线', '机床', '台账'], page: 'EquipmentPage', typeLabel: '智能工厂', title: '设备孪生 · 台账', sub: '全生命周期设备主数据与现场管理' },
  { keywords: ['智能工厂', '工序编排', '工序管理', '工序执行', 'WorkingProcedure', '毛坯', '精加工'], page: 'ProcedurePage', typeLabel: '智能工厂', title: '工序编排 · 执行', sub: '工序建模、关联与现场执行' },
  { keywords: ['智慧供应链', '物料中枢', '物料管理', '主数据', 'BOM', '物料分类', 'Part'], page: 'MaterialPage', typeLabel: '智慧供应链', title: '物料中枢 · 主数据', sub: '物料、分类与 BOM 协同' },
  { keywords: ['智慧供应链', '工艺路线', '工艺路线管理', 'ProcessRoute', '排程', '中心轮'], page: 'PlanPage', typeLabel: '智慧供应链', title: '工艺路线 · 编排', sub: '路线版本与工序串联' }
]

function buildListBody(conditions = []) {
  return { params: { sort: 'ASC', orderBy: 'id', characterSet: 'SCHINESE_PINYIN_M', orderByTableAlias: 'p', conditions } }
}

function buildUpdateParams(data) {
  return {
    params: {
      ...data,
      modifier: 'admin',
      lastUpdateTime: formatXdmTime(),
      rdmExtensionType: 'User_Infor',
      tenant: { id: TENANT_ID, clazz: 'Tenant' }
    }
  }
}

export default {
  name: 'App',
  components: {
    BackgroundDecor,
    AboutPage,
    PlanetReducerPage,
    ProcessIntroPage,
    ProductManufacturingPage,
    CvInspectionPage,
    EquipmentPage,
    MaterialPage,
    ProcedurePage,
    PlanPage,
    GuidePage,
    ArchitecturePage,
    GreenProductionPage,
    GreenCarbonFootprintPage,
    BusinessAnalysisPage,
    TradeVisualizationPage,
    AiBlockchainHubPage,
    CrossBorderRailHubPage,
    ReportExportPage,
    MaterialImageModulePage,
    SmartLogisticsPage,
    WarehouseDispatchPage,
    WmsSmartSystemPage,
    TmsSmartPage,
    MultimodalLogisticsPage,
    MapTransportPage,
    PetExpressPage,
    LoginRegisterPage,
    WelcomeFireworks,
    FeedbackBox,
    RobotAgent,
    SilkRoadTradePanel,
    MulticulturalRibbon
  },
  provide() {
    return {
      globalSearchApply: this.globalSearchApply,
      robotExplain: (term) => {
        const el = this.$refs.robotAgent
        if (el && typeof el.explainTerm === 'function') el.explainTerm(term)
      },
      navigateToPage: (page, params) => {
        const p = params || {}
        this.sharedNavigateParams.openProductionPlan = !!p.openProductionPlan
        this.sharedNavigateParams.product = p.product || ''
        this.sharedNavigateParams.lastAnswer = p.lastAnswer || ''
        if (page === 'ProductManufacturingPage') {
          this.sharedNavigateParams.product = ''
          this.sharedNavigateParams.openProductionPlan = false
        }
        this.currentPage = page
      },
      sharedNavigateParams: this.sharedNavigateParams
    }
  },
  computed: {
    /** 左侧菜单高亮：展示 / 分析 / 配图等 */
    leftMenuActive() {
      const map = {
        AboutPage: '0',
        PlanetReducerPage: '0b',
        ProcessIntroPage: '0c',
        ProductManufacturingPage: '0d',
        CvInspectionPage: '0e',
        GreenProductionPage: '',
        GreenCarbonFootprintPage: 'green',
        BusinessAnalysisPage: '',
        TradeVisualizationPage: 'trade-viz',
        AiBlockchainHubPage: 'ai-chain',
        CrossBorderRailHubPage: 'rail-ops',
        ReportExportPage: 'report',
        MaterialImageModulePage: 'material-a'
      }
      return map[this.currentPage] || ''
    },
    /** 右侧菜单高亮：工厂 / 供应链 / 物流等 */
    rightMenuActive() {
      const map = {
        EquipmentPage: '1',
        MaterialPage: '2',
        ProcedurePage: '3',
        PlanPage: '4',
        SmartLogisticsPage: 'logistics',
        WarehouseDispatchPage: 'warehouse-dispatch',
        WmsSmartSystemPage: 'wms-smart',
        TmsSmartPage: 'tms-smart',
        MultimodalLogisticsPage: 'multimodal',
        MapTransportPage: 'map',
        PetExpressPage: 'pet',
        GuidePage: 'guide',
        ArchitecturePage: 'arch'
      }
      return map[this.currentPage] || ''
    },
    isIntroPage() {
      return ['AboutPage', 'PlanetReducerPage', 'ProcessIntroPage', 'ProductManufacturingPage', 'CvInspectionPage', 'GreenProductionPage', 'GreenCarbonFootprintPage', 'BusinessAnalysisPage', 'TradeVisualizationPage', 'AiBlockchainHubPage', 'CrossBorderRailHubPage', 'ReportExportPage', 'MaterialImageModulePage', 'SmartLogisticsPage', 'WarehouseDispatchPage', 'WmsSmartSystemPage', 'TmsSmartPage', 'MultimodalLogisticsPage', 'MapTransportPage', 'PetExpressPage', 'GuidePage', 'ArchitecturePage'].includes(this.currentPage)
    },
    keepAlivePages() {
      return 'ProductManufacturingPage,GreenProductionPage,GreenCarbonFootprintPage,BusinessAnalysisPage,TradeVisualizationPage,AiBlockchainHubPage,CrossBorderRailHubPage,ReportExportPage,MaterialImageModulePage,SmartLogisticsPage,WarehouseDispatchPage,WmsSmartSystemPage,TmsSmartPage,MultimodalLogisticsPage,MapTransportPage,PetExpressPage'
    }
  },
  data() {
    return {
      searchKeyword: '',
      searchLoading: false,
      searchDialogVisible: false,
      searchResults: [],
      pendingSearchPage: null,
      globalSearchApply: { page: '', keyword: '' },
      showWelcome: true,
      currentPage: 'AboutPage',
      sharedNavigateParams: { openProductionPlan: false, product: '', lastAnswer: '' },
      currentUser: null,
      profileDrawerVisible: false,
      profileEditing: false,
      profileForm: {
        id: '',
        username: '',
        gender: '',
        occupation: '',
        department: '',
        responsible_business: '',
        newPassword: ''
      }
    }
  },
  mounted() {
    const saved = localStorage.getItem('current_user')
    if (saved) {
      try {
        this.currentUser = JSON.parse(saved)
      } catch (e) {
        localStorage.removeItem('current_user')
      }
    }
  },
  methods: {
    onLoginSuccess(user) {
      this.currentUser = user
      this.showWelcome = true
    },
    onGuestEnter() {
      this.currentUser = { username: '访客', isGuest: true }
      this.showWelcome = true
    },
    goToLogin() {
      this.profileDrawerVisible = false
      this.logout()
    },
    logout() {
      const wasGuest = this.currentUser?.isGuest
      this.currentUser = null
      localStorage.removeItem('current_user')
      this.profileDrawerVisible = false
      this.$message.info(wasGuest ? '已退出访客模式' : '已退出登录')
    },
    startProfileEdit() {
      this.profileForm = {
        id: this.currentUser.id,
        username: this.currentUser.username || this.currentUser.user_name,
        gender: this.currentUser.gender || '',
        occupation: this.currentUser.occupation || '',
        department: this.currentUser.department || '',
        responsible_business: this.currentUser.responsible_business || this.currentUser.responsibleBusiness || '',
        newPassword: ''
      }
      this.profileEditing = true
    },
    cancelProfileEdit() {
      this.profileEditing = false
    },
    async saveProfile() {
      try {
        const updateData = {
          id: this.profileForm.id,
          username: this.profileForm.username,
          gender: this.profileForm.gender || '',
          occupation: this.profileForm.occupation || '',
          department: this.profileForm.department || '',
          responsible_business: this.profileForm.responsible_business || ''
        }
        if (this.profileForm.newPassword) {
          updateData.password = this.profileForm.newPassword
        }
        const body = buildUpdateParams(updateData)
        await this.$axios.post('/api/dynamic/api/User_Infor/update', body, {
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        this.currentUser = { ...this.currentUser, ...updateData }
        delete this.currentUser.password
        localStorage.setItem('current_user', JSON.stringify(this.currentUser))
        this.$message.success('保存成功')
        this.profileEditing = false
        this.profileForm.newPassword = ''
      } catch (e) {
        this.$message.error('保存失败：' + (e.response?.data?.message || e.message))
      }
    },
    async onSearch() {
      const kw = (this.searchKeyword || '').trim()
      if (!kw) {
        this.$message.warning('请输入搜索关键词')
        return
      }
      this.searchLoading = true
      this.searchDialogVisible = true
      this.searchResults = []
      const likeVal = '%' + kw + '%'
      const listOpt = { params: { pageVO: JSON.stringify(PAGE_VO) }, headers: { 'Content-Type': 'application/json;charset=UTF-8' } }
      const cond = (name, val) => buildListBody([{ conditionName: name, operator: 'like', conditionValues: [val] }])
      const match = (s) => (s && String(s).toLowerCase().includes(kw.toLowerCase()))
      const results = []
      try {
        const [equipRes, matRes, procRes, routeRes] = await Promise.allSettled([
          this.$axios.post('/api/dynamic/api/EquipmentManagement/list', cond('equipment_name', likeVal), listOpt),
          this.$axios.post('/api/dynamic/api/MaterialManagement/list', cond('material_name', likeVal), listOpt),
          this.$axios.post('/api/dynamic/api/ProcedureManagement/list', cond('procedure_name', likeVal), listOpt),
          this.$axios.post('/api/dynamic/api/ProcessRoute/list', cond('route_name', likeVal), listOpt)
        ])
        const toList = (res) => (res.status === 'fulfilled' && res.value?.data ? parseListResponse(res.value.data) : [])
        toList(equipRes).forEach(r => results.push({ type: 'equipment', typeLabel: '设备', page: 'EquipmentPage', id: r.id, title: r.equipment_name || r.equipmentName || '-', sub: r.equipment_code || r.equipmentCode }))
        toList(matRes).forEach(r => results.push({ type: 'material', typeLabel: '物料', page: 'MaterialPage', id: r.id, title: r.material_name || r.materialName || '-', sub: r.material_code || r.materialCode }))
        toList(procRes).forEach(r => results.push({ type: 'procedure', typeLabel: '工序', page: 'ProcedurePage', id: r.id, title: r.procedure_name || r.procedureName || '-', sub: r.procedure_code || r.procedureCode }))
        toList(routeRes).forEach(r => results.push({ type: 'route', typeLabel: '工艺路线', page: 'PlanPage', id: r.id, title: r.route_name || r.routeName || '-', sub: (r.route_code || r.routeCode) + ' v' + (r.version || '') }))
        getTempEquipmentList().filter(r => match(r.equipment_name || r.equipmentName) || match(r.equipment_code || r.equipmentCode)).forEach(r => results.push({ type: 'equipment', typeLabel: '设备', page: 'EquipmentPage', id: r.id, title: r.equipment_name || r.equipmentName || '-', sub: r.equipment_code || r.equipmentCode }))
        getTempMaterialList().filter(r => match(r.material_name || r.materialName) || match(r.material_code || r.materialCode)).forEach(r => results.push({ type: 'material', typeLabel: '物料', page: 'MaterialPage', id: r.id, title: r.material_name || r.materialName || '-', sub: r.material_code || r.materialCode }))
        getTempProcedureList().filter(r => match(r.procedure_name || r.procedureName) || match(r.procedure_code || r.procedureCode)).forEach(r => results.push({ type: 'procedure', typeLabel: '工序', page: 'ProcedurePage', id: r.id, title: r.procedure_name || r.procedureName || '-', sub: r.procedure_code || r.procedureCode }))
        getTempRouteList().filter(r => match(r.route_name || r.routeName) || match(r.route_code || r.routeCode)).forEach(r => results.push({ type: 'route', typeLabel: '工艺路线', page: 'PlanPage', id: r.id, title: r.route_name || r.routeName || '-', sub: (r.route_code || r.routeCode) + ' v' + (r.version || '') }))
        SEARCHABLE_PAGES.forEach(p => {
          if (p.keywords.some(k => kw.toLowerCase().includes(k.toLowerCase()) || k.toLowerCase().includes(kw.toLowerCase()))) {
            results.push({ type: 'page', typeLabel: p.typeLabel, page: p.page, id: 'page-' + p.page, title: p.title, sub: p.sub })
          }
        })
        const seen = new Set()
        this.searchResults = results.filter(r => { const k = r.type + '-' + r.id; if (seen.has(k)) return false; seen.add(k); return true })
      } catch (e) {
        this.$message.error('搜索失败：' + (e.response?.data?.message || e.message))
      } finally {
        this.searchLoading = false
      }
    },
    goToSearchResult(item) {
      if (!item || !item.page) return
      this.pendingSearchPage = item.page
      this.globalSearchApply.page = item.page
      this.globalSearchApply.keyword = this.searchKeyword.trim()
      this.searchDialogVisible = false
    },
    onSearchDialogClose() {
      if (this.pendingSearchPage) {
        this.currentPage = this.pendingSearchPage
        this.pendingSearchPage = null
      }
    },
    async exportModelsToDisk() {
      try {
        const res = await this.$axios.post('/api/export/models-to-disk', {}, { headers: { 'Content-Type': 'application/json' } })
        const d = res.data || res
        if (d.success) {
          this.$message.success('已导出到 D:\\EGS：' + (d.excelPath || '') + '、' + (d.mermaidPath || ''))
        } else {
          this.$message.error(d.message || '导出失败')
        }
      } catch (e) {
        this.$message.error('导出失败：' + (e.response?.data?.message || e.message))
      }
    },
  }
}
</script>

<style>
.page-content {
  margin: 20px;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fff;
}
.page-content-transparent {
  background: transparent !important;
  border-color: transparent !important;
}
.main-content-layer {
  position: relative;
  z-index: 1;
}
/* 左导航 + 主内容 + 右快捷栏 */
.app-layout {
  display: flex;
  align-items: stretch;
  min-height: 100vh;
  width: 100%;
  box-sizing: border-box;
  --sidebar-right-w: 228px;
}
.layout-sidebar-left {
  flex: 0 0 228px;
  width: 228px;
  min-width: 228px;
  background: linear-gradient(180deg, #165dff 0%, #0e4bcc 100%);
  box-shadow: 4px 0 24px rgba(22, 93, 255, 0.18);
  display: flex;
  flex-direction: column;
  z-index: 2;
}
.sidebar-brand {
  flex-shrink: 0;
  padding: 18px 16px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}
.sidebar-brand__mark {
  display: block;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: #fff;
  line-height: 1.2;
}
.sidebar-brand__sub {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 0.12em;
}
.sidebar-brand--right .sidebar-brand__mark {
  font-size: 18px;
}
.main-menu-side-right {
  flex: 1;
  min-height: 0;
}
.main-menu-vertical {
  flex: 1;
  min-height: 0;
  border-right: none !important;
  overflow-y: auto;
  overflow-x: hidden;
}
.main-menu-vertical:not(.el-menu--collapse) {
  width: 100% !important;
}
.main-menu-vertical .el-menu-item,
.main-menu-vertical .el-submenu__title {
  line-height: 44px;
  height: auto;
  min-height: 44px;
  white-space: normal;
  padding-left: 16px !important;
  padding-right: 12px !important;
}
.main-menu-vertical .el-menu-item [class^="el-icon-"] {
  margin-right: 6px;
}
.layout-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.layout-sidebar-right {
  flex: 0 0 var(--sidebar-right-w);
  width: var(--sidebar-right-w);
  min-width: var(--sidebar-right-w);
  box-sizing: border-box;
  padding: 0;
  background: linear-gradient(180deg, #164ecf 0%, #0c3580 100%);
  border-left: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: -4px 0 24px rgba(22, 93, 255, 0.14);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  z-index: 2;
}
.right-rail-tools {
  flex-shrink: 0;
  padding: 12px 10px 14px;
  background: rgba(15, 23, 42, 0.38);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.right-rail-title {
  font-size: 12px;
  font-weight: 700;
  color: rgba(248, 250, 252, 0.95);
  letter-spacing: 0.1em;
  margin-bottom: 0;
}
.rail-search-input .el-input__inner {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98);
}
.rail-search-btn,
.rail-export-btn {
  width: 100%;
  margin: 0 !important;
}
.rail-user {
  margin-top: 4px;
  padding: 12px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(22, 93, 255, 0.15);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: box-shadow 0.2s;
}
.rail-user:hover {
  box-shadow: 0 4px 14px rgba(22, 93, 255, 0.15);
}
.rail-user-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.rail-user-name {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rail-user-hint {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}
/* 丝路贸易悬浮条让出右侧栏宽度 */
.app-layout .silk-road-panel-root .edge-trigger {
  right: var(--sidebar-right-w);
}
.main-menu-vertical.main-menu {
  display: block;
}
.menu-featured { font-weight: 600 !important; }
.menu-featured i { margin-right: 4px; }
.menu-featured-green { background: linear-gradient(135deg, rgba(82,196,26,0.35) 0%, rgba(56,158,13,0.25) 100%) !important; }
.menu-featured-green:hover { background: linear-gradient(135deg, rgba(82,196,26,0.5) 0%, rgba(56,158,13,0.4) 100%) !important; }
.menu-featured-business { background: linear-gradient(135deg, rgba(22,93,255,0.4) 0%, rgba(14,66,194,0.3) 100%) !important; }
.menu-featured-business:hover { background: linear-gradient(135deg, rgba(22,93,255,0.55) 0%, rgba(14,66,194,0.45) 100%) !important; }
.menu-featured-trade { background: linear-gradient(135deg, rgba(0,212,255,0.35) 0%, rgba(22,93,255,0.32) 100%) !important; }
.menu-featured-trade:hover { background: linear-gradient(135deg, rgba(0,212,255,0.5) 0%, rgba(22,93,255,0.45) 100%) !important; }
.menu-featured-rail { background: linear-gradient(135deg, rgba(217,119,6,0.48) 0%, rgba(180,83,9,0.38) 100%) !important; }
.menu-featured-rail:hover { background: linear-gradient(135deg, rgba(217,119,6,0.62) 0%, rgba(180,83,9,0.52) 100%) !important; }
.menu-featured-aichain { background: linear-gradient(135deg, rgba(16,185,129,0.42) 0%, rgba(99,102,241,0.38) 100%) !important; }
.menu-featured-aichain:hover { background: linear-gradient(135deg, rgba(16,185,129,0.58) 0%, rgba(99,102,241,0.52) 100%) !important; }
.menu-featured-mat { background: linear-gradient(135deg, rgba(236,72,153,0.38) 0%, rgba(99,102,241,0.35) 100%) !important; }
.menu-featured-mat:hover { background: linear-gradient(135deg, rgba(236,72,153,0.52) 0%, rgba(99,102,241,0.48) 100%) !important; }
.menu-featured-report { background: linear-gradient(135deg, rgba(82,196,26,0.35) 0%, rgba(56,158,13,0.25) 100%) !important; }
.menu-featured-report:hover { background: linear-gradient(135deg, rgba(82,196,26,0.5) 0%, rgba(56,158,13,0.4) 100%) !important; }
.menu-featured-logistics { background: linear-gradient(135deg, rgba(230,162,60,0.45) 0%, rgba(200,120,20,0.35) 100%) !important; }
.menu-featured-logistics:hover { background: linear-gradient(135deg, rgba(230,162,60,0.6) 0%, rgba(200,120,20,0.5) 100%) !important; }
.menu-featured-dispatch { background: linear-gradient(135deg, rgba(64,158,255,0.4) 0%, rgba(22,93,255,0.3) 100%) !important; }
.menu-featured-dispatch:hover { background: linear-gradient(135deg, rgba(64,158,255,0.55) 0%, rgba(22,93,255,0.45) 100%) !important; }
.menu-featured-wms { background: linear-gradient(135deg, rgba(103,194,58,0.38) 0%, rgba(56,158,13,0.28) 100%) !important; }
.menu-featured-wms:hover { background: linear-gradient(135deg, rgba(103,194,58,0.52) 0%, rgba(56,158,13,0.42) 100%) !important; }
.menu-featured-tms { background: linear-gradient(135deg, rgba(144,147,153,0.45) 0%, rgba(96,98,102,0.35) 100%) !important; }
.menu-featured-tms:hover { background: linear-gradient(135deg, rgba(144,147,153,0.6) 0%, rgba(96,98,102,0.5) 100%) !important; }
.menu-featured-mm { background: linear-gradient(135deg, rgba(121,87,213,0.42) 0%, rgba(64,158,255,0.32) 100%) !important; }
.menu-featured-mm:hover { background: linear-gradient(135deg, rgba(121,87,213,0.58) 0%, rgba(64,158,255,0.48) 100%) !important; }
/* 智能工厂 / 智慧供应链 分组：标题略强调 */
.menu-cluster-title {
  font-weight: 600;
  letter-spacing: 0.04em;
}
.menu-cluster-title i { margin-right: 4px; opacity: 0.95; }
/* 子菜单下拉：科技感面板（挂载在 body） */
.menu-popper-factory.el-menu--popup,
.menu-popper-factory {
  min-width: 200px !important;
  border: 1px solid rgba(34, 211, 238, 0.35) !important;
  border-radius: 12px !important;
  background: linear-gradient(165deg, #0c1929 0%, #0f2847 100%) !important;
  box-shadow: 0 16px 40px rgba(0, 30, 60, 0.45) !important;
  padding: 6px 0 !important;
}
.menu-popper-factory .el-menu-item {
  color: #e0f2fe !important;
  font-weight: 500;
}
.menu-popper-factory .el-menu-item:hover,
.menu-popper-factory .el-menu-item:focus {
  background: rgba(34, 211, 238, 0.15) !important;
}
.menu-popper-factory .el-menu-item.is-active {
  color: #7dd3fc !important;
  background: rgba(34, 211, 238, 0.12) !important;
}
.menu-popper-supply.el-menu--popup,
.menu-popper-supply {
  min-width: 200px !important;
  border: 1px solid rgba(251, 191, 36, 0.35) !important;
  border-radius: 12px !important;
  background: linear-gradient(165deg, #1e1b4b 0%, #312e81 100%) !important;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.55) !important;
  padding: 6px 0 !important;
}
.menu-popper-supply .el-menu-item {
  color: #fef3c7 !important;
  font-weight: 500;
}
.menu-popper-supply .el-menu-item:hover,
.menu-popper-supply .el-menu-item:focus {
  background: rgba(251, 191, 36, 0.12) !important;
}
.menu-popper-supply .el-menu-item.is-active {
  color: #fcd34d !important;
  background: rgba(251, 191, 36, 0.1) !important;
}
.main-menu .el-submenu__title { font-weight: 600; letter-spacing: 0.02em; }
.menu-featured-pet { background: linear-gradient(135deg, rgba(255,143,179,0.45) 0%, rgba(200,120,160,0.35) 100%) !important; }
.menu-featured-pet:hover { background: linear-gradient(135deg, rgba(255,143,179,0.6) 0%, rgba(200,120,160,0.5) 100%) !important; }
.pet-menu-label { font-weight: 600; }
.menu-search { flex: 0 0 auto; padding: 0 12px !important; }
.menu-search .top-search-input { width: 180px; }
.menu-search .el-input__inner { background: rgba(255,255,255,0.2); border-color: rgba(255,255,255,0.4); color: #fff; }
.menu-search .el-input__inner::placeholder { color: rgba(255,255,255,0.7); }
.menu-search .el-input__prefix { color: rgba(255,255,255,0.8); }
.menu-right { margin-left: auto !important; }
.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.user-avatar:hover { background: rgba(255,255,255,0.1); }
.username { font-size: 14px; }
.profile-drawer { padding: 0 16px; }
.business-text { white-space: pre-wrap; word-break: break-all; }
.guest-tip { padding: 20px 0; text-align: center; }
.guest-tip .tip-text { color: #909399; font-size: 13px; margin: 12px 0; }
.search-results { min-height: 120px; }
.search-empty { padding: 40px; text-align: center; color: #909399; }
.search-result-list { max-height: 360px; overflow-y: auto; }
.search-result-item {
  display: flex; align-items: center; gap: 12px; padding: 10px 12px;
  border-bottom: 1px solid #ebeef5; cursor: pointer;
}
.search-result-item:hover { background: #f5f7fa; }
.result-type { flex-shrink: 0; font-size: 12px; color: #909399; width: 70px; }
.result-title { flex: 1; font-weight: 500; }
.result-sub { flex-shrink: 0; font-size: 12px; color: #909399; }

/* —— 移动端 / 窄屏：三栏改纵向，避免左右栏与主内容挤在同一行 —— */
@media screen and (max-width: 768px) {
  .app-layout {
    flex-direction: column;
    align-items: stretch;
    min-height: 100vh;
    width: 100%;
    max-width: 100vw;
    overflow-x: hidden;
    --sidebar-right-w: 0px;
  }
  /* 主内容在上，先读正文再展开菜单 */
  .layout-main {
    order: 1;
    flex: 1 1 auto;
    width: 100%;
    min-width: 0;
  }
  .layout-sidebar-left {
    order: 2;
    flex: 0 0 auto;
    width: 100%;
    min-width: 0;
    max-height: min(40vh, 320px);
    box-shadow: 0 4px 16px rgba(22, 93, 255, 0.12);
  }
  .layout-sidebar-right {
    order: 3;
    flex: 0 0 auto;
    width: 100%;
    min-width: 0;
    max-height: min(45vh, 380px);
    border-left: none;
    border-top: 1px solid rgba(255, 255, 255, 0.08);
    box-shadow: 0 -4px 20px rgba(22, 93, 255, 0.1);
  }
  .main-menu-vertical {
    max-height: min(38vh, 300px);
    -webkit-overflow-scrolling: touch;
  }
  .sidebar-brand {
    padding: 12px 14px 10px;
  }
  .sidebar-brand__mark {
    font-size: 18px;
  }
  .page-content {
    margin: 8px;
    padding: 12px;
  }
  /* 丝路贸易侧条：不占右侧栏宽度，改贴底避免挡两列菜单 */
  .app-layout .silk-road-panel-root .edge-trigger {
    right: max(8px, env(safe-area-inset-right, 0px));
    top: auto;
    bottom: max(100px, calc(88px + env(safe-area-inset-bottom, 0px)));
    transform: none;
    width: 40px;
    min-height: 88px;
    padding: 8px 4px;
    font-size: 11px;
    z-index: 2100;
  }
  /* 右下角反馈钮略上移，避免与系统手势条重叠 */
  .feedback-trigger {
    right: max(10px, env(safe-area-inset-right, 0px)) !important;
    bottom: max(10px, env(safe-area-inset-bottom, 0px)) !important;
    width: 48px !important;
    height: 48px !important;
  }
}
</style>
