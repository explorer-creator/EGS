<template>
  <div id="app">
    <!-- 未登录：显示登录/注册页 -->
    <LoginRegisterPage v-if="!currentUser" @login-success="onLoginSuccess" @guest-enter="onGuestEnter" />

    <!-- 已登录：主界面 -->
    <template v-else>
      <BackgroundDecor />
      <div class="main-content-layer">
        <!-- 欢迎烟花效果 -->
        <WelcomeFireworks v-if="showWelcome" :duration="5000" @done="showWelcome = false" />
        <!-- 顶部导航栏 -->
        <el-menu mode="horizontal" background-color="#165DFF" text-color="#fff" active-text-color="#ffd04b" class="main-menu">
        <el-menu-item index="0" @click="currentPage = 'AboutPage'">企业介绍</el-menu-item>
        <el-menu-item index="0b" @click="currentPage = 'PlanetReducerPage'">行星减速器原理</el-menu-item>
        <el-menu-item index="0c" @click="currentPage = 'ProcessIntroPage'">工序与工艺介绍</el-menu-item>
        <el-menu-item index="0d" @click="currentPage = 'ProductManufacturingPage'">产品制造问答</el-menu-item>
        <el-menu-item index="0e" @click="currentPage = 'CvInspectionPage'">视觉质检</el-menu-item>
        <el-menu-item index="green" class="menu-featured menu-featured-green" @click="currentPage = 'GreenProductionPage'">
          <i class="el-icon-sunny"></i> 绿色生产
        </el-menu-item>
        <el-menu-item index="business" class="menu-featured menu-featured-business" @click="currentPage = 'BusinessAnalysisPage'">
          <i class="el-icon-data-line"></i> 商业分析
        </el-menu-item>
        <el-menu-item index="1" @click="currentPage = 'EquipmentPage'">设备管理</el-menu-item>
        <el-menu-item index="2" @click="currentPage = 'MaterialPage'">物料管理</el-menu-item>
        <el-menu-item index="3" @click="currentPage = 'ProcedurePage'">工序管理</el-menu-item>
        <el-menu-item index="4" @click="currentPage = 'PlanPage'">工艺路线管理</el-menu-item>
        <el-menu-item index="guide" @click="currentPage = 'GuidePage'">使用指南</el-menu-item>
        <el-menu-item index="arch" @click="currentPage = 'ArchitecturePage'">架构先进性</el-menu-item>
        <el-menu-item index="export" @click="exportModelsToDisk">
          <i class="el-icon-download"></i> 导出模型
        </el-menu-item>
        <el-menu-item index="search" class="menu-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索设备、物料、工序、工艺路线、架构、xDM-F..."
            prefix-icon="el-icon-search"
            size="small"
            clearable
            class="top-search-input"
            @keyup.enter.native="onSearch"
          />
        </el-menu-item>
        <el-menu-item index="5" class="menu-right">
          <div class="user-avatar" @click="profileDrawerVisible = true">
            <el-avatar :size="32" icon="el-icon-user-solid" />
            <span class="username">{{ currentUser.isGuest ? '访客' : (currentUser.username || currentUser.user_name || '用户') }}</span>
          </div>
        </el-menu-item>
      </el-menu>

      <!-- 全局认证设置（遇 403 时必填） -->
      <el-card class="auth-card" shadow="never">
        <el-form inline size="small">
          <el-form-item label="Cookie（遇 403 必填）">
            <el-input v-model="xdmToken" type="textarea" :rows="2" placeholder="先访问 8003 登录，F12→Network→选中任意请求→Headers→复制 Request Headers 中的 Cookie 整段粘贴。需包含 SESSION= 和 XSRF-TOKEN= 或 TOKEN=，多行可自动合并" style="width: 560px;" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" @click="saveToken">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 页面内容区：keep-alive 保留产品问答等页面状态，跳转后返回不丢失 -->
      <div class="page-content" :class="{ 'page-content-transparent': isIntroPage }">
        <keep-alive :include="keepAlivePages">
          <component :is="currentPage" :key="currentPage" />
        </keep-alive>
      </div>

        <!-- 小机器人智能体：全页面可拖拽 -->
        <RobotAgent ref="robotAgent" />
        <!-- 反馈箱：右下角悬浮按钮 -->
        <FeedbackBox />
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
import BusinessAnalysisPage from './views/BusinessAnalysisPage.vue'
import LoginRegisterPage from './views/LoginRegisterPage.vue'
import BackgroundDecor from './components/BackgroundDecor.vue'
import WelcomeFireworks from './components/WelcomeFireworks.vue'
import FeedbackBox from './components/FeedbackBox.vue'
import RobotAgent from './components/RobotAgent.vue'
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
  { keywords: ['企业', '介绍', '公司'], page: 'AboutPage', typeLabel: '企业介绍', title: '企业介绍', sub: '企业概况' },
  { keywords: ['行星', '减速器', '原理'], page: 'PlanetReducerPage', typeLabel: '行星减速器', title: '行星减速器原理', sub: '原理介绍' },
  { keywords: ['工序', '工艺', '介绍'], page: 'ProcessIntroPage', typeLabel: '工序工艺', title: '工序与工艺介绍', sub: '工序与工艺说明' },
  { keywords: ['产品', '制造', '问答', '千问', 'LLM', '原材料', '设备', '耳机', '充电宝'], page: 'ProductManufacturingPage', typeLabel: '产品制造', title: '产品制造知识问答', sub: 'AI 输出原材料、设备、工序、工艺流程、物理原理' },
  { keywords: ['视觉', '质检', 'CV', 'PCB', '缺陷', '漏焊', '短路', '断路', '报废'], page: 'CvInspectionPage', typeLabel: '视觉质检', title: 'PCB 裸板视觉质检', sub: '缺陷识别、坐标同步、自动报废' },
  { keywords: ['绿色', '生产', '低碳', '碳足迹', 'PUE', '供应链', '环保'], page: 'GreenProductionPage', typeLabel: '绿色生产', title: '绿色生产', sub: '能效监测、碳足迹、绿色供应链、零浪费' },
  { keywords: ['商业', '分析', '报价', '敏感度', '利润', '市场策略'], page: 'BusinessAnalysisPage', typeLabel: '商业分析', title: '商业分析', sub: '利润敏感度、智能报价、最优生产方案' }
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
    BusinessAnalysisPage,
    LoginRegisterPage,
    WelcomeFireworks,
    FeedbackBox,
    RobotAgent
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
    isIntroPage() {
      return ['AboutPage', 'PlanetReducerPage', 'ProcessIntroPage', 'ProductManufacturingPage', 'CvInspectionPage', 'GreenProductionPage', 'BusinessAnalysisPage', 'GuidePage', 'ArchitecturePage'].includes(this.currentPage)
    },
    keepAlivePages() {
      return 'ProductManufacturingPage,GreenProductionPage,BusinessAnalysisPage'
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
      xdmToken: localStorage.getItem('xdm_token') || '',
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
    this.xdmToken = localStorage.getItem('xdm_token') || ''
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
    saveToken() {
      const t = (this.xdmToken || '').trim()
      if (t) {
        localStorage.setItem('xdm_token', t)
        this.$message.success('已保存，请重新尝试查询')
      } else {
        localStorage.removeItem('xdm_token')
        this.$message.info('已清除')
      }
    }
  }
}
</script>

<style>
.auth-card {
  margin: 12px 20px 0;
  padding: 8px 16px;
  background: #fff !important;
}
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
.main-menu { display: flex; flex-wrap: wrap; align-items: center; }
.menu-featured { font-weight: 600 !important; }
.menu-featured i { margin-right: 4px; }
.menu-featured-green { background: linear-gradient(135deg, rgba(82,196,26,0.35) 0%, rgba(56,158,13,0.25) 100%) !important; }
.menu-featured-green:hover { background: linear-gradient(135deg, rgba(82,196,26,0.5) 0%, rgba(56,158,13,0.4) 100%) !important; }
.menu-featured-business { background: linear-gradient(135deg, rgba(22,93,255,0.4) 0%, rgba(14,66,194,0.3) 100%) !important; }
.menu-featured-business:hover { background: linear-gradient(135deg, rgba(22,93,255,0.55) 0%, rgba(14,66,194,0.45) 100%) !important; }
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
</style>
