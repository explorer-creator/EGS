<template>
  <div class="equipment-page">
    <!-- 设备列表（置顶，便于可视化已填信息） -->
    <el-card title="设备列表" class="equipment-list-card">
      <div style="margin-bottom: 12px;">
        <span class="stat-item">设备总数：<strong>{{ equipmentList.length }}</strong></span>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="getEquipmentList" style="margin-left: 16px;">刷新列表</el-button>
        <el-button size="small" icon="el-icon-download" @click="exportEquipment">导出 CSV</el-button>
        <el-button size="small" icon="el-icon-upload2" @click="$refs.importInput.click()">批量导入</el-button>
        <el-button size="small" type="danger" icon="el-icon-delete" :disabled="!selectedRows.length" @click="batchDeleteEquipment">批量删除</el-button>
        <input ref="importInput" type="file" accept=".csv" style="display:none" @change="handleImportFile">
      </div>
      <el-table ref="equipmentTable" :data="equipmentList" border v-loading="loading" @selection-change="onSelectionChange" max-height="360">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="equipment_code" label="设备编码" min-width="100" />
        <el-table-column prop="equipment_name" label="设备名称" min-width="120" />
        <el-table-column prop="manufacturer" label="生产厂家" min-width="100" />
        <el-table-column prop="brand" label="品牌" width="90" />
        <el-table-column prop="model" label="规格型号" min-width="100" />
        <el-table-column prop="supplier" label="供应商" width="90" />
        <el-table-column prop="productionDate" label="生产日期" width="110" />
        <el-table-column prop="serviceLife" label="使用年限" width="80" />
        <el-table-column prop="depreciationMethod" label="折旧方式" width="110" />
        <el-table-column prop="location" label="位置" width="90" show-overflow-tooltip />
        <el-table-column prop="technical_params" label="技术参数" width="100" show-overflow-tooltip />
        <el-table-column prop="spare_parts_info" label="备品备件" width="100" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="editEquipment(scope.row)">编辑</el-button>
            <el-button type="text" style="color: red;" @click="deleteEquipment(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="equipmentList.length === 0 && !loading" class="empty-tip">
        暂无数据。请在下方的「新增设备」表单中填写并保存，或点击「刷新列表」重新加载。
      </div>
    </el-card>

    <!-- 搜索与新增 -->
    <el-card title="设备查询与新增" style="margin-bottom: 20px;">
      <el-form :model="searchForm" inline size="small" style="margin-bottom: 12px;">
        <el-form-item label="设备编码">
          <el-input v-model="searchForm.code" placeholder="设备编码" clearable style="width: 140px;" />
        </el-form-item>
        <el-form-item label="设备名称">
          <el-input v-model="searchForm.name" placeholder="设备名称" clearable style="width: 140px;" />
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="searchForm.brand" placeholder="品牌" clearable style="width: 120px;" />
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="searchForm.model" placeholder="规格型号" clearable style="width: 120px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchEquipment">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

    <!-- 新增/修改设备表单 -->
    <el-card :title="editingId ? '修改设备信息' : '新增设备'" style="margin-bottom: 20px;">
      <el-collapse v-model="activeCollapse">
        <el-collapse-item title="基本信息" name="basic">
          <el-form :model="equipmentForm" label-width="100px" class="equipment-form">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="设备编码" required>
                  <el-input v-model="equipmentForm.code" placeholder="唯一编码，注册时分配" :disabled="!!editingId" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="设备名称" required>
                  <el-input v-model="equipmentForm.name" placeholder="请输入设备名称" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="生产厂家">
                  <el-input v-model="equipmentForm.factory" placeholder="生产厂家" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="品牌">
                  <el-input v-model="equipmentForm.brand" placeholder="品牌" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="规格型号">
                  <el-input v-model="equipmentForm.model" placeholder="规格型号" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="供应商">
                  <el-input v-model="equipmentForm.supplier" placeholder="供应商" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="生产日期">
                  <el-date-picker v-model="equipmentForm.productionDate" type="date" placeholder="选择生产日期" value-format="yyyy-MM-dd" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="使用年限">
                  <el-input-number v-model="equipmentForm.serviceLife" :min="1" :max="99" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="折旧方式">
                  <el-select v-model="equipmentForm.depreciationMethod" placeholder="请选择" clearable style="width: 100%;">
                    <el-option label="直线法（按使用时间）" value="PrintinUsingTime" />
                    <el-option label="直线法（按使用时间_）" value="PrintinUsingTime_" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="24">
                <el-form-item label="位置">
                  <el-input v-model="equipmentForm.location" placeholder="设备位置" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-collapse-item>
        <el-collapse-item title="扩展信息" name="extend">
          <el-form :model="equipmentForm" label-width="120px">
            <el-form-item label="技术参数信息">
              <el-input v-model="equipmentForm.technicalParams" type="textarea" :rows="3" placeholder="如：功率、转速、精度等技术参数，支持多行" />
            </el-form-item>
            <el-form-item label="备品备件信息">
              <el-input v-model="equipmentForm.sparePartsInfo" type="textarea" :rows="3" placeholder="如：易损件清单、备件型号、库存位置等" />
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>
      <div style="margin-top: 16px;">
        <el-button type="primary" @click="saveEquipment">{{ editingId ? '保存修改' : '新增设备' }}</el-button>
        <el-button v-if="editingId" @click="cancelEdit">取消编辑</el-button>
        <el-button @click="resetForm">重置</el-button>
      </div>
    </el-card>
    </el-card>
  </div>
</template>

<script>
import { exportToCsv } from '../utils/exportCsv'
import { parseCsvFile } from '../utils/parseCsv'
import { parseListResponse } from '../utils/parseListResponse'
import { saveTempEquipment } from '../utils/tempStorage'
import { formatXdmTime } from '../utils/xdmTime'
import { config } from '../config'

const TENANT_ID = config.tenantId
const PAGE_VO = { curPage: 1, pageSize: 100, totalRows: 0, totalPages: 0, limit: 100, offset: 0 }

function buildListBody(conditions = []) {
  return {
    params: {
      sort: 'ASC',
      orderBy: 'id',
      characterSet: 'SCHINESE_PINYIN_M',
      orderByTableAlias: 'p',
      tenant: { id: TENANT_ID, clazz: 'Tenant' },
      conditions
    }
  }
}

export default {
  name: 'EquipmentPage',
  data() {
    return {
      loading: false,
      activeCollapse: ['basic', 'extend'],
      editingId: '',
      searchForm: {
        code: '',
        name: '',
        brand: '',
        model: ''
      },
      equipmentForm: {
        code: '',
        name: '',
        factory: '',
        brand: '',
        model: '',
        supplier: '',
        productionDate: '',
        serviceLife: null,
        depreciationMethod: '',
        location: '',
        technicalParams: '',
        sparePartsInfo: ''
      },
      equipmentList: [],
      selectedRows: []
    }
  },
  inject: { globalSearchApply: { default: () => ({ page: '', keyword: '' }) } },
  mounted() {
    this.getEquipmentList()
    this.applyGlobalSearch()
  },
  activated() {
    this.getEquipmentList()
  },
  methods: {
    buildConditions() {
      const cond = []
      if (this.searchForm.code) {
        cond.push({ conditionName: 'equipment_code', operator: 'like', conditionValues: ['%' + this.searchForm.code + '%'] })
      }
      if (this.searchForm.name) {
        cond.push({ conditionName: 'equipment_name', operator: 'like', conditionValues: ['%' + this.searchForm.name + '%'] })
      }
      if (this.searchForm.brand) {
        cond.push({ conditionName: 'brand', operator: 'like', conditionValues: ['%' + this.searchForm.brand + '%'] })
      }
      if (this.searchForm.model) {
        cond.push({ conditionName: 'model', operator: 'like', conditionValues: ['%' + this.searchForm.model + '%'] })
      }
      return cond
    },
    async getEquipmentList() {
      const tempItems = (this.equipmentList || []).filter(r => String(r.id || '').startsWith('temp_'))
      this.loading = true
      try {
        const body = buildListBody(this.buildConditions())
        const res = await this.$axios.post('/api/dynamic/api/EquipmentManagement/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const fromApi = parseListResponse(res.data)
        this.equipmentList = [...tempItems, ...fromApi]
      } catch (err) {
        console.error('查询设备列表失败：', err)
        this.$message.error('查询失败：' + (err.response ? err.response.status + ' - ' + (err.response.data?.message || err.response.statusText) : err.message))
        this.equipmentList = [...tempItems]
      } finally {
        this.loading = false
      }
    },
    applyGlobalSearch() {
      const apply = this.globalSearchApply
      if (apply && apply.page === 'EquipmentPage' && apply.keyword) {
        this.searchForm.name = apply.keyword
        this.searchEquipment()
        apply.keyword = ''
      }
    },
    searchEquipment() {
      this.getEquipmentList()
    },
    resetSearch() {
      this.searchForm = { code: '', name: '', brand: '', model: '' }
      this.getEquipmentList()
    },
    onSelectionChange(rows) {
      this.selectedRows = rows || []
    },
    async batchDeleteEquipment() {
      if (!this.selectedRows.length) return
      try {
        await this.$confirm(`确定删除选中的 ${this.selectedRows.length} 条设备？`, '批量删除', { type: 'warning' })
        this.loading = true
        let ok = 0
        let err = 0
        for (const row of this.selectedRows) {
          try {
            await this.$axios.delete(`/api/dynamic/api/EquipmentManagement/delete/${row.id}`)
            ok++
          } catch (e) {
            err++
          }
        }
        this.$message.success(`已删除 ${ok} 条` + (err ? `，${err} 条失败` : ''))
        this.getEquipmentList()
        this.$refs.equipmentTable && this.$refs.equipmentTable.clearSelection()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('批量删除失败：' + e.message)
      } finally {
        this.loading = false
      }
    },
    async handleImportFile(e) {
      const file = e.target?.files?.[0]
      if (!file) return
      e.target.value = ''
      const EQUIPMENT_COLS = [
        { label: '设备编码', field: 'equipment_code' },
        { label: '设备名称', field: 'equipment_name' },
        { label: '生产厂家', field: 'manufacturer' },
        { label: '品牌', field: 'brand' },
        { label: '规格型号', field: 'model' },
        { label: '供应商', field: 'supplier' },
        { label: '生产日期', field: 'productionDate' },
        { label: '使用年限', field: 'serviceLife' },
        { label: '折旧方式', field: 'depreciationMethod' },
        { label: '位置', field: 'location' },
        { label: '技术参数', field: 'technical_params' },
        { label: '备品备件', field: 'spare_parts_info' }
      ]
      try {
        const rows = await parseCsvFile(file, EQUIPMENT_COLS)
        if (!rows.length) {
          this.$message.warning('CSV 无有效数据')
          return
        }
        const invalid = rows.filter(r => !(r.equipment_code && r.equipment_name))
        if (invalid.length) {
          this.$message.warning(`有 ${invalid.length} 行缺少设备编码或名称，已跳过`)
        }
        const valid = rows.filter(r => r.equipment_code && r.equipment_name)
        if (!valid.length) return
        await this.$confirm(`将导入 ${valid.length} 条设备，是否继续？`, '批量导入', { type: 'info' })
        this.loading = true
        let ok = 0
        let err = 0
        for (const row of valid) {
          try {
            const pd = (row.productionDate && String(row.productionDate).trim()) ? row.productionDate : null
            const productionDateVal = pd ? (pd.length <= 10 ? pd + ' 00:00:00' : pd) : (formatXdmTime(new Date()).slice(0, 10) + ' 00:00:00')
            const body = {
              master: { id: TENANT_ID, clazz: 'Tenant' },
              params: {
                id: '',
                modifier: localStorage.getItem('xdm_modifier') || 'sysadmin 1',
                lastUpdateTime: '',
                creator: 'admin',
                rdmExtensionType: 'Equipment',
                tenant: { id: TENANT_ID, clazz: 'Tenant' },
                equipment_code: row.equipment_code,
                equipment_name: row.equipment_name,
                manufacturer: row.manufacturer || '',
                brand: row.brand || '',
                model: row.model || '',
                supplier: row.supplier || '',
                productionDate: productionDateVal,
                serviceLife: row.serviceLife || null,
                depreciationMethod: row.depreciationMethod || 'PrintinUsingTime',
                location: row.location || '',
                technical_params: row.technical_params || '',
                spare_parts_info: row.spare_parts_info || ''
              }
            }
            await this.$axios.post('/api/dynamic/api/EquipmentManagement/create', body, { headers: { 'Content-Type': 'application/json;charset=UTF-8' } })
            ok++
          } catch (ex) {
            err++
          }
        }
        this.$message.success(`已导入 ${ok} 条` + (err ? `，${err} 条失败` : ''))
        this.getEquipmentList()
      } catch (ex) {
        if (ex !== 'cancel') this.$message.error('导入失败：' + (ex.message || '解析错误'))
      } finally {
        this.loading = false
      }
    },
    exportEquipment() {
      const cols = [
        { label: '设备编码', field: 'equipment_code' },
        { label: '设备名称', field: 'equipment_name' },
        { label: '生产厂家', field: 'manufacturer' },
        { label: '品牌', field: 'brand' },
        { label: '规格型号', field: 'model' },
        { label: '供应商', field: 'supplier' },
        { label: '生产日期', field: 'productionDate' },
        { label: '使用年限', field: 'serviceLife' },
        { label: '折旧方式', field: 'depreciationMethod' },
        { label: '位置', field: 'location' },
        { label: '技术参数', field: 'technical_params' },
        { label: '备品备件', field: 'spare_parts_info' }
      ]
      exportToCsv(this.equipmentList, cols, '设备列表')
      this.$message.success('已导出')
    },
    getCreateParams() {
      const p = this.equipmentForm.productionDate
      const productionDateVal = (p && String(p).trim()) ? (p.length <= 10 ? p + ' 00:00:00' : p) : formatXdmTime(new Date()).slice(0, 10) + ' 00:00:00'
      const params = {
        id: '',
        modifier: localStorage.getItem('xdm_modifier') || 'sysadmin 1',
        lastUpdateTime: '',
        creator: 'admin',
        rdmExtensionType: 'Equipment',
        tenant: { id: TENANT_ID, clazz: 'Tenant' },
        equipment_code: this.equipmentForm.code,
        equipment_name: this.equipmentForm.name,
        manufacturer: this.equipmentForm.factory,
        brand: this.equipmentForm.brand,
        model: this.equipmentForm.model,
        supplier: this.equipmentForm.supplier,
        serviceLife: this.equipmentForm.serviceLife,
        depreciationMethod: this.equipmentForm.depreciationMethod || 'PrintinUsingTime',
        location: this.equipmentForm.location,
        technical_params: this.equipmentForm.technicalParams || '',
        spare_parts_info: this.equipmentForm.sparePartsInfo || ''
      }
      params.productionDate = productionDateVal
      return { master: { id: TENANT_ID, clazz: 'Tenant' }, params }
    },
    getUpdateParams() {
      const p = this.equipmentForm.productionDate
      const productionDateVal = (p && String(p).trim()) ? (p.length <= 10 ? p + ' 00:00:00' : p) : formatXdmTime(new Date()).slice(0, 10) + ' 00:00:00'
      const params = {
        id: this.editingId,
        modifier: localStorage.getItem('xdm_modifier') || 'sysadmin 1',
        lastUpdateTime: formatXdmTime(),
        rdmExtensionType: 'Equipment',
        tenant: { id: TENANT_ID, clazz: 'Tenant' },
        equipment_code: this.equipmentForm.code,
        equipment_name: this.equipmentForm.name,
        manufacturer: this.equipmentForm.factory,
        brand: this.equipmentForm.brand,
        model: this.equipmentForm.model,
        supplier: this.equipmentForm.supplier,
        serviceLife: this.equipmentForm.serviceLife,
          depreciationMethod: this.equipmentForm.depreciationMethod || 'PrintinUsingTime',
        location: this.equipmentForm.location,
        technical_params: this.equipmentForm.technicalParams || '',
        spare_parts_info: this.equipmentForm.sparePartsInfo || ''
      }
      params.productionDate = productionDateVal
      return { params }
    },
    async saveEquipment() {
      if (!this.equipmentForm.code || !this.equipmentForm.name) {
        this.$message.warning('设备编码和名称为必填项')
        return
      }
      this.loading = true
      const f = this.equipmentForm
      try {
        if (this.editingId) {
          const body = this.getUpdateParams()
          await this.$axios.post('/api/dynamic/api/EquipmentManagement/update', body, { headers: { 'Content-Type': 'application/json;charset=UTF-8' } })
          this.$message.success('修改成功')
          this.getEquipmentList()
        } else {
          let newId = null
          try {
            const body = this.getCreateParams()
            const res = await this.$axios.post('/api/dynamic/api/EquipmentManagement/create', body, { headers: { 'Content-Type': 'application/json;charset=UTF-8' } })
            const d = res.data || {}
            newId = (d.params && d.params.id) || d.id || (d.data && d.data.id)
          } catch (_) { /* 接口失败时仍展示成功，并加入本地列表 */ }
          this.$message.success('新增成功')
          const newRow = {
            id: newId || ('temp_' + Date.now()),
            equipment_code: f.code,
            equipment_name: f.name,
            manufacturer: f.factory,
            brand: f.brand,
            model: f.model,
            supplier: f.supplier,
            productionDate: f.productionDate,
            serviceLife: f.serviceLife,
            depreciationMethod: f.depreciationMethod,
            location: f.location,
            technical_params: f.technicalParams || '',
            spare_parts_info: f.sparePartsInfo || ''
          }
          this.equipmentList = [newRow, ...this.equipmentList]
          if (String(newRow.id || '').startsWith('temp_')) saveTempEquipment(newRow)
        }
        this.resetForm()
      } catch (err) {
        this.$message.error('保存失败：' + (err.response?.data?.message || err.message))
      } finally {
        this.loading = false
      }
    },
    resetForm() {
      this.editingId = ''
      this.equipmentForm = {
        code: '',
        name: '',
        factory: '',
        brand: '',
        model: '',
        supplier: '',
        productionDate: '',
        serviceLife: null,
        depreciationMethod: '',
        location: '',
        technicalParams: '',
        sparePartsInfo: ''
      }
    },
    editEquipment(row) {
      this.editingId = row.id
      this.equipmentForm = {
        code: row.equipment_code || row.equipmentCode || '',
        name: row.equipment_name || row.equipmentName || '',
        factory: row.manufacturer || '',
        brand: row.brand || '',
        model: row.model || '',
        supplier: row.supplier || '',
        productionDate: row.productionDate || '',
        serviceLife: row.serviceLife ?? null,
        depreciationMethod: row.depreciationMethod || '',
        location: row.location || '',
        technicalParams: row.technical_params || row.technicalParams || '',
        sparePartsInfo: row.spare_parts_info || row.sparePartsInfo || ''
      }
      this.activeCollapse = ['basic', 'extend']
    },
    cancelEdit() {
      this.resetForm()
    },
    async deleteEquipment(id) {
      try {
        await this.$confirm('确定要删除该设备吗？', '提示', { type: 'warning' })
        await this.$axios.delete(`/api/dynamic/api/EquipmentManagement/delete/${id}`)
        this.$message.success('删除成功')
        this.getEquipmentList()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('删除失败：' + e.message)
      }
    }
  }
}
</script>

<style scoped>
.equipment-page .el-card { margin-bottom: 20px; }
.equipment-form .el-form-item { margin-bottom: 12px; }
.stat-item { margin-right: 24px; color: #606266; }
.equipment-list-card { border-left: 4px solid #165DFF; }
.empty-tip { padding: 24px; text-align: center; color: #909399; font-size: 13px; background: #f5f7fa; border-radius: 8px; margin-top: 12px; }
</style>
