<template>
  <div id="app">
    <!-- 背景音乐：入口见 LoginRegisterPage；登录后主界面右上角 -->
    <BackgroundMusic v-if="currentUser" />
    <!-- 未登录：显示登录/注册页 -->
    <LoginRegisterPage v-if="!currentUser" @login-success="onLoginSuccess" @guest-enter="onGuestEnter" />

    <!-- 已登录：主界面 -->
    <template v-else>
      <BackgroundDecor />
      <div class="main-content-layer app-shell-v2" :class="{ 'app-shell-v2--mobile': isMobileLayout }">
        <!-- 欢迎烟花效果 -->
        <WelcomeFireworks v-if="showWelcome" :duration="5000" @done="showWelcome = false" />

        <!-- 快看式：大黑框约 2/3 屏高；EGS 与工具条嵌入黑框角上 -->
        <div class="kk-hero-shell">
          <div class="kk-hero-brand">
            <span class="kk-hero-brand__mark">EGS</span>
            <span class="kk-hero-brand__sub">智慧制造</span>
          </div>
          <div class="kk-hero-toolbar">
            <el-button size="mini" icon="el-icon-download" class="kk-hero-toolbar-btn" @click="exportModelsToDisk">导出</el-button>
            <div class="rail-user kk-hero-user" @click="profileDrawerVisible = true">
              <el-avatar :size="28" icon="el-icon-user-solid" />
              <span class="rail-user-name kk-hero-user-name">{{ currentUser.isGuest ? '访客' : (currentUser.username || currentUser.user_name || '用户') }}</span>
            </div>
          </div>
          <nav class="kk-pill-strip kk-pill-strip--top" aria-label="主导航">
            <button
              v-for="item in leftNavItems"
              :key="item.idx"
              type="button"
              class="kk-pill"
              :class="['kk-pill--' + (item.tone || 'default'), { 'is-active': leftMenuActive === item.idx }]"
              @click="onLeftMenuSelect(item.idx)"
            >
              <i v-if="item.icon" :class="item.icon"></i>
              {{ item.label }}
            </button>
          </nav>
          <div class="kk-hero-carousel-wrap">
            <SilkAiCarousel hero fill-container embedded class="app-hero-carousel" />
          </div>
          <nav class="kk-pill-strip kk-pill-strip--bottom" aria-label="运营与物流">
            <button
              v-for="item in rightNavItems"
              :key="item.idx"
              type="button"
              class="kk-pill"
              :class="['kk-pill--' + (item.tone || 'default'), { 'is-active': rightMenuActive === item.idx }]"
              @click="onRightMenuSelect(item.idx)"
            >
              <i v-if="item.icon" :class="item.icon"></i>
              {{ item.label }}
            </button>
          </nav>
        </div>

        <!-- 页面内容区：左右留白放装饰图（不遮挡中间正文）；窄屏改为横幅双图 -->
        <div class="app-page-with-gutters" :class="{ 'app-page-with-gutters--stack': isMobileLayout }">
          <div v-if="isMobileLayout" class="app-page-gutter-mobile" aria-hidden="true">
            <div class="app-page-gutter-mobile__cell">
              <div class="app-page-gutter-mobile__pulse app-page-gutter-mobile__pulse--delay">
                <img class="app-page-gutter-mobile__img" :src="pageMascotPair.left" alt="" draggable="false" />
              </div>
            </div>
            <div class="app-page-gutter-mobile__cell">
              <div class="app-page-gutter-mobile__pulse">
                <img class="app-page-gutter-mobile__img" :src="pageMascotPair.right" alt="" draggable="false" />
              </div>
            </div>
          </div>
          <aside v-if="!isMobileLayout" class="app-page-gutter app-page-gutter--left" aria-hidden="true">
            <div class="app-page-gutter__pulse app-page-gutter__pulse--delay">
              <img class="app-page-gutter__img" :src="pageMascotPair.left" alt="" draggable="false" />
            </div>
          </aside>
          <div
            class="page-content"
            :class="{ 'page-content-transparent': isIntroPage, 'page-content--pb-shell': currentPage === 'PictureBookLikeShellDemoPage' }"
          >
            <keep-alive :include="keepAlivePages">
              <component :is="currentPage" :key="currentPage" />
            </keep-alive>
          </div>
          <aside v-if="!isMobileLayout" class="app-page-gutter app-page-gutter--right" aria-hidden="true">
            <div class="app-page-gutter__pulse">
              <img class="app-page-gutter__img" :src="pageMascotPair.right" alt="" draggable="false" />
            </div>
          </aside>
        </div>

        <!-- 小机器人智能体：左邻搜索小框，可拖拽 -->
        <RobotAgent
          ref="robotAgent"
          :search-keyword.sync="searchKeyword"
          @search="onSearch"
        />
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
import PictureBookLikeShellDemoPage from './views/PictureBookLikeShellDemoPage.vue'
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
import BackgroundMusic from './components/BackgroundMusic.vue'
import SilkAiCarousel from './components/SilkAiCarousel.vue'
import { config } from './config'

import { pageMascotPairForKey } from './utils/mascotPair'
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
  { keywords: ['主内容布局', '黑框', '平台与展示', '制造与协同', '丝路扩展', '布局演示'], page: 'PictureBookLikeShellDemoPage', typeLabel: '布局演示', title: '主内容布局', sub: '上下分区入口 + 中间业务区' },
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
    PictureBookLikeShellDemoPage,
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
    BackgroundMusic,
    SilkAiCarousel
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
        MaterialImageModulePage: 'material-a',
        PictureBookLikeShellDemoPage: 'pb-shell'
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
      return ['AboutPage', 'PlanetReducerPage', 'ProcessIntroPage', 'ProductManufacturingPage', 'CvInspectionPage', 'GreenProductionPage', 'GreenCarbonFootprintPage', 'BusinessAnalysisPage', 'TradeVisualizationPage', 'AiBlockchainHubPage', 'CrossBorderRailHubPage', 'ReportExportPage', 'MaterialImageModulePage', 'PictureBookLikeShellDemoPage', 'SmartLogisticsPage', 'WarehouseDispatchPage', 'WmsSmartSystemPage', 'TmsSmartPage', 'MultimodalLogisticsPage', 'MapTransportPage', 'PetExpressPage', 'GuidePage', 'ArchitecturePage'].includes(this.currentPage)
    },
    keepAlivePages() {
      return 'ProductManufacturingPage,GreenProductionPage,GreenCarbonFootprintPage,BusinessAnalysisPage,TradeVisualizationPage,AiBlockchainHubPage,CrossBorderRailHubPage,ReportExportPage,MaterialImageModulePage,PictureBookLikeShellDemoPage,SmartLogisticsPage,WarehouseDispatchPage,WmsSmartSystemPage,TmsSmartPage,MultimodalLogisticsPage,MapTransportPage,PetExpressPage'
    },
    /** 窄屏与 CSS 媒体查询双保险，避免侧栏 min-width 挤成两列 */
    isMobileLayout() {
      return this.layoutViewportWidth <= 1024
    },
    /** 按当前页面名稳定哈希选两张不同图：每页固定一对，页与页不同 */
    pageMascotPair() {
      return pageMascotPairForKey(this.currentPage)
    }
  },
  data() {
    return {
      layoutViewportWidth: typeof window !== 'undefined' ? window.innerWidth : 1200,
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
      },
      leftNavItems: [
        { idx: '0', label: '平台介绍', tone: 'default' },
        { idx: '0b', label: '行星减速器原理', tone: 'default' },
        { idx: '0c', label: '工序与工艺介绍', tone: 'default' },
        { idx: '0d', label: '产品制造问答', tone: 'default' },
        { idx: '0e', label: '视觉质检', tone: 'default' },
        { idx: 'green', label: '绿能排产演示', icon: 'el-icon-data-analysis', tone: 'green' },
        { idx: 'trade-viz', label: '贸易可视化', icon: 'el-icon-pie-chart', tone: 'trade' },
        { idx: 'ai-chain', label: 'AI+链集成', icon: 'el-icon-link', tone: 'aichain' },
        { idx: 'rail-ops', label: '丝路班列协同', icon: 'el-icon-data-board', tone: 'rail' },
        { idx: 'report', label: '生成综合报告', icon: 'el-icon-document-checked', tone: 'report' },
        { idx: 'material-a', label: '智能物料配图', icon: 'el-icon-picture', tone: 'mat' },
        { idx: 'pb-shell', label: '主内容布局', icon: 'el-icon-menu', tone: 'default' }
      ],
      rightNavItems: [
        { idx: '1', label: '设备孪生 · 台账', icon: 'el-icon-cpu', tone: 'default' },
        { idx: '3', label: '工序编排 · 执行', icon: 'el-icon-s-operation', tone: 'default' },
        { idx: '2', label: '物料中枢 · 主数据', icon: 'el-icon-s-cooperation', tone: 'default' },
        { idx: '4', label: '工艺路线 · 编排', icon: 'el-icon-share', tone: 'default' },
        { idx: 'logistics', label: '智慧物流', icon: 'el-icon-truck', tone: 'logistics' },
        { idx: 'warehouse-dispatch', label: '仓库调度', icon: 'el-icon-office-building', tone: 'dispatch' },
        { idx: 'wms-smart', label: '智能WMS', icon: 'el-icon-box', tone: 'wms' },
        { idx: 'tms-smart', label: '智慧TMS', icon: 'el-icon-position', tone: 'tms' },
        { idx: 'multimodal', label: '多模态报单', icon: 'el-icon-camera', tone: 'mm' },
        { idx: 'map', label: '运输地图', icon: 'el-icon-location', tone: 'default' },
        { idx: 'pet', label: '🐾 爱宠专送', tone: 'pet' },
        { idx: 'guide', label: '使用指南', tone: 'default' },
        { idx: 'arch', label: '架构先进性', tone: 'default' }
      ]
    }
  },
  mounted() {
    this.syncViewportWidth()
    window.addEventListener('resize', this.syncViewportWidth, { passive: true })
    window.addEventListener('orientationchange', this.syncViewportWidth, { passive: true })
    const saved = localStorage.getItem('current_user')
    if (saved) {
      try {
        this.currentUser = JSON.parse(saved)
      } catch (e) {
        localStorage.removeItem('current_user')
      }
    }
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.syncViewportWidth)
    window.removeEventListener('orientationchange', this.syncViewportWidth)
  },
  methods: {
    syncViewportWidth() {
      if (typeof window === 'undefined') return
      this.layoutViewportWidth = window.innerWidth
    },
    /** Element UI 菜单须用 @select，仅用子项 @click 时选中态与 currentPage 可能不同步，导致「点了菜单页面不变」 */
    onLeftMenuSelect(index) {
      const map = {
        '0': 'AboutPage',
        '0b': 'PlanetReducerPage',
        '0c': 'ProcessIntroPage',
        '0d': 'ProductManufacturingPage',
        '0e': 'CvInspectionPage',
        green: 'GreenCarbonFootprintPage',
        'trade-viz': 'TradeVisualizationPage',
        'ai-chain': 'AiBlockchainHubPage',
        'rail-ops': 'CrossBorderRailHubPage',
        report: 'ReportExportPage',
        'material-a': 'MaterialImageModulePage',
        'pb-shell': 'PictureBookLikeShellDemoPage'
      }
      const page = map[index]
      if (page) this.currentPage = page
    },
    onRightMenuSelect(index) {
      const map = {
        '1': 'EquipmentPage',
        '2': 'MaterialPage',
        '3': 'ProcedurePage',
        '4': 'PlanPage',
        logistics: 'SmartLogisticsPage',
        'warehouse-dispatch': 'WarehouseDispatchPage',
        'wms-smart': 'WmsSmartSystemPage',
        'tms-smart': 'TmsSmartPage',
        multimodal: 'MultimodalLogisticsPage',
        map: 'MapTransportPage',
        pet: 'PetExpressPage',
        guide: 'GuidePage',
        arch: 'ArchitecturePage'
      }
      const page = map[index]
      if (page) this.currentPage = page
    },
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
/* 移动端全屏与安全区：避免横向溢出与刘海遮挡 */
#app {
  min-height: 100vh;
  min-height: 100dvh;
  width: 100%;
  max-width: 100vw;
  overflow-x: clip;
  box-sizing: border-box;
}
/* 中间正文 + 左右留白装饰（与正文并排，不叠在卡片上） */
.app-page-with-gutters {
  display: flex;
  flex-direction: row;
  align-items: stretch;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  gap: 6px;
  flex: 1;
  min-height: 0;
}
.app-page-with-gutters--stack {
  flex-direction: column;
  align-items: stretch;
  gap: 0;
}
.app-page-gutter-mobile {
  display: flex;
  flex-direction: row;
  align-items: flex-end;
  justify-content: center;
  gap: min(5vw, 24px);
  width: 100%;
  box-sizing: border-box;
  padding: 6px 10px 2px;
  flex-shrink: 0;
  pointer-events: none;
  user-select: none;
}
.app-page-gutter-mobile__cell {
  flex: 1 1 0;
  min-width: 0;
  max-width: 46%;
  display: flex;
  justify-content: center;
  align-items: flex-end;
}
.app-page-gutter-mobile__pulse {
  transform-origin: 50% 88%;
  animation: app-gutter-pulse 3.6s ease-in-out infinite;
}
.app-page-gutter-mobile__pulse--delay {
  animation-delay: -1.8s;
}
.app-page-gutter-mobile__img {
  display: block;
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: min(26vw, 150px);
  object-fit: contain;
  mix-blend-mode: multiply;
  opacity: 0.92;
  filter: saturate(1.05) drop-shadow(0 6px 16px rgba(30, 27, 75, 0.12));
}
.app-page-gutter {
  flex: 0 0 clamp(108px, 18vw, 240px);
  max-width: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 4px;
  position: relative;
  z-index: 0;
  pointer-events: none;
  user-select: none;
}
.app-page-gutter__pulse {
  transform-origin: 50% 88%;
  animation: app-gutter-pulse 3.6s ease-in-out infinite;
}
.app-page-gutter__pulse--delay {
  animation-delay: -1.8s;
}
@keyframes app-gutter-pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}
.app-page-gutter__img {
  display: block;
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: min(58vh, 560px);
  object-fit: contain;
  object-position: center;
  /* 浅色渐变底上弱化白边；复杂实景图可换透明 PNG */
  mix-blend-mode: multiply;
  opacity: 0.9;
  filter: saturate(1.05) drop-shadow(0 10px 28px rgba(30, 27, 75, 0.12));
}

.page-content {
  margin: 20px;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fff;
}
.app-page-with-gutters .page-content {
  flex: 1;
  min-width: 0;
  margin: 16px 6px 20px;
}
.page-content-transparent {
  background: transparent !important;
  border-color: transparent !important;
}
/* 主内容布局：去掉白卡片外框，让壳层铺满主栏 */
.page-content--pb-shell {
  margin: 8px 12px 20px;
  padding: 0 !important;
  border: none !important;
  background: transparent !important;
}
.main-content-layer {
  position: relative;
  z-index: 1;
}

/* 全站壳：黑框主视觉 + 页面内容（顶栏已并入黑框角 / 搜索并入智能体） */
.app-shell-v2 {
  --sidebar-right-w: 0px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  padding: 4px 12px 20px;
  padding-left: max(12px, env(safe-area-inset-left, 0px));
  padding-right: max(12px, env(safe-area-inset-right, 0px));
  padding-bottom: max(20px, env(safe-area-inset-bottom, 0px));
  align-items: stretch;
}
.app-shell-v2 > .kk-hero-shell {
  flex-shrink: 0;
  width: 100%;
}
/* 快看式主视觉：约 2/3 视口高，上下胶囊嵌入黑框；角标与工具条叠在框内 */
.kk-hero-shell {
  position: relative;
  height: 66vh;
  min-height: 280px;
  max-height: min(920px, 80vh);
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(165deg, #12121a 0%, #06060a 100%);
  box-shadow:
    0 20px 50px rgba(0, 0, 0, 0.38),
    inset 0 1px 0 rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.kk-hero-brand {
  position: absolute;
  top: 12px;
  left: 14px;
  z-index: 8;
  display: flex;
  flex-direction: column;
  gap: 2px;
  pointer-events: none;
}
.kk-hero-brand__mark {
  font-size: 22px;
  font-weight: 800;
  color: #fff;
  line-height: 1.1;
  letter-spacing: 0.06em;
  text-shadow:
    0 2px 14px rgba(0, 0, 0, 0.9),
    0 0 2px rgba(0, 0, 0, 0.95);
}
.kk-hero-brand__sub {
  font-size: 11px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.88);
  letter-spacing: 0.08em;
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.85);
}
.kk-hero-toolbar {
  position: absolute;
  top: 8px;
  right: 10px;
  z-index: 8;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  max-width: calc(100% - 120px);
  pointer-events: auto;
}
.kk-hero-toolbar-btn {
  color: #fff !important;
  background: linear-gradient(145deg, rgba(194, 65, 12, 0.85) 0%, rgba(234, 88, 12, 0.8) 50%, rgba(185, 28, 28, 0.85) 100%) !important;
  border: 1px solid rgba(253, 186, 116, 0.55) !important;
  box-shadow: 0 2px 10px rgba(220, 38, 38, 0.25);
}
.kk-hero-toolbar-btn:hover {
  background: linear-gradient(145deg, #ea580c 0%, #f97316 45%, #ef4444 100%) !important;
  border-color: rgba(255, 237, 213, 0.65) !important;
}
.kk-hero-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  cursor: pointer;
  background: linear-gradient(145deg, rgba(120, 53, 15, 0.55) 0%, rgba(154, 52, 18, 0.5) 100%);
  border: 1px solid rgba(255, 149, 99, 0.45);
  transition: background 0.15s, border-color 0.15s;
}
.kk-hero-user:hover {
  background: linear-gradient(145deg, rgba(234, 88, 12, 0.55) 0%, rgba(220, 38, 38, 0.5) 100%);
  border-color: rgba(253, 186, 116, 0.55);
}
.kk-hero-user-name {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kk-hero-carousel-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  position: relative;
}
.kk-pill-strip {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 8px 10px;
  padding: 10px 12px;
  z-index: 3;
}
.kk-pill-strip--top {
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.9) 0%, rgba(0, 0, 0, 0.4) 70%, transparent 100%);
  padding-top: 52px;
  padding-bottom: 12px;
}
.kk-pill-strip--bottom {
  background: linear-gradient(0deg, rgba(0, 0, 0, 0.9) 0%, rgba(0, 0, 0, 0.4) 70%, transparent 100%);
  padding-top: 12px;
}
.kk-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 149, 99, 0.55);
  background: linear-gradient(145deg, rgba(194, 65, 12, 0.72) 0%, rgba(234, 88, 12, 0.65) 45%, rgba(185, 28, 28, 0.7) 100%);
  color: rgba(255, 255, 255, 0.98);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s, border-color 0.18s, box-shadow 0.18s, transform 0.12s;
  font-family: inherit;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  box-shadow: 0 2px 10px rgba(220, 38, 38, 0.22);
}
.kk-pill:hover {
  border-color: rgba(253, 186, 116, 0.95);
  background: linear-gradient(145deg, #ea580c 0%, #f97316 42%, #ef4444 100%);
  box-shadow: 0 4px 18px rgba(249, 115, 22, 0.45);
}
.kk-pill.is-active {
  background: linear-gradient(145deg, #fb923c 0%, #f97316 38%, #ef4444 72%, #dc2626 100%);
  border-color: rgba(255, 237, 213, 0.55);
  color: #fff;
  box-shadow:
    0 4px 20px rgba(249, 115, 22, 0.55),
    0 0 0 1px rgba(255, 255, 255, 0.12) inset;
}
.kk-pill.is-active i {
  color: #fff !important;
}
.kk-pill i {
  color: rgba(255, 247, 237, 0.95);
  font-size: 13px;
}
/* 各 tone 仅在橙红系内微调描边，保持统一暖色 */
.kk-pill--green:not(.is-active),
.kk-pill--trade:not(.is-active),
.kk-pill--aichain:not(.is-active),
.kk-pill--rail:not(.is-active),
.kk-pill--report:not(.is-active),
.kk-pill--mat:not(.is-active),
.kk-pill--logistics:not(.is-active),
.kk-pill--dispatch:not(.is-active),
.kk-pill--wms:not(.is-active),
.kk-pill--tms:not(.is-active),
.kk-pill--mm:not(.is-active),
.kk-pill--pet:not(.is-active) {
  border-color: rgba(255, 159, 107, 0.5);
}
.app-hero-carousel {
  width: 100%;
  max-width: 100%;
  flex: 1;
  min-height: 0;
  align-self: stretch;
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
/* 丝路贸易悬浮条：有侧栏时让出宽度；新壳无侧栏时贴右 */
.app-layout .silk-road-panel-root .edge-trigger,
.app-shell-v2 .silk-road-panel-root .edge-trigger {
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

/* —— 移动端 / 平板竖屏：三栏纵向；勿限制侧栏高度，否则 el-submenu 展开被裁切 —— */
@media screen and (max-width: 1024px) {
  .app-layout {
    flex-direction: column !important;
    align-items: stretch;
    min-height: 100vh;
    width: 100%;
    max-width: 100vw;
    overflow-x: hidden;
    --sidebar-right-w: 0px;
  }
  .layout-main {
    order: 1;
    flex: 1 1 auto;
    width: 100% !important;
    min-width: 0 !important;
  }
  .layout-sidebar-left {
    order: 2;
    flex: 0 0 auto !important;
    width: 100% !important;
    min-width: 0 !important;
    max-width: 100% !important;
    max-height: none !important;
    overflow: visible !important;
    box-shadow: 0 4px 16px rgba(22, 93, 255, 0.12);
  }
  .layout-sidebar-right {
    order: 3;
    flex: 0 0 auto !important;
    width: 100% !important;
    min-width: 0 !important;
    max-width: 100% !important;
    max-height: none !important;
    overflow: visible !important;
    border-left: none;
    border-top: 1px solid rgba(255, 255, 255, 0.08);
    box-shadow: 0 -4px 20px rgba(22, 93, 255, 0.1);
  }
  .main-menu-vertical {
    max-height: none !important;
    overflow-y: visible !important;
    overflow-x: hidden;
    -webkit-overflow-scrolling: touch;
  }
  .main-menu-vertical .el-submenu .el-menu {
    overflow: visible !important;
  }
  .main-menu-vertical .el-submenu__title {
    touch-action: manipulation;
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
  .app-shell-v2 .page-content {
    margin: 8px;
    padding: 12px;
  }
  .app-page-with-gutters .page-content {
    margin: 8px;
    padding: 12px;
  }
  .kk-hero-shell {
    height: min(52vh, 420px);
    min-height: 220px;
    max-height: 72vh;
    border-radius: 12px;
  }
  .kk-pill {
    padding: 5px 10px;
    font-size: 11px;
    gap: 4px;
  }
  .kk-pill i {
    font-size: 12px;
  }
  .kk-pill-strip {
    gap: 6px 8px;
    padding-left: 8px;
    padding-right: 8px;
  }
  .kk-pill-strip--top {
    padding-top: 46px;
  }
  .kk-hero-brand__mark {
    font-size: 18px;
  }
  .kk-hero-toolbar {
    max-width: calc(100% - 96px);
  }
  .kk-hero-user-name {
    max-width: 64px;
  }
  .app-shell-v2 {
    padding-left: max(6px, env(safe-area-inset-left, 0px));
    padding-right: max(6px, env(safe-area-inset-right, 0px));
  }
  .app-layout .silk-road-panel-root .edge-trigger,
  .app-shell-v2 .silk-road-panel-root .edge-trigger {
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
  .feedback-trigger {
    right: max(10px, env(safe-area-inset-right, 0px)) !important;
    bottom: max(10px, env(safe-area-inset-bottom, 0px)) !important;
    width: 48px !important;
    height: 48px !important;
  }
}
/* JS 检测窄屏时再加类，与上面规则一致（双保险） */
.app-layout.app-layout--mobile {
  flex-direction: column !important;
  align-items: stretch;
  min-height: 100vh;
  width: 100%;
  max-width: 100vw;
  overflow-x: hidden;
  --sidebar-right-w: 0px;
}
.app-layout.app-layout--mobile .layout-main {
  order: 1;
  flex: 1 1 auto;
  width: 100% !important;
  min-width: 0 !important;
}
.app-layout.app-layout--mobile .layout-sidebar-left {
  order: 2;
  flex: 0 0 auto !important;
  width: 100% !important;
  min-width: 0 !important;
  max-width: 100% !important;
  max-height: none !important;
  overflow: visible !important;
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.12);
}
.app-layout.app-layout--mobile .layout-sidebar-right {
  order: 3;
  flex: 0 0 auto !important;
  width: 100% !important;
  min-width: 0 !important;
  max-width: 100% !important;
  max-height: none !important;
  overflow: visible !important;
  border-left: none;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 -4px 20px rgba(22, 93, 255, 0.1);
}
.app-layout.app-layout--mobile .main-menu-vertical {
  max-height: none !important;
  overflow-y: visible !important;
  overflow-x: hidden;
}
.app-layout.app-layout--mobile .main-menu-vertical .el-submenu .el-menu {
  overflow: visible !important;
}
.app-layout.app-layout--mobile .main-menu-vertical .el-submenu__title {
  touch-action: manipulation;
}
.app-layout.app-layout--mobile .page-content {
  margin: 8px;
  padding: 12px;
}
.app-layout.app-layout--mobile .silk-road-panel-root .edge-trigger,
.app-shell-v2.app-shell-v2--mobile .silk-road-panel-root .edge-trigger {
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
.app-layout.app-layout--mobile .feedback-trigger,
.app-shell-v2.app-shell-v2--mobile .feedback-trigger {
  right: max(10px, env(safe-area-inset-right, 0px)) !important;
  bottom: max(10px, env(safe-area-inset-bottom, 0px)) !important;
  width: 48px !important;
  height: 48px !important;
}
</style>
