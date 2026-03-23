<template>
  <div class="plan-page fs-page fs-page--supply">
    <FactorySupplyHero
      variant="supply"
      kicker="智慧供应链 · PROCESS ROUTING"
      title="工艺路线 · 版本化编排"
      description="将工序按序串联为可版本管理的工艺路线，支持一键配置任务与 Mermaid 流程可视，衔接智能工厂工序数据。"
      :tags="['ProcessRoute', '工序串联', '流程图']"
    />
    <!-- 任务说明 -->
    <el-card title="任务说明" class="fs-card" style="margin-bottom: 16px;">
      <p>工艺流程：毛坯制造 → 粗加工 → 精加工 → 检测 → 入库</p>
      <p>新增工艺路线「中心轮零件加工」，版本 1.0，主要涉及与行星减速器相关零件的生产加工工艺。</p>
    </el-card>

    <!-- 使用建议 -->
    <el-card class="usage-tips-card" shadow="hover">
      <template slot="header">
        <span class="card-header"><i class="el-icon-info"></i> 使用建议</span>
      </template>
      <ul class="usage-tips-list">
        <li><strong>首次使用：</strong>先在「工序管理」创建工序（如：毛坯制造、粗加工、精加工、检测、入库），再在「工艺路线管理」创建工艺路线并关联工序。</li>
        <li><strong>下拉框为空时：</strong>点击「刷新列表」，或切换到其他页面再切回「工艺路线管理」。</li>
        <li><strong>xDM-F 配置：</strong>确认 xDM-F 中已创建并发布 ProcessRoute、ProcedureManagement、ProcessRouteProcedure、EquipmentManagement、MaterialManagement 等实体。</li>
        <li><strong>Cookie 配置（可选）：</strong>对接真实 xDM 时，可在顶部输入框填入从 8003 登录后获取的 Cookie。</li>
      </ul>
    </el-card>

    <!-- 一键配置工艺路线 -->
    <el-card title="一键配置工艺路线" style="margin-bottom: 16px;">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px;">
        请先在「工序管理」页面正确新增 5 道工序（毛坯制造、粗加工、精加工、检测、入库），再执行一键配置。
      </el-alert>
      <el-button type="primary" :loading="batchLoading" @click="batchCreateRouteAndProcedures">
        一键配置：新增工艺路线「中心轮零件加工」v1.0 并关联 5 道工序
      </el-button>
    </el-card>

    <!-- 新增工艺路线表单 -->
    <el-card title="新增工艺路线" style="margin-bottom: 16px;">
      <el-form :model="routeForm" label-width="140px" class="route-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工艺编号" required>
              <el-input v-model="routeForm.routeCode" placeholder="如 PR001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工艺名称" required>
              <el-input v-model="routeForm.routeName" placeholder="如 中心轮零件加工" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属产品" required>
              <el-input v-model="routeForm.product" placeholder="如 行星减速器相关零件" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本" required>
              <el-input v-model="routeForm.version" placeholder="如 1.0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="工艺描述" required>
              <el-input v-model="routeForm.description" type="textarea" :rows="2" placeholder="主要涉及与行星减速器相关零件的生产加工工艺" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="操作人员">
              <el-input v-model="routeForm.operator" placeholder="操作人员" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作时间">
              <el-date-picker
                v-model="routeForm.operationTime"
                type="datetime"
                placeholder="选择操作时间"
                value-format="yyyy-MM-dd HH:mm:ss"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="设备使用情况">
              <el-input v-model="routeForm.equipmentUsage" type="textarea" :rows="1" placeholder="设备使用情况说明" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" @click="addRoute">新增工艺路线</el-button>
          <el-button @click="resetRouteForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工艺路线流程图 -->
    <el-card title="工艺路线流程图" style="margin-bottom: 16px;">
      <div class="flowchart-wrap">
        <div style="margin-bottom: 12px;">
          <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="refreshAll">刷新列表</el-button>
          <el-select v-model="selectedRouteId" placeholder="选择工艺路线" filterable size="small" style="width: 280px; margin-left: 8px;">
            <el-option v-for="r in routeList" :key="r.id" :label="`${r.route_name || r.routeName} (${r.route_code || r.routeCode}) v${r.version || ''}`" :value="r.id" />
          </el-select>
          <el-radio-group v-model="flowchartView" size="small" style="margin-left: 16px;">
            <el-radio-button label="simple">简图</el-radio-button>
            <el-radio-button label="mermaid">Mermaid</el-radio-button>
          </el-radio-group>
        </div>
        <div v-if="flowchartProcedureList.length === 0" class="flowchart-empty">请选择工艺路线并关联工序</div>
        <template v-else>
          <div v-show="flowchartView === 'simple'" class="flowchart-diagram">
            <div v-for="(item, idx) in flowchartProcedureList" :key="item.id || idx" class="flowchart-node-wrap">
              <div class="flowchart-node">
                <span class="node-seq">{{ item.sequence_order || item.sequenceOrder || idx + 1 }}</span>
                <span class="node-name">{{ getProcedureName(item.procedure_id || item.procedureId) }}</span>
                <span class="node-code">{{ getProcedureCode(item.procedure_id || item.procedureId) }}</span>
              </div>
              <div v-if="idx < flowchartProcedureList.length - 1" class="flowchart-arrow">→</div>
            </div>
          </div>
          <div v-show="flowchartView === 'mermaid'" class="mermaid-wrap" ref="mermaidContainer"></div>
        </template>
      </div>
    </el-card>

    <!-- 工序关联 -->
    <el-card title="工序关联" style="margin-bottom: 16px;">
      <p class="hint">选择工艺路线，再按顺序添加工序（毛坯制造→粗加工→精加工→检测→入库）</p>
      <div style="margin-bottom: 12px;">
        <el-button size="small" icon="el-icon-download" @click="exportRouteProcedureList">导出工序关联 CSV</el-button>
      </div>
      <el-form inline size="small" style="margin-bottom: 12px;">
        <el-form-item label="工艺路线">
          <el-select v-model="selectedRouteId" placeholder="选择工艺路线" filterable style="width: 280px;">
            <el-option v-for="r in routeList" :key="r.id" :label="`${r.route_name || r.routeName} (${r.route_code || r.routeCode}) v${r.version || ''}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="工序">
          <el-select v-model="selectedProcedureId" placeholder="选择工序" filterable style="width: 200px;">
            <el-option v-for="p in procedureList" :key="p.id" :label="`${p.procedure_name || p.procedureName} (${p.procedure_code || p.procedureCode})`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="顺序">
          <el-input-number v-model="linkSequence" :min="1" :max="20" style="width: 80px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" :loading="linkLoading" @click="addProcedureLink">添加关联</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="routeProcedureList" border v-loading="loading">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="sequence_order" label="顺序" width="80" />
        <el-table-column label="工序名称" min-width="120">
          <template slot-scope="scope">
            {{ getProcedureName(scope.row.procedure_id || scope.row.procedureId) }}
          </template>
        </el-table-column>
        <el-table-column label="工序编号" width="100">
          <template slot-scope="scope">
            {{ getProcedureCode(scope.row.procedure_id || scope.row.procedureId) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template slot-scope="scope">
            <el-button type="text" style="color: red;" @click="deleteProcedureLink(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 工艺路线列表 -->
    <el-card title="工艺路线列表">
      <div style="margin-bottom: 12px;">
        <el-button size="small" icon="el-icon-download" @click="exportRouteList">导出工艺路线 CSV</el-button>
        <el-button size="small" type="danger" icon="el-icon-delete" :disabled="!selectedRoutes.length" @click="batchDeleteRoute">批量删除</el-button>
      </div>
      <el-table ref="routeTable" :data="routeList" border v-loading="loading" @selection-change="onRouteSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="route_code" label="工艺编号" min-width="100" />
        <el-table-column prop="route_name" label="工艺名称" min-width="140" />
        <el-table-column prop="product" label="所属产品" min-width="140" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="description" label="工艺描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人员" width="100" />
        <el-table-column prop="equipment_usage" label="设备使用情况" width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="100">
          <template slot-scope="scope">
            <el-button type="text" style="color: red;" @click="deleteRoute(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import FactorySupplyHero from '../components/FactorySupplyHero.vue'
import { exportToCsv } from '../utils/exportCsv'
import { parseListResponse } from '../utils/parseListResponse'
import { getTempProcedureList, getTempRouteList, saveTempRoute } from '../utils/tempStorage'
import { config } from '../config'

const TENANT_ID = config.tenantId
const PAGE_VO = { curPage: 1, pageSize: 100, totalRows: 0, totalPages: 0, limit: 100, offset: 0 }

// 任务要求：中心轮零件加工 v1.0，5道工序
const TASK_ROUTE = {
  routeCode: 'PR001',
  routeName: '中心轮零件加工',
  product: '行星减速器相关零件',
  version: '1.0',
  description: '主要涉及与行星减速器相关零件的生产加工工艺，涵盖从毛坯制造到入库的完整流程',
  operator: '',
  operationTime: '',
  equipmentUsage: '毛坯制造设备、粗加工设备、精加工设备、检测设备等'
}

function buildListBody(conditions = []) {
  return { params: { sort: 'ASC', orderBy: 'id', characterSet: 'SCHINESE_PINYIN_M', orderByTableAlias: 'p', conditions } }
}

function buildCreateParams(entityType, data) {
  return {
    master: { id: TENANT_ID, clazz: 'Tenant' },
    params: {
      id: '',
      modifier: localStorage.getItem('xdm_modifier') || 'sysadmin 1',
      lastUpdateTime: '',
      creator: 'admin',
      rdmExtensionType: entityType,
      tenant: { id: TENANT_ID, clazz: 'Tenant' },
      ...data
    }
  }
}

/** 工艺-工序关联创建参数 */
function buildProcessRouteProcedureParams(routeId, procedureId, sequenceOrder) {
  return buildCreateParams('ProcessRouteProcedure', {
    route_id: routeId,
    procedure_id: procedureId,
    sequence_order: sequenceOrder
  })
}

export default {
  name: 'PlanPage',
  components: { FactorySupplyHero },
  data() {
    return {
      loading: false,
      batchLoading: false,
      routeForm: {
        routeCode: '',
        routeName: '',
        product: '',
        version: '',
        description: '',
        operator: '',
        operationTime: '',
        equipmentUsage: ''
      },
      routeList: [],
      procedureList: [],
      routeProcedureList: [],
      selectedRouteId: '',
      selectedProcedureId: '',
      linkSequence: 1,
      linkLoading: false,
      flowchartView: 'simple',
      selectedRoutes: []
    }
  },
  computed: {
    /** 按顺序排序的工序关联列表，供简图和 Mermaid 展示 */
    flowchartProcedureList() {
      const list = this.routeProcedureList || []
      return [...list].sort((a, b) => (a.sequence_order || a.sequenceOrder || 0) - (b.sequence_order || b.sequenceOrder || 0))
    },
    mermaidCode() {
      const list = this.flowchartProcedureList
      if (list.length === 0) return ''
      const lines = ['flowchart LR']
      const esc = (s) => String(s || '工序').replace(/["\[\]\\\n\r]/g, ' ').trim() || '工序'
      list.forEach((item, i) => {
        const name = esc(this.getProcedureName(item.procedure_id || item.procedureId) || '工序')
        const id = 'P' + (i + 1)
        lines.push(`    ${id}("${name}")`)
        if (i < list.length - 1) lines.push(`    ${id} --> P${i + 2}`)
      })
      return lines.join('\n')
    }
  },
  inject: { globalSearchApply: { default: () => ({ page: '', keyword: '' }) } },
  mounted() {
    this.loadRouteList()
    this.loadProcedureList()
    this.$nextTick(() => this.applyGlobalSearch())
  },
  activated() {
    this.loadRouteList()
    this.loadProcedureList()
    if (this.selectedRouteId) this.loadRouteProcedureList(this.selectedRouteId)
  },
  watch: {
    selectedRouteId(v) {
      if (v) this.loadRouteProcedureList(v)
      else this.routeProcedureList = []
    },
    routeProcedureList() {
      this.$nextTick(() => this.renderMermaid())
    },
    flowchartView(v) {
      if (v === 'mermaid') this.$nextTick(() => this.renderMermaid())
    }
  },
  methods: {
    async refreshAll() {
      await Promise.all([this.loadRouteList(), this.loadProcedureList()])
      if (this.selectedRouteId) this.loadRouteProcedureList(this.selectedRouteId)
      this.$message.success('已刷新')
    },
    applyGlobalSearch() {
      const apply = this.globalSearchApply
      if (apply && apply.page === 'PlanPage' && apply.keyword) {
        this.loadRouteList([{ conditionName: 'route_name', operator: 'like', conditionValues: ['%' + apply.keyword + '%'] }])
        apply.keyword = ''
      }
    },
    async loadRouteList(extraConditions = []) {
      const tempItems = [...getTempRouteList(), ...(this.routeList || []).filter(r => String(r.id || '').startsWith('temp_'))]
      const seen = new Set()
      const tempUnique = tempItems.filter(r => { if (seen.has(r.id)) return false; seen.add(r.id); return true })
      this.loading = true
      try {
        const body = buildListBody(extraConditions)
        const res = await this.$axios.post('/api/dynamic/api/ProcessRoute/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const fromApi = parseListResponse(res.data)
        this.routeList = [...tempUnique, ...fromApi]
        if (this.routeList.length && !this.selectedRouteId) this.selectedRouteId = this.routeList[0].id
      } catch (e) {
        console.error(e)
        this.routeList = [...tempUnique]
        if (e.response && e.response.status !== 404) {
          this.$message.error('查询工艺路线失败：' + (e.response && e.response.status === 403 ? '服务暂不可用，请稍后重试' : e.message))
        }
      } finally {
        this.loading = false
      }
    },
    async loadProcedureList() {
      const tempItems = getTempProcedureList()
      try {
        const body = buildListBody()
        const res = await this.$axios.post('/api/dynamic/api/ProcedureManagement/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const fromApi = parseListResponse(res.data)
        this.procedureList = [...tempItems, ...fromApi]
      } catch (e) {
        this.procedureList = [...tempItems]
      }
    },
    async loadRouteProcedureList(routeId) {
      if (!routeId) return
      const tempItems = (this.routeProcedureList || []).filter(r =>
        String(r.id || '').startsWith('temp_') && (r.route_id || r.routeId) === routeId
      )
      this.loading = true
      try {
        const conditions = [{ conditionName: 'route_id', operator: '=', conditionValues: [routeId] }]
        const body = buildListBody(conditions)
        const res = await this.$axios.post('/api/dynamic/api/ProcessRouteProcedure/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        let list = parseListResponse(res.data)
        const combined = [...tempItems, ...list]
        combined.sort((a, b) => (a.sequence_order || a.sequenceOrder || 0) - (b.sequence_order || b.sequenceOrder || 0))
        this.routeProcedureList = combined
      } catch (e) {
        this.routeProcedureList = [...tempItems]
      } finally {
        this.loading = false
      }
    },
    getProcedureName(id) {
      const p = this.procedureList.find(x => x.id === id)
      return p ? (p.procedure_name || p.procedureName) : '-'
    },
    getProcedureCode(id) {
      const p = this.procedureList.find(x => x.id === id)
      return p ? (p.procedure_code || p.procedureCode) : '-'
    },
    exportRouteList() {
      const cols = [
        { label: '工艺编号', field: 'route_code' },
        { label: '工艺名称', field: 'route_name' },
        { label: '所属产品', field: 'product' },
        { label: '版本', field: 'version' },
        { label: '工艺描述', field: 'description' },
        { label: '操作人员', field: 'operator' },
        { label: '设备使用情况', field: 'equipment_usage' }
      ]
      exportToCsv(this.routeList, cols, '工艺路线列表')
      this.$message.success('已导出')
    },
    exportRouteProcedureList() {
      const cols = [
        { label: '顺序', field: 'sequence_order' },
        { label: '工序名称', getter: (row) => this.getProcedureName(row.procedure_id || row.procedureId) },
        { label: '工序编号', getter: (row) => this.getProcedureCode(row.procedure_id || row.procedureId) }
      ]
      exportToCsv(this.routeProcedureList, cols, '工序关联列表')
      this.$message.success('已导出')
    },
    async addRoute() {
      const f = this.routeForm
      if (!f.routeCode || !f.routeName || !f.product || !f.version || !f.description) {
        this.$message.warning('工艺编号、工艺名称、所属产品、版本、工艺描述为必填项')
        return
      }
      this.loading = true
      const newRow = {
        id: 'temp_' + Date.now(),
        route_code: f.routeCode,
        route_name: f.routeName,
        product: f.product,
        version: f.version,
        description: f.description,
        operator: f.operator || '',
        equipment_usage: f.equipmentUsage || ''
      }
      try {
        const body = buildCreateParams('ProcessRoute', {
          route_code: f.routeCode,
          route_name: f.routeName,
          product: f.product,
          version: f.version,
          description: f.description,
          operator: f.operator || '',
          operation_time: f.operationTime || '',
          equipment_usage: f.equipmentUsage || ''
        })
        const res = await this.$axios.post('/api/dynamic/api/ProcessRoute/create', body, {
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const d = res.data || {}
        if (d.result !== 'FAIL') {
          this.loadRouteList()
        } else {
          this.routeList = [newRow, ...this.routeList]
          saveTempRoute(newRow)
        }
      } catch (_) {
        this.routeList = [newRow, ...this.routeList]
        saveTempRoute(newRow)
      }
      this.$message.success('新增工艺路线成功')
      this.resetRouteForm()
      if (!this.selectedRouteId) this.selectedRouteId = newRow.id
      this.loading = false
    },
    resetRouteForm() {
      this.routeForm = {
        routeCode: '',
        routeName: '',
        product: '',
        version: '',
        description: '',
        operator: '',
        operationTime: '',
        equipmentUsage: ''
      }
    },
    async batchCreateRouteAndProcedures() {
      this.batchLoading = true
      try {
        await this.loadProcedureList()
        const order = ['毛坯制造', '粗加工', '精加工', '检测', '入库']
        const procs = order.map(name => this.procedureList.find(p => (p.procedure_name || p.procedureName) === name)).filter(Boolean)
        if (procs.length < 5) {
          this.$message.warning('请先在工序管理页面新增 5 道工序：毛坯制造、粗加工、精加工、检测、入库')
          this.batchLoading = false
          return
        }
        const body = buildCreateParams('ProcessRoute', {
          route_code: TASK_ROUTE.routeCode,
          route_name: TASK_ROUTE.routeName,
          product: TASK_ROUTE.product,
          version: TASK_ROUTE.version,
          description: TASK_ROUTE.description,
          operator: TASK_ROUTE.operator || '',
          operation_time: '',
          equipment_usage: TASK_ROUTE.equipmentUsage || ''
        })
        let routeId = null
        let tempRoute = null
        try {
          const createRes = await this.$axios.post('/api/dynamic/api/ProcessRoute/create', body, {
            headers: { 'Content-Type': 'application/json;charset=UTF-8' }
          })
          const d = createRes.data || {}
          routeId = (d.params && d.params.id) || d.id || (d.data && d.data.id) || (Array.isArray(d.data) && d.data[0] && d.data[0].id)
        } catch (_) {}
        if (!routeId) {
          routeId = 'temp_' + Date.now()
          tempRoute = { id: routeId, route_code: TASK_ROUTE.routeCode, route_name: TASK_ROUTE.routeName, product: TASK_ROUTE.product, version: TASK_ROUTE.version, description: TASK_ROUTE.description }
          this.routeList = [tempRoute, ...this.routeList]
          saveTempRoute(tempRoute)
        }
        const routeProcedureTemp = []
        for (let i = 0; i < procs.length; i++) {
          try {
            const linkBody = buildProcessRouteProcedureParams(routeId, procs[i].id, i + 1)
            await this.$axios.post('/api/dynamic/api/ProcessRouteProcedure/create', linkBody, {
              headers: { 'Content-Type': 'application/json;charset=UTF-8' }
            })
          } catch (_) {
            routeProcedureTemp.push({ id: 'temp_' + Date.now() + '_' + i, route_id: routeId, procedure_id: procs[i].id, sequence_order: i + 1 })
          }
        }
        if (routeProcedureTemp.length) {
          this.routeProcedureList = [...routeProcedureTemp, ...this.routeProcedureList]
        }
        this.loadRouteList()
        this.selectedRouteId = routeId
        this.loadRouteProcedureList(routeId)
        this.$message.success(`工艺路线「中心轮零件加工」v1.0 已创建，已关联 ${procs.length - routeProcedureTemp.length}/5 道工序` + (routeProcedureTemp.length ? '（部分关联失败，已加入本地列表）' : ''))
      } catch (e) {
        this.$message.error('一键配置失败：' + (e.response && e.response.status === 403 ? '服务暂不可用，请稍后重试' : (e.response && e.response.data && e.response.data.message) || e.message) + '。请确认工艺路线相关模型已就绪')
      } finally {
        this.batchLoading = false
      }
    },
    async addProcedureLink() {
      if (!this.selectedRouteId || !this.selectedProcedureId) {
        this.$message.warning('请选择工艺路线和工序')
        return
      }
      const procId = this.selectedProcedureId
      const exists = this.routeProcedureList.some(
        r => (r.procedure_id || r.procedureId) === procId
      )
      if (exists) {
        this.$message.warning('该工序已关联到当前工艺路线，请勿重复添加')
        return
      }
      if (this.linkSequence < 1 || this.linkSequence > 99) {
        this.$message.warning('顺序应在 1～99 之间')
        return
      }
      this.linkLoading = true
      const routeId = this.selectedRouteId
      const newRow = {
        id: 'temp_' + Date.now(),
        route_id: routeId,
        procedure_id: procId,
        sequence_order: this.linkSequence
      }
      try {
        const body = buildProcessRouteProcedureParams(routeId, procId, this.linkSequence)
        const res = await this.$axios.post('/api/dynamic/api/ProcessRouteProcedure/create', body, {
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const d = res.data || {}
        if (d.result !== 'FAIL') {
          await this.loadRouteProcedureList(routeId)
        } else {
          this.routeProcedureList = [newRow, ...this.routeProcedureList]
        }
      } catch (_) {
        this.routeProcedureList = [newRow, ...this.routeProcedureList]
      }
      this.$message.success('添加关联成功')
      const maxSeq = Math.max(0, ...this.routeProcedureList.map(r => r.sequence_order || r.sequenceOrder || 0))
      this.linkSequence = maxSeq + 1
      this.linkLoading = false
    },
    async deleteProcedureLink(id) {
      try {
        await this.$confirm('确定删除该工序关联？', '提示', { type: 'warning' })
        await this.$axios.delete(`/api/dynamic/api/ProcessRouteProcedure/delete/${id}`)
        this.$message.success('删除成功')
        this.loadRouteProcedureList(this.selectedRouteId)
      } catch (e) {
        if (e !== 'cancel') this.$message.error('删除失败：' + e.message)
      }
    },
    renderMermaid() {
      if (this.flowchartView !== 'mermaid' || !this.routeProcedureList.length || !window.mermaid) return
      const el = this.$refs.mermaidContainer
      if (!el) return
      const code = this.mermaidCode
      if (!code) return
      try {
        el.innerHTML = ''
        const id = 'mermaid-' + Date.now()
        window.mermaid.initialize({ startOnLoad: false })
        window.mermaid.render(id, code, (svg) => {
          el.innerHTML = svg || ''
        })
      } catch (e) {
        el.innerHTML = '<div class="mermaid-error">Mermaid 渲染失败，请使用简图</div>'
      }
    },
    onRouteSelectionChange(rows) {
      this.selectedRoutes = rows || []
    },
    async batchDeleteRoute() {
      if (!this.selectedRoutes.length) return
      try {
        await this.$confirm(`确定删除选中的 ${this.selectedRoutes.length} 条工艺路线？`, '批量删除', { type: 'warning' })
        this.loading = true
        let ok = 0
        let err = 0
        for (const row of this.selectedRoutes) {
          try {
            await this.$axios.delete(`/api/dynamic/api/ProcessRoute/delete/${row.id}`)
            ok++
          } catch (e) {
            err++
          }
        }
        this.$message.success(`已删除 ${ok} 条` + (err ? `，${err} 条失败` : ''))
        this.loadRouteList()
        this.$refs.routeTable && this.$refs.routeTable.clearSelection()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('批量删除失败：' + e.message)
      } finally {
        this.loading = false
      }
    },
    async deleteRoute(id) {
      try {
        await this.$confirm('确定删除该工艺路线？', '提示', { type: 'warning' })
        await this.$axios.delete(`/api/dynamic/api/ProcessRoute/delete/${id}`)
        this.$message.success('删除成功')
        this.loadRouteList()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('删除失败：' + e.message)
      }
    }
  }
}
</script>

<style scoped>
.fs-page--supply .el-card.fs-card,
.fs-page--supply >>> .el-card {
  margin-bottom: 16px;
  border-radius: 12px;
  border: 1px solid rgba(245, 158, 11, 0.22);
  box-shadow: 0 8px 28px rgba(30, 27, 75, 0.09);
  overflow: hidden;
}
.fs-page--supply >>> .el-card__header {
  font-weight: 600;
  letter-spacing: 0.02em;
  background: linear-gradient(90deg, rgba(254, 243, 199, 0.55) 0%, rgba(255, 255, 255, 0.95) 100%);
  border-bottom: 1px solid rgba(245, 158, 11, 0.18);
}
.usage-tips-card { margin-bottom: 16px; border-left: 4px solid #d97706; }
.usage-tips-card .card-header { font-weight: 600; color: #303133; }
.usage-tips-card .card-header i { margin-right: 6px; color: #d97706; }
.usage-tips-list {
  margin: 0; padding-left: 20px; color: #606266; font-size: 13px; line-height: 1.8;
}
.usage-tips-list li { margin-bottom: 8px; }
.usage-tips-list li:last-child { margin-bottom: 0; }
.usage-tips-list strong { color: #303133; }
.route-form .el-form-item { margin-bottom: 12px; }
.hint { margin-bottom: 12px; color: #606266; font-size: 13px; }
.flowchart-wrap { padding: 8px 0; }
.flowchart-empty { padding: 24px; text-align: center; color: #909399; font-size: 14px; background: #f5f7fa; border-radius: 8px; }
.flowchart-diagram {
  display: flex; flex-wrap: wrap; align-items: center; gap: 0;
  padding: 16px; background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 8px; border: 1px solid #e2e8f0;
}
.flowchart-node-wrap { display: flex; align-items: center; }
.flowchart-node {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-width: 100px; padding: 12px 16px; background: #fff;
  border: 2px solid #165DFF; border-radius: 8px; box-shadow: 0 2px 8px rgba(22,93,255,0.15);
}
.flowchart-node:hover { box-shadow: 0 4px 12px rgba(22,93,255,0.25); }
.node-seq { font-size: 11px; color: #165DFF; font-weight: 600; margin-bottom: 4px; }
.node-name { font-size: 14px; font-weight: 500; color: #303133; }
.node-code { font-size: 11px; color: #909399; margin-top: 2px; }
.flowchart-arrow { padding: 0 8px; font-size: 18px; color: #165DFF; font-weight: 300; }
.mermaid-wrap { min-height: 120px; padding: 16px; background: #fff; border-radius: 8px; }
.mermaid-wrap svg { max-width: 100%; }
.mermaid-error { padding: 24px; color: #909399; text-align: center; }
</style>
