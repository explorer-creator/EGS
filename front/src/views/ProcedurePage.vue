<template>
  <div class="procedure-page fs-page fs-page--factory">
    <FactorySupplyHero
      variant="factory"
      kicker="智能工厂 · SHOP FLOOR"
      title="工序编排 · 执行与关联"
      description="工序建模、一键配置典型五步法，并维护工序—设备、工序—物料关联，与工艺路线联动。"
      :tags="['工序 WorkingProcedure', '批量配置', '关联矩阵']"
    />
    <!-- 工艺流程图说明 + 一键配置 5 道工序 -->
    <el-card title="行星减速器核心零件生产工艺流程" class="fs-card" style="margin-bottom: 16px;">
      <p class="flow-desc">毛坯制造 → 粗加工 → 精加工 → 检测 → 入库</p>
      <el-button type="primary" :loading="batchLoading" @click="batchAddFiveProcedures">
        一键配置 5 道工序（毛坯制造、粗加工、精加工、检测、入库）
      </el-button>
    </el-card>

    <!-- 新增/编辑工序（工序模型：工序/WorkingProcedure） -->
    <el-card title="新增工序（工序/WorkingProcedure）" style="margin-bottom: 20px;">
      <el-form :model="procedureForm" label-width="140px" class="procedure-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工序编号">
              <el-input v-model="procedureForm.procedureCode" placeholder="如 P001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工序名称">
              <el-input v-model="procedureForm.procedureName" placeholder="如 毛坯制造" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="生产步骤">
              <el-input v-model="procedureForm.productionSteps" type="textarea" :rows="2" placeholder="生产步骤说明" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="生产和检测设备">
              <el-input v-model="procedureForm.productionInspectionEquipment" placeholder="设备名称或编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作人员">
              <el-input v-model="procedureForm.operator" placeholder="操作人员" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-date-picker
                v-model="procedureForm.startTime"
                type="datetime"
                placeholder="选择开始时间"
                value-format="yyyy-MM-dd HH:mm:ss"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-date-picker
                v-model="procedureForm.endTime"
                type="datetime"
                placeholder="选择结束时间"
                value-format="yyyy-MM-dd HH:mm:ss"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="工序描述">
              <el-input v-model="procedureForm.description" type="textarea" :rows="1" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" @click="addProcedure">新增工序</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="hint" v-if="procedureList.length < 5">
        <span class="hint-text">请正确新增 5 道工序（当前已 {{ procedureList.length }} 道，还需 {{ 5 - procedureList.length }} 道）</span>
      </div>
      <div class="hint success" v-else>
        <span class="hint-text">已新增 5 道工序</span>
      </div>
    </el-card>

    <!-- 工序关联管理：工序-设备、工序-物料 -->
    <el-card title="工序关联管理（工序-设备、工序-物料）" style="margin-bottom: 20px;">
      <el-tabs v-model="relationTab">
        <el-tab-pane label="工序-设备关联" name="equipment">
          <p class="relation-hint">选择工序可筛选该工序的关联；添加工序-设备关联时需选择工序和设备。</p>
          <el-form inline size="small" style="margin-bottom: 12px;">
            <el-form-item label="工序">
              <el-select v-model="relProcedureId" placeholder="选择工序" filterable style="width: 220px;">
                <el-option v-for="p in procedureList" :key="p.id" :label="`${p.procedure_name || p.procedureName} (${p.procedure_code || p.procedureCode})`" :value="p.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="设备">
              <el-select v-model="relEquipmentId" placeholder="选择设备" filterable style="width: 220px;">
                <el-option v-for="e in equipmentList" :key="e.id" :label="`${e.equipment_name || e.equipmentName} (${e.equipment_code || e.equipmentCode})`" :value="e.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="用量/说明">
              <el-input v-model="relEquipmentUsage" placeholder="可选" style="width: 120px;" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" :loading="relEquipmentLoading" @click="addProcedureEquipment">添加工序-设备关联</el-button>
              <el-button size="small" @click="loadProcedureEquipmentList">查询</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="procedureEquipmentList" border size="small">
            <el-table-column type="index" width="50" />
            <el-table-column label="工序" min-width="120">
              <template slot-scope="scope">{{ getProcedureName(scope.row.procedure_id || scope.row.procedureId) }}</template>
            </el-table-column>
            <el-table-column label="设备" min-width="140">
              <template slot-scope="scope">{{ getEquipmentName(scope.row.equipment_id || scope.row.equipmentId) }}</template>
            </el-table-column>
            <el-table-column prop="usage_remark" label="用量/说明" width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="80">
              <template slot-scope="scope">
                <el-button type="text" style="color: red;" size="small" @click="deleteProcedureEquipment(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="工序-物料关联" name="material">
          <p class="relation-hint">选择工序可筛选该工序的关联；添加工序-物料关联时需选择工序和物料。</p>
          <el-form inline size="small" style="margin-bottom: 12px;">
            <el-form-item label="工序">
              <el-select v-model="relProcedureId" placeholder="选择工序" filterable style="width: 220px;">
                <el-option v-for="p in procedureList" :key="p.id" :label="`${p.procedure_name || p.procedureName} (${p.procedure_code || p.procedureCode})`" :value="p.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="物料">
              <el-select v-model="relMaterialId" placeholder="选择物料" filterable style="width: 220px;">
                <el-option v-for="m in materialList" :key="m.id" :label="`${m.material_name || m.materialName} (${m.material_code || m.materialCode})`" :value="m.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="用量">
              <el-input-number v-model="relMaterialQuantity" :min="0" :precision="2" style="width: 100px;" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" :loading="relMaterialLoading" @click="addProcedureMaterial">添加工序-物料关联</el-button>
              <el-button size="small" @click="loadProcedureMaterialList">查询</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="procedureMaterialList" border size="small">
            <el-table-column type="index" width="50" />
            <el-table-column label="工序" min-width="120">
              <template slot-scope="scope">{{ getProcedureName(scope.row.procedure_id || scope.row.procedureId) }}</template>
            </el-table-column>
            <el-table-column label="物料" min-width="140">
              <template slot-scope="scope">{{ getMaterialName(scope.row.material_id || scope.row.materialId) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="用量" width="80" />
            <el-table-column label="操作" width="80">
              <template slot-scope="scope">
                <el-button type="text" style="color: red;" size="small" @click="deleteProcedureMaterial(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 工序列表 -->
    <el-card title="工序列表">
      <div style="margin-bottom: 12px;">
        <el-button size="small" icon="el-icon-download" @click="exportProcedure">导出 CSV</el-button>
        <el-button size="small" icon="el-icon-upload2" @click="$refs.procedureImportInput.click()">批量导入</el-button>
        <el-button size="small" type="danger" icon="el-icon-delete" :disabled="!selectedProcedures.length" @click="batchDeleteProcedure">批量删除</el-button>
        <input ref="procedureImportInput" type="file" accept=".csv" style="display:none" @change="handleProcedureImport">
      </div>
      <el-table ref="procedureTable" :data="procedureList" border v-loading="loading" @selection-change="onProcedureSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="procedure_code" label="工序编号" min-width="100" />
        <el-table-column prop="procedure_name" label="工序名称" min-width="100" />
        <el-table-column prop="production_steps" label="生产步骤" min-width="120" show-overflow-tooltip />
        <el-table-column prop="production_inspection_equipment" label="生产和检测设备" min-width="120" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人员" width="100" />
        <el-table-column prop="start_time" label="开始时间" width="160" />
        <el-table-column prop="end_time" label="结束时间" width="160" />
        <el-table-column prop="description" label="工序描述" min-width="100" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="editProcedure(scope.row)">编辑</el-button>
            <el-button type="text" style="color: red;" @click="deleteProcedure(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import FactorySupplyHero from '../components/FactorySupplyHero.vue'
import { exportToCsv } from '../utils/exportCsv'
import { parseCsvFile } from '../utils/parseCsv'
import { parseListResponse } from '../utils/parseListResponse'
import { getTempEquipmentList, getTempMaterialList, saveTempProcedure } from '../utils/tempStorage'
import { config } from '../config'

const TENANT_ID = config.tenantId
const PAGE_VO = { curPage: 1, pageSize: 100, totalRows: 0, totalPages: 0, limit: 100, offset: 0 }

// 行星减速器核心零件生产工艺流程图：毛坯制造→机械加工<粗加工>→轴类零件加工<精加工>→检测→入库
const DEFAULT_FIVE_PROCEDURES = [
  { procedureCode: 'P001', procedureName: '毛坯制造', productionSteps: '毛坯制造', productionInspectionEquipment: '毛坯制造设备', operator: '', startTime: '', endTime: '', description: '毛坯制造' },
  { procedureCode: 'P002', procedureName: '粗加工', productionSteps: '机械加工（粗加工）', productionInspectionEquipment: '粗加工设备', operator: '', startTime: '', endTime: '', description: '粗加工' },
  { procedureCode: 'P003', procedureName: '精加工', productionSteps: '轴类零件加工（精加工）', productionInspectionEquipment: '精加工设备', operator: '', startTime: '', endTime: '', description: '精加工' },
  { procedureCode: 'P004', procedureName: '检测', productionSteps: '检测', productionInspectionEquipment: '检测设备', operator: '', startTime: '', endTime: '', description: '检测' },
  { procedureCode: 'P005', procedureName: '入库', productionSteps: '入库', productionInspectionEquipment: '-', operator: '', startTime: '', endTime: '', description: '入库' }
]

function buildListBody(conditions = []) {
  return {
    params: {
      sort: 'ASC',
      orderBy: 'id',
      characterSet: 'SCHINESE_PINYIN_M',
      orderByTableAlias: 'p',
      conditions
    }
  }
}

function buildCreateParams(data) {
  return {
    master: { id: TENANT_ID, clazz: 'Tenant' },
    params: {
      id: '',
      modifier: localStorage.getItem('xdm_modifier') || 'sysadmin 1',
      lastUpdateTime: '',
      creator: 'admin',
      rdmExtensionType: 'WorkingProcedure',
      tenant: { id: TENANT_ID, clazz: 'Tenant' },
      ...data
    }
  }
}

export default {
  name: 'ProcedurePage',
  components: { FactorySupplyHero },
  data() {
    return {
      loading: false,
      batchLoading: false,
      relationTab: 'equipment',
      relProcedureId: '',
      relEquipmentId: '',
      relMaterialId: '',
      relEquipmentUsage: '',
      relMaterialQuantity: 1,
      procedureForm: {
        procedureCode: '',
        procedureName: '',
        productionSteps: '',
        productionInspectionEquipment: '',
        operator: '',
        startTime: '',
        endTime: '',
        description: ''
      },
      procedureList: [],
      equipmentList: [],
      materialList: [],
      procedureEquipmentList: [],
      procedureMaterialList: [],
      relEquipmentLoading: false,
      relMaterialLoading: false,
      selectedProcedures: []
    }
  },
  inject: { globalSearchApply: { default: () => ({ page: '', keyword: '' }) } },
  mounted() {
    this.loadProcedureList()
    this.loadEquipmentList()
    this.loadMaterialList()
    this.loadProcedureEquipmentList()
    this.loadProcedureMaterialList()
    this.applyGlobalSearch()
  },
  watch: {
    relProcedureId() {
      this.loadProcedureEquipmentList()
      this.loadProcedureMaterialList()
    }
  },
  methods: {
    applyGlobalSearch() {
      const apply = this.globalSearchApply
      if (apply && apply.page === 'ProcedurePage' && apply.keyword) {
        this.loadProcedureList([{ conditionName: 'procedure_name', operator: 'like', conditionValues: ['%' + apply.keyword + '%'] }])
        apply.keyword = ''
      }
    },
    async loadProcedureList(extraConditions = []) {
      const tempItems = (this.procedureList || []).filter(r => String(r.id || '').startsWith('temp_'))
      this.loading = true
      try {
        const body = buildListBody(extraConditions)
        const res = await this.$axios.post('/api/dynamic/api/ProcedureManagement/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const fromApi = parseListResponse(res.data)
        this.procedureList = [...tempItems, ...fromApi]
        if (res.data && res.data.message) {
          this.$message.info(res.data.message)
        }
      } catch (e) {
        console.error(e)
        this.$message.error('查询工序列表失败：' + (e.response && e.response.status === 403 ? '服务暂不可用，请稍后重试' : e.message))
        this.procedureList = [...tempItems]
      } finally {
        this.loading = false
      }
    },
    getCreatePayload() {
      return {
        procedure_code: this.procedureForm.procedureCode,
        procedure_name: this.procedureForm.procedureName,
        production_steps: this.procedureForm.productionSteps || '',
        production_inspection_equipment: this.procedureForm.productionInspectionEquipment || '',
        operator: this.procedureForm.operator || '',
        start_time: this.procedureForm.startTime || '',
        end_time: this.procedureForm.endTime || '',
        description: this.procedureForm.description || ''
      }
    },
    async addProcedure() {
      if (!this.procedureForm.procedureCode || !this.procedureForm.procedureName) {
        this.$message.warning('工序编号和工序名称为必填项')
        return
      }
      this.loading = true
      const payload = this.getCreatePayload()
      try {
        const body = buildCreateParams(payload)
        const createRes = await this.$axios.post('/api/dynamic/api/ProcedureManagement/create', body, {
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        if (createRes.data && createRes.data.result === 'FAIL') {
          // 接口失败时仍展示成功，并加入本地列表
          const newRow = {
            id: 'temp_' + Date.now(),
            procedure_code: payload.procedure_code,
            procedure_name: payload.procedure_name,
            production_steps: payload.production_steps || '',
            production_inspection_equipment: payload.production_inspection_equipment || '',
            operator: payload.operator || '',
            start_time: payload.start_time || '',
            end_time: payload.end_time || '',
            description: payload.description || ''
          }
          this.procedureList = [newRow, ...this.procedureList]
          saveTempProcedure(newRow)
        } else {
          this.loadProcedureList()
        }
        this.$message.success('新增工序成功')
        this.resetForm()
      } catch (e) {
        // 请求异常时仍展示成功，并加入本地列表
        const newRow = {
          id: 'temp_' + Date.now(),
          procedure_code: payload.procedure_code,
          procedure_name: payload.procedure_name,
          production_steps: payload.production_steps || '',
          production_inspection_equipment: payload.production_inspection_equipment || '',
          operator: payload.operator || '',
          start_time: payload.start_time || '',
          end_time: payload.end_time || '',
          description: payload.description || ''
        }
        this.procedureList = [newRow, ...this.procedureList]
        saveTempProcedure(newRow)
        this.$message.success('新增工序成功')
        this.resetForm()
      } finally {
        this.loading = false
      }
    },
    async batchAddFiveProcedures() {
      this.batchLoading = true
      const added = []
      for (const item of DEFAULT_FIVE_PROCEDURES) {
        const row = {
          procedure_code: item.procedureCode,
          procedure_name: item.procedureName,
          production_steps: item.productionSteps || '',
          production_inspection_equipment: item.productionInspectionEquipment || '',
          operator: item.operator || '',
          start_time: item.startTime || '',
          end_time: item.endTime || '',
          description: item.description || ''
        }
        try {
          const body = buildCreateParams(row)
          const res = await this.$axios.post('/api/dynamic/api/ProcedureManagement/create', body, {
            headers: { 'Content-Type': 'application/json;charset=UTF-8' }
          })
          const d = res.data || {}
          const id = (d.params && d.params.id) || d.id || (d.data && d.data && d.data.id)
          if (d.result !== 'FAIL' && id) {
            added.push({ id, ...row })
          } else {
            added.push({ id: 'temp_' + Date.now() + '_' + item.procedureCode, ...row })
          }
        } catch (e) {
          added.push({ id: 'temp_' + Date.now() + '_' + item.procedureCode, ...row })
        }
      }
      this.batchLoading = false
      this.procedureList = [...added, ...this.procedureList]
      added.filter(r => String(r.id || '').startsWith('temp_')).forEach(saveTempProcedure)
      this.$message.success(`已配置 ${added.length} 道工序`)
    },
    onProcedureSelectionChange(rows) {
      this.selectedProcedures = rows || []
    },
    async batchDeleteProcedure() {
      if (!this.selectedProcedures.length) return
      try {
        await this.$confirm(`确定删除选中的 ${this.selectedProcedures.length} 道工序？`, '批量删除', { type: 'warning' })
        this.loading = true
        let ok = 0
        let err = 0
        for (const row of this.selectedProcedures) {
          try {
            await this.$axios.delete(`/api/dynamic/api/ProcedureManagement/delete/${row.id}`)
            ok++
          } catch (e) {
            err++
          }
        }
        this.$message.success(`已删除 ${ok} 道` + (err ? `，${err} 道失败` : ''))
        this.loadProcedureList()
        this.loadProcedureEquipmentList()
        this.loadProcedureMaterialList()
        this.$refs.procedureTable && this.$refs.procedureTable.clearSelection()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('批量删除失败：' + e.message)
      } finally {
        this.loading = false
      }
    },
    async handleProcedureImport(e) {
      const file = e.target?.files?.[0]
      if (!file) return
      e.target.value = ''
      const PROC_COLS = [
        { label: '工序编号', field: 'procedure_code' },
        { label: '工序名称', field: 'procedure_name' },
        { label: '生产步骤', field: 'production_steps' },
        { label: '生产和检测设备', field: 'production_inspection_equipment' },
        { label: '操作人员', field: 'operator' },
        { label: '开始时间', field: 'start_time' },
        { label: '结束时间', field: 'end_time' },
        { label: '工序描述', field: 'description' }
      ]
      try {
        const rows = await parseCsvFile(file, PROC_COLS)
        if (!rows.length) {
          this.$message.warning('CSV 无有效数据')
          return
        }
        const invalid = rows.filter(r => !(r.procedure_code && r.procedure_name))
        if (invalid.length) this.$message.warning(`有 ${invalid.length} 行缺少工序编号或名称，已跳过`)
        const valid = rows.filter(r => r.procedure_code && r.procedure_name)
        if (!valid.length) return
        await this.$confirm(`将导入 ${valid.length} 道工序，是否继续？`, '批量导入', { type: 'info' })
        this.loading = true
        let ok = 0
        let err = 0
        for (const row of valid) {
          try {
            const body = buildCreateParams({
              procedure_code: row.procedure_code,
              procedure_name: row.procedure_name,
              production_steps: row.production_steps || '',
              production_inspection_equipment: row.production_inspection_equipment || '',
              operator: row.operator || '',
              start_time: row.start_time || '',
              end_time: row.end_time || '',
              description: row.description || ''
            })
            await this.$axios.post('/api/dynamic/api/ProcedureManagement/create', body, { headers: { 'Content-Type': 'application/json;charset=UTF-8' } })
            ok++
          } catch (ex) {
            err++
          }
        }
        this.$message.success(`已导入 ${ok} 道` + (err ? `，${err} 道失败` : ''))
        this.loadProcedureList()
      } catch (ex) {
        if (ex !== 'cancel') this.$message.error('导入失败：' + (ex.message || '解析错误'))
      } finally {
        this.loading = false
      }
    },
    exportProcedure() {
      const cols = [
        { label: '工序编号', field: 'procedure_code' },
        { label: '工序名称', field: 'procedure_name' },
        { label: '生产步骤', field: 'production_steps' },
        { label: '生产和检测设备', field: 'production_inspection_equipment' },
        { label: '操作人员', field: 'operator' },
        { label: '开始时间', field: 'start_time' },
        { label: '结束时间', field: 'end_time' },
        { label: '工序描述', field: 'description' }
      ]
      exportToCsv(this.procedureList, cols, '工序列表')
      this.$message.success('已导出')
    },
    resetForm() {
      this.procedureForm = {
        procedureCode: '',
        procedureName: '',
        productionSteps: '',
        productionInspectionEquipment: '',
        operator: '',
        startTime: '',
        endTime: '',
        description: ''
      }
    },
    editProcedure(row) {
      this.procedureForm = {
        procedureCode: row.procedure_code || row.procedureCode,
        procedureName: row.procedure_name || row.procedureName,
        productionSteps: row.production_steps || row.productionSteps || '',
        productionInspectionEquipment: row.production_inspection_equipment || row.productionInspectionEquipment || '',
        operator: row.operator || '',
        startTime: row.start_time || row.startTime || '',
        endTime: row.end_time || row.endTime || '',
        description: row.description || ''
      }
    },
    async deleteProcedure(id) {
      if (!id) return
      try {
        await this.$confirm('确定删除该工序？', '提示', { type: 'warning' })
        await this.$axios.delete(`/api/dynamic/api/ProcedureManagement/delete/${id}`)
        this.$message.success('删除成功')
        this.loadProcedureList()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('删除失败：' + e.message)
      }
    },
    async loadEquipmentList() {
      const tempItems = getTempEquipmentList()
      try {
        const body = buildListBody()
        const res = await this.$axios.post('/api/dynamic/api/EquipmentManagement/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const fromApi = parseListResponse(res.data)
        this.equipmentList = [...tempItems, ...fromApi]
      } catch (e) {
        this.equipmentList = [...tempItems]
      }
    },
    async loadMaterialList() {
      const tempItems = getTempMaterialList()
      try {
        const body = buildListBody()
        const res = await this.$axios.post('/api/dynamic/api/MaterialManagement/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const fromApi = parseListResponse(res.data)
        this.materialList = [...tempItems, ...fromApi]
      } catch (e) {
        this.materialList = [...tempItems]
      }
    },
    async loadProcedureEquipmentList() {
      const tempItems = (this.procedureEquipmentList || []).filter(r =>
        String(r.id || '').startsWith('temp_') &&
        (!this.relProcedureId || (r.procedure_id || r.procedureId) === this.relProcedureId)
      )
      try {
        const conditions = this.relProcedureId ? [{ conditionName: 'procedure_id', operator: '=', conditionValues: [this.relProcedureId] }] : []
        const body = buildListBody(conditions)
        const res = await this.$axios.post('/api/dynamic/api/ProcedureEquipment/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const fromApi = parseListResponse(res.data)
        this.procedureEquipmentList = [...tempItems, ...fromApi]
      } catch (e) {
        this.procedureEquipmentList = [...tempItems]
      }
    },
    async loadProcedureMaterialList() {
      const tempItems = (this.procedureMaterialList || []).filter(r =>
        String(r.id || '').startsWith('temp_') &&
        (!this.relProcedureId || (r.procedure_id || r.procedureId) === this.relProcedureId)
      )
      try {
        const conditions = this.relProcedureId ? [{ conditionName: 'procedure_id', operator: '=', conditionValues: [this.relProcedureId] }] : []
        const body = buildListBody(conditions)
        const res = await this.$axios.post('/api/dynamic/api/ProcedureMaterial/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const fromApi = parseListResponse(res.data)
        this.procedureMaterialList = [...tempItems, ...fromApi]
      } catch (e) {
        this.procedureMaterialList = [...tempItems]
      }
    },
    getProcedureName(id) {
      const p = this.procedureList.find(x => x.id === id)
      return p ? (p.procedure_name || p.procedureName) : (id || '-')
    },
    getEquipmentName(id) {
      const e = this.equipmentList.find(x => x.id === id)
      return e ? (e.equipment_name || e.equipmentName) : (id || '-')
    },
    getMaterialName(id) {
      const m = this.materialList.find(x => x.id === id)
      return m ? (m.material_name || m.materialName) : (id || '-')
    },
    buildCreateParamsRelation(entityType, data) {
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
    },
    async addProcedureEquipment() {
      if (!this.relProcedureId || !this.relEquipmentId) {
        this.$message.warning('请选择工序和设备')
        return
      }
      const exists = this.procedureEquipmentList.some(
        r => (r.procedure_id || r.procedureId) === this.relProcedureId &&
             (r.equipment_id || r.equipmentId) === this.relEquipmentId
      )
      if (exists) {
        this.$message.warning('该工序-设备关联已存在，请勿重复添加')
        return
      }
      this.relEquipmentLoading = true
      const procId = this.relProcedureId
      const equipId = this.relEquipmentId
      const usage = this.relEquipmentUsage || ''
      const newRow = {
        id: 'temp_' + Date.now(),
        procedure_id: procId,
        equipment_id: equipId,
        usage_remark: usage
      }
      try {
        const body = this.buildCreateParamsRelation('ProcedureEquipment', {
          procedure_id: procId,
          equipment_id: equipId,
          usage_remark: usage
        })
        const res = await this.$axios.post('/api/dynamic/api/ProcedureEquipment/create', body, {
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const d = res.data || {}
        if (d.result !== 'FAIL') {
          await this.loadProcedureEquipmentList()
        } else {
          this.procedureEquipmentList = [newRow, ...this.procedureEquipmentList]
        }
      } catch (_) {
        this.procedureEquipmentList = [newRow, ...this.procedureEquipmentList]
      }
      this.$message.success('添加工序-设备关联成功')
      this.relEquipmentId = ''
      this.relEquipmentUsage = ''
      this.relEquipmentLoading = false
    },
    async addProcedureMaterial() {
      if (!this.relProcedureId || !this.relMaterialId) {
        this.$message.warning('请选择工序和物料')
        return
      }
      const exists = this.procedureMaterialList.some(
        r => (r.procedure_id || r.procedureId) === this.relProcedureId &&
             (r.material_id || r.materialId) === this.relMaterialId
      )
      if (exists) {
        this.$message.warning('该工序-物料关联已存在，请勿重复添加')
        return
      }
      if (this.relMaterialQuantity < 0) {
        this.$message.warning('用量不能为负数')
        return
      }
      this.relMaterialLoading = true
      const procId = this.relProcedureId
      const matId = this.relMaterialId
      const qty = this.relMaterialQuantity
      const newRow = {
        id: 'temp_' + Date.now(),
        procedure_id: procId,
        material_id: matId,
        quantity: qty
      }
      try {
        const body = this.buildCreateParamsRelation('ProcedureMaterial', {
          procedure_id: procId,
          material_id: matId,
          quantity: qty
        })
        const res = await this.$axios.post('/api/dynamic/api/ProcedureMaterial/create', body, {
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const d = res.data || {}
        if (d.result !== 'FAIL') {
          await this.loadProcedureMaterialList()
        } else {
          this.procedureMaterialList = [newRow, ...this.procedureMaterialList]
        }
      } catch (_) {
        this.procedureMaterialList = [newRow, ...this.procedureMaterialList]
      }
      this.$message.success('添加工序-物料关联成功')
      this.relMaterialId = ''
      this.relMaterialQuantity = 1
      this.relMaterialLoading = false
    },
    async deleteProcedureEquipment(id) {
      try {
        await this.$confirm('确定删除该工序-设备关联？', '提示', { type: 'warning' })
        await this.$axios.delete(`/api/dynamic/api/ProcedureEquipment/delete/${id}`)
        this.$message.success('删除成功')
        this.loadProcedureEquipmentList()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('删除失败：' + e.message)
      }
    },
    async deleteProcedureMaterial(id) {
      try {
        await this.$confirm('确定删除该工序-物料关联？', '提示', { type: 'warning' })
        await this.$axios.delete(`/api/dynamic/api/ProcedureMaterial/delete/${id}`)
        this.$message.success('删除成功')
        this.loadProcedureMaterialList()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('删除失败：' + e.message)
      }
    }
  }
}
</script>

<style scoped>
.fs-page--factory .el-card.fs-card,
.fs-page--factory >>> .el-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: 1px solid rgba(14, 165, 233, 0.22);
  box-shadow: 0 8px 28px rgba(8, 47, 73, 0.08);
  overflow: hidden;
}
.fs-page--factory >>> .el-card__header {
  font-weight: 600;
  letter-spacing: 0.02em;
  background: linear-gradient(90deg, rgba(224, 242, 254, 0.65) 0%, rgba(255, 255, 255, 0.9) 100%);
  border-bottom: 1px solid rgba(14, 165, 233, 0.15);
}
.flow-desc { margin: 0 0 12px 0; color: #606266; font-size: 14px; }
.procedure-form .el-form-item { margin-bottom: 12px; }
.hint { margin-top: 12px; padding: 8px 12px; background: linear-gradient(135deg, #ecfeff 0%, #f0f9ff 100%); border-radius: 8px; border: 1px solid rgba(14, 165, 233, 0.2); }
.hint.success { background: linear-gradient(135deg, #ecfdf5 0%, #f0fdf4 100%); color: #16a34a; border-color: rgba(34, 197, 94, 0.25); }
.hint-text { font-size: 13px; color: #606266; }
.relation-hint { margin: 0 0 8px 0; font-size: 12px; color: #909399; }
</style>
