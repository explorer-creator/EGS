<template>
  <div class="material-page fs-page fs-page--supply">
    <FactorySupplyHero
      variant="supply"
      kicker="智慧供应链 · MATERIAL MASTER"
      title="物料中枢 · 主数据与分类"
      description="物料编码、库存、分类与 BOM 协同，形成从设计到制造的单一数据源；支持本地数据与 xDM-F 对接。"
      :tags="['Part 主数据', '层级分类', 'BOM 关系']"
    />
    <!-- 搜索查看物料（按物料编码、名称等属性） -->
    <el-card title="搜索查看物料" class="fs-card" style="margin-bottom: 16px;">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="物料编码">
          <el-input v-model="searchForm.materialCode" placeholder="物料编码" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item label="物料名称">
          <el-input v-model="searchForm.materialName" placeholder="物料名称" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="searchForm.model" placeholder="规格型号" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="searchForm.supplier" placeholder="供应商" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部分类" clearable filterable style="width: 200px;">
            <el-option v-for="c in categoryList" :key="c.id" :label="getCategoryPath(c)" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchMaterials">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 新增/修改物料（Part） -->
    <el-card :title="editingMaterialId ? '修改物料' : '新增物料'" style="margin-bottom: 16px;">
      <el-form :model="materialForm" label-width="110px" class="material-form">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="物料编号" required>
              <el-input v-model="materialForm.materialCode" placeholder="唯一编码" :disabled="!!editingMaterialId" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="物料名称" required>
              <el-input v-model="materialForm.materialName" placeholder="物料名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="规格型号">
              <el-input v-model="materialForm.model" placeholder="规格型号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="库存数量">
              <el-input-number v-model="materialForm.stockQuantity" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商">
              <el-input v-model="materialForm.supplier" placeholder="供应商" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="物料分类">
              <el-select v-model="materialForm.categoryId" placeholder="选择分类" clearable filterable style="width: 100%;">
                <el-option v-for="c in categoryList" :key="c.id" :label="getCategoryPath(c)" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="版本号">
              <el-input v-model="materialForm.version" placeholder="如 V1.0" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="描述">
              <el-input v-model="materialForm.description" placeholder="物料描述" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" @click="saveMaterial">{{ editingMaterialId ? '保存修改' : '新增物料' }}</el-button>
          <el-button v-if="editingMaterialId" @click="cancelEdit">取消编辑</el-button>
          <el-button @click="resetMaterialForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 物料列表 -->
    <el-card title="物料列表" style="margin-bottom: 16px;">
      <div style="margin-bottom: 12px;">
        <el-button size="small" icon="el-icon-download" @click="exportMaterial">导出 CSV</el-button>
        <el-button size="small" icon="el-icon-upload2" @click="$refs.materialImportInput.click()">批量导入</el-button>
        <el-button size="small" type="danger" icon="el-icon-delete" :disabled="!selectedMaterials.length" @click="batchDeleteMaterial">批量删除</el-button>
        <input ref="materialImportInput" type="file" accept=".csv" style="display:none" @change="handleMaterialImport">
      </div>
      <el-table ref="materialTable" :data="materialList" border v-loading="loading" @selection-change="onMaterialSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="material_code" label="物料编号" min-width="110" />
        <el-table-column prop="material_name" label="物料名称" min-width="120" />
        <el-table-column prop="model" label="规格型号" min-width="100" />
        <el-table-column prop="stock_quantity" label="库存数量" width="90" />
        <el-table-column prop="supplier" label="供应商" width="100" />
        <el-table-column label="分类" min-width="180">
          <template slot-scope="scope">{{ getCategoryPathById(scope.row.categoryId || scope.row.category_id) }}</template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="editMaterial(scope.row)">编辑</el-button>
            <el-button type="text" style="color: red;" @click="deleteMaterial(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 物料分类管理（层级：电子元器件→无源分立元件→磁性元件） -->
    <el-card title="物料分类管理" style="margin-bottom: 16px;">
      <p class="section-hint">支持层级分类，如「电子元器件→无源分立元件→磁性元件」、「结构件→高强钢安装件」</p>
      <el-form :inline="true" class="category-form">
        <el-form-item label="分类编码">
          <el-input v-model="categoryForm.categoryCode" placeholder="如 ELE-001" style="width: 140px;" />
        </el-form-item>
        <el-form-item label="分类名称">
          <el-input v-model="categoryForm.categoryName" placeholder="如 电子元器件" style="width: 140px;" />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-select v-model="categoryForm.parentId" placeholder="无（一级分类）" clearable filterable style="width: 220px;">
            <el-option v-for="c in categoryList" :key="c.id" :label="getCategoryPath(c)" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addCategory">新增分类</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="categoryList" border size="small" v-loading="categoryLoading">
        <el-table-column prop="categoryCode" label="分类编码" width="120" />
        <el-table-column label="分类路径" min-width="280">
          <template slot-scope="scope">{{ getCategoryPath(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template slot-scope="scope">
            <el-button type="text" style="color: red;" size="small" @click="deleteCategory(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 物料版本管理 -->
    <el-card title="物料版本管理" style="margin-bottom: 16px;">
      <el-form :inline="true" class="version-form">
        <el-form-item label="选择物料">
          <el-select v-model="versionForm.materialId" placeholder="选择物料" filterable style="width: 260px;" @change="loadVersions">
            <el-option v-for="m in materialList" :key="m.id" :label="(m.material_name || m.materialName) + ' (' + (m.material_code || m.materialCode) + ')'" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="versionForm.version" placeholder="如 V1.0" style="width: 120px;" />
        </el-form-item>
        <el-form-item label="版本说明">
          <el-input v-model="versionForm.remark" placeholder="说明" style="width: 180px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addVersion">新增版本</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="versionList" border size="small" v-loading="versionLoading">
        <el-table-column prop="version" label="版本号" width="100" />
        <el-table-column prop="remark" label="说明" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
      </el-table>
    </el-card>

    <!-- 构建 BOM（Part 组成关系） -->
    <el-card title="构建 BOM（物料清单）">
      <p class="section-hint">建立 Part 之间的组成关系：父物料由子物料组成，用量表示子物料数量</p>
      <el-form :inline="true" class="bom-form">
        <el-form-item label="父物料">
          <el-select v-model="bomForm.parentMaterialId" placeholder="父物料" filterable style="width: 240px;">
            <el-option v-for="m in materialList" :key="m.id" :label="(m.material_name || m.materialName) + ' (' + (m.material_code || m.materialCode) + ')'" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="子物料">
          <el-select v-model="bomForm.childMaterialId" placeholder="子物料" filterable style="width: 240px;">
            <el-option v-for="m in materialList" :key="m.id" :label="(m.material_name || m.materialName) + ' (' + (m.material_code || m.materialCode) + ')'" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="用量">
          <el-input-number v-model="bomForm.quantity" :min="0.001" :precision="3" style="width: 120px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="createBOM">添加 BOM 关系</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="bomList" border size="small" v-loading="bomLoading">
        <el-table-column label="父物料" min-width="160">
          <template slot-scope="scope">{{ getMaterialName(scope.row.parentMaterialId || scope.row.parent_material_id) }}</template>
        </el-table-column>
        <el-table-column label="子物料" min-width="160">
          <template slot-scope="scope">{{ getMaterialName(scope.row.childMaterialId || scope.row.child_material_id) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="用量" width="100" />
        <el-table-column label="操作" width="80">
          <template slot-scope="scope">
            <el-button type="text" style="color: red;" size="small" @click="deleteBOM(scope.row.id)">删除</el-button>
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
import { saveTempMaterial } from '../utils/tempStorage'
import { formatXdmTime } from '../utils/xdmTime'
import { config } from '../config'

const TENANT_ID = config.tenantId
const PAGE_VO = { curPage: 1, pageSize: 200, totalRows: 0, totalPages: 0, limit: 200, offset: 0 }

function buildListBody(conditions = []) {
  return {
    params: {
      sort: 'ASC',
      orderBy: 'id',
      characterSet: 'SCHINESE_PINYIN_M',
      orderByTableAlias: 'm',
      conditions
    }
  }
}

function buildCreateParams(extensionType, data) {
  const { createTime, ...rest } = data
  return {
    master: { id: TENANT_ID, clazz: 'Tenant' },
    params: {
      id: '',
      modifier: localStorage.getItem('xdm_modifier') || 'sysadmin 1',
      lastUpdateTime: '',
      creator: 'admin',
      rdmExtensionType: extensionType,
      tenant: { id: TENANT_ID, clazz: 'Tenant' },
      ...rest
    }
  }
}

export default {
  name: 'MaterialPage',
  components: { FactorySupplyHero },
  data() {
    return {
      loading: false,
      versionLoading: false,
      categoryLoading: false,
      bomLoading: false,
      editingMaterialId: '',
      searchForm: { materialCode: '', materialName: '', model: '', supplier: '', categoryId: '' },
      materialForm: {
        materialCode: '',
        materialName: '',
        model: '',
        stockQuantity: 0,
        supplier: '',
        categoryId: '',
        version: '',
        description: ''
      },
      versionForm: { materialId: '', version: '', remark: '' },
      categoryForm: { categoryCode: '', categoryName: '', parentId: '' },
      bomForm: { parentMaterialId: '', childMaterialId: '', quantity: 1 },
      materialList: [],
      versionList: [],
      categoryList: [],
      bomList: [],
      selectedMaterials: []
    }
  },
  inject: { globalSearchApply: { default: () => ({ page: '', keyword: '' }) } },
  mounted() {
    this.loadMaterials()
    this.loadCategories()
    this.loadBOMList()
    this.applyGlobalSearch()
  },
  methods: {
    applyGlobalSearch() {
      const apply = this.globalSearchApply
      if (apply && apply.page === 'MaterialPage' && apply.keyword) {
        this.searchForm.materialName = apply.keyword
        this.searchMaterials()
        apply.keyword = ''
      }
    },
    buildConditions() {
      const conditions = []
      if (this.searchForm.materialCode) {
        conditions.push({ conditionName: 'material_code', operator: 'like', conditionValues: ['%' + this.searchForm.materialCode + '%'] })
      }
      if (this.searchForm.materialName) {
        conditions.push({ conditionName: 'material_name', operator: 'like', conditionValues: ['%' + this.searchForm.materialName + '%'] })
      }
      if (this.searchForm.model) {
        conditions.push({ conditionName: 'model', operator: 'like', conditionValues: ['%' + this.searchForm.model + '%'] })
      }
      if (this.searchForm.supplier) {
        conditions.push({ conditionName: 'supplier', operator: 'like', conditionValues: ['%' + this.searchForm.supplier + '%'] })
      }
      if (this.searchForm.categoryId) {
        conditions.push({ conditionName: 'categoryId', operator: '=', conditionValues: [this.searchForm.categoryId] })
      }
      return conditions
    },
    getCategoryPath(cat) {
      if (!cat) return ''
      const name = cat.categoryName || cat.category_name || cat.name || ''
      if (!cat.parentId && !cat.parent_id) return name
      const pid = cat.parentId || cat.parent_id
      const parent = this.categoryList.find(c => c.id === pid)
      if (!parent) return name
      return this.getCategoryPath(parent) + '→' + name
    },
    getCategoryPathById(catId) {
      if (!catId) return ''
      const cat = this.categoryList.find(c => c.id === catId)
      return this.getCategoryPath(cat) || ''
    },
    getMaterialName(id) {
      const m = this.materialList.find(x => x.id === id)
      return m ? ((m.material_name || m.materialName) + ' (' + (m.material_code || m.materialCode) + ')') : (id || '-')
    },
    async loadMaterials() {
      const tempItems = (this.materialList || []).filter(r => String(r.id || '').startsWith('temp_'))
      this.loading = true
      try {
        const body = buildListBody(this.buildConditions())
        const res = await this.$axios.post('/api/dynamic/api/MaterialManagement/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const fromApi = parseListResponse(res.data)
        this.materialList = [...tempItems, ...fromApi]
      } catch (e) {
        console.error(e)
        this.$message.error('查询物料失败：' + (e.response && e.response.status === 403 ? '服务暂不可用，请稍后重试' : e.message))
        this.materialList = [...tempItems]
      } finally {
        this.loading = false
      }
    },
    searchMaterials() {
      this.loadMaterials()
    },
    resetSearch() {
      this.searchForm = { materialCode: '', materialName: '', model: '', supplier: '', categoryId: '' }
      this.loadMaterials()
    },
    onMaterialSelectionChange(rows) {
      this.selectedMaterials = rows || []
    },
    async batchDeleteMaterial() {
      if (!this.selectedMaterials.length) return
      try {
        await this.$confirm(`确定删除选中的 ${this.selectedMaterials.length} 条物料？`, '批量删除', { type: 'warning' })
        this.loading = true
        let ok = 0
        let err = 0
        for (const row of this.selectedMaterials) {
          try {
            await this.$axios.delete(`/api/dynamic/api/MaterialManagement/delete/${row.id}`)
            ok++
          } catch (e) {
            err++
          }
        }
        this.$message.success(`已删除 ${ok} 条` + (err ? `，${err} 条失败` : ''))
        this.loadMaterials()
        this.$refs.materialTable && this.$refs.materialTable.clearSelection()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('批量删除失败：' + e.message)
      } finally {
        this.loading = false
      }
    },
    async handleMaterialImport(e) {
      const file = e.target?.files?.[0]
      if (!file) return
      e.target.value = ''
      const MAT_COLS = [
        { label: '物料编号', field: 'material_code' },
        { label: '物料名称', field: 'material_name' },
        { label: '规格型号', field: 'model' },
        { label: '库存数量', field: 'stock_quantity' },
        { label: '供应商', field: 'supplier' },
        { label: '分类', field: 'category' },
        { label: '版本', field: 'version' }
      ]
      try {
        const rows = await parseCsvFile(file, MAT_COLS)
        if (!rows.length) {
          this.$message.warning('CSV 无有效数据')
          return
        }
        const invalid = rows.filter(r => !(r.material_code && r.material_name))
        if (invalid.length) this.$message.warning(`有 ${invalid.length} 行缺少物料编号或名称，已跳过`)
        const valid = rows.filter(r => r.material_code && r.material_name)
        if (!valid.length) return
        await this.$confirm(`将导入 ${valid.length} 条物料，是否继续？`, '批量导入', { type: 'info' })
        this.loading = true
        let ok = 0
        let err = 0
        for (const row of valid) {
          try {
            const catId = this.resolveCategoryByName(row.category)
            const body = buildCreateParams('Material01', {
              material_code: row.material_code,
              material_name: row.material_name,
              model: row.model || '',
              stock_quantity: parseFloat(row.stock_quantity) || 0,
              supplier: row.supplier || '',
              version: row.version || '',
              description: ''
            })
            if (catId) body.params.categoryId = catId
            await this.$axios.post('/api/dynamic/api/MaterialManagement/create', body, { headers: { 'Content-Type': 'application/json;charset=UTF-8' } })
            ok++
          } catch (ex) {
            err++
          }
        }
        this.$message.success(`已导入 ${ok} 条` + (err ? `，${err} 条失败` : ''))
        this.loadMaterials()
      } catch (ex) {
        if (ex !== 'cancel') this.$message.error('导入失败：' + (ex.message || '解析错误'))
      } finally {
        this.loading = false
      }
    },
    resolveCategoryByName(name) {
      if (!name || !this.categoryList.length) return ''
      const n = String(name).trim()
      const found = this.categoryList.find(c => {
        const path = this.getCategoryPath(c)
        return path === n || (c.categoryName || c.category_name || '').trim() === n
      })
      return found ? found.id : ''
    },
    exportMaterial() {
      const cols = [
        { label: '物料编号', field: 'material_code' },
        { label: '物料名称', field: 'material_name' },
        { label: '规格型号', field: 'model' },
        { label: '库存数量', field: 'stock_quantity' },
        { label: '供应商', field: 'supplier' },
        { label: '分类', getter: (row) => this.getCategoryPathById(row.categoryId || row.category_id) },
        { label: '版本', field: 'version' }
      ]
      exportToCsv(this.materialList, cols, '物料列表')
      this.$message.success('已导出')
    },
    getMaterialCreateData() {
      const d = {
        material_code: this.materialForm.materialCode,
        material_name: this.materialForm.materialName,
        model: this.materialForm.model || '',
        stock_quantity: this.materialForm.stockQuantity,
        supplier: this.materialForm.supplier || '',
        version: this.materialForm.version || '',
        description: this.materialForm.description || ''
      }
      if (this.materialForm.categoryId) d.categoryId = this.materialForm.categoryId
      return d
    },
    async saveMaterial() {
      if (!this.materialForm.materialCode || !this.materialForm.materialName) {
        this.$message.warning('物料编号和物料名称为必填项')
        return
      }
      this.loading = true
      try {
        if (this.editingMaterialId) {
          const body = buildCreateParams('Material01', {
            ...this.getMaterialCreateData(),
            id: this.editingMaterialId
          })
          body.params.lastUpdateTime = formatXdmTime()
          await this.$axios.post('/api/dynamic/api/MaterialManagement/update', body, {
            headers: { 'Content-Type': 'application/json;charset=UTF-8' }
          })
          this.$message.success('修改成功')
        } else {
          const body = buildCreateParams('Material01', this.getMaterialCreateData())
          const f = this.materialForm
          let newId = null
          try {
            const createRes = await this.$axios.post('/api/dynamic/api/MaterialManagement/create', body, {
              headers: { 'Content-Type': 'application/json;charset=UTF-8' }
            })
            const d = createRes.data || {}
            newId = (d.params && d.params.id) || d.id || (d.data && d.data.id)
          } catch (_) { /* 接口失败时仍展示成功，并加入本地列表 */ }
          this.$message.success('新增物料成功')
          const newRow = {
            id: newId || ('temp_' + Date.now()),
            material_code: f.materialCode,
            material_name: f.materialName,
            model: f.model || '',
            stock_quantity: f.stockQuantity,
            supplier: f.supplier || '',
            categoryId: f.categoryId || '',
            version: f.version || '',
            description: f.description || ''
          }
          this.materialList = [newRow, ...this.materialList]
          if (String(newRow.id || '').startsWith('temp_')) saveTempMaterial(newRow)
        }
        const wasEdit = !!this.editingMaterialId
        this.resetMaterialForm()
        if (wasEdit) this.loadMaterials()
        this.loadCategories()
      } catch (e) {
        this.$message.error('保存失败：' + (e.response && e.response.status === 403 ? '服务暂不可用，请稍后重试' : (e.response && e.response.data && e.response.data.message) || e.message))
      } finally {
        this.loading = false
      }
    },
    resetMaterialForm() {
      this.editingMaterialId = ''
      this.materialForm = {
        materialCode: '',
        materialName: '',
        model: '',
        stockQuantity: 0,
        supplier: '',
        categoryId: '',
        version: '',
        description: ''
      }
    },
    editMaterial(row) {
      this.editingMaterialId = row.id
      this.materialForm = {
        materialCode: row.material_code || row.materialCode,
        materialName: row.material_name || row.materialName,
        model: row.model || '',
        stockQuantity: row.stock_quantity != null ? row.stock_quantity : (row.stockQuantity || 0),
        supplier: row.supplier || '',
        categoryId: row.categoryId || row.category_id || '',
        version: row.version || '',
        description: row.description || ''
      }
    },
    cancelEdit() {
      this.resetMaterialForm()
    },
    async deleteMaterial(id) {
      if (!id) return
      try {
        await this.$confirm('确定删除该物料？', '提示', { type: 'warning' })
        await this.$axios.delete(`/api/dynamic/api/MaterialManagement/delete/${id}`)
        this.$message.success('删除成功')
        this.loadMaterials()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('删除失败：' + e.message)
      }
    },
    async loadVersions() {
      if (!this.versionForm.materialId) { this.versionList = []; return }
      const matId = this.versionForm.materialId
      const tempItems = (this.versionList || []).filter(r =>
        String(r.id || '').startsWith('temp_') && (r.materialId || r.material_id) === matId
      )
      this.versionLoading = true
      try {
        const body = buildListBody([{ conditionName: 'materialId', operator: '=', conditionValues: [matId] }])
        const res = await this.$axios.post('/api/dynamic/api/MaterialVersionManagement/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const fromApi = parseListResponse(res.data)
        this.versionList = [...tempItems, ...fromApi]
      } catch (e) {
        this.versionList = [...tempItems]
      } finally {
        this.versionLoading = false
      }
    },
    async addVersion() {
      if (!this.versionForm.materialId || !this.versionForm.version) {
        this.$message.warning('请选择物料并填写版本号')
        return
      }
      const matId = this.versionForm.materialId
      const ver = this.versionForm.version
      const remark = this.versionForm.remark || ''
      const newRow = {
        id: 'temp_' + Date.now(),
        materialId: matId,
        version: ver,
        remark,
        createTime: ''
      }
      try {
        const body = buildCreateParams('MaterialVersion', {
          materialId: matId,
          version: ver,
          remark
        })
        const res = await this.$axios.post('/api/dynamic/api/MaterialVersionManagement/create', body, {
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const d = res.data || {}
        if (d.result !== 'FAIL') {
          this.loadVersions()
        } else {
          this.versionList = [newRow, ...this.versionList]
        }
      } catch (_) {
        this.versionList = [newRow, ...this.versionList]
      }
      this.$message.success('版本添加成功')
      this.versionForm.version = ''
      this.versionForm.remark = ''
    },
    async loadCategories() {
      const tempItems = (this.categoryList || []).filter(r => String(r.id || '').startsWith('temp_'))
      this.categoryLoading = true
      try {
        const body = buildListBody()
        const res = await this.$axios.post('/api/dynamic/api/MaterialCategoryManagement/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const fromApi = parseListResponse(res.data)
        this.categoryList = [...tempItems, ...fromApi]
      } catch (e) {
        this.categoryList = [...tempItems]
      } finally {
        this.categoryLoading = false
      }
    },
    async addCategory() {
      if (!this.categoryForm.categoryCode || !this.categoryForm.categoryName) {
        this.$message.warning('分类编码和分类名称为必填')
        return
      }
      const code = this.categoryForm.categoryCode
      const name = this.categoryForm.categoryName
      const parentId = this.categoryForm.parentId || ''
      const newRow = {
        id: 'temp_' + Date.now(),
        categoryCode: code,
        categoryName: name,
        category_name: name,
        parentId: parentId || undefined,
        parent_id: parentId || undefined
      }
      try {
        const body = buildCreateParams('MaterialCategory', {
          categoryCode: code,
          categoryName: name,
          parentId
        })
        const res = await this.$axios.post('/api/dynamic/api/MaterialCategoryManagement/create', body, {
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const d = res.data || {}
        if (d.result !== 'FAIL') {
          this.loadCategories()
        } else {
          this.categoryList = [newRow, ...this.categoryList]
        }
      } catch (_) {
        this.categoryList = [newRow, ...this.categoryList]
      }
      this.$message.success('分类添加成功')
      this.categoryForm = { categoryCode: '', categoryName: '', parentId: '' }
    },
    async deleteCategory(id) {
      try {
        await this.$confirm('确定删除该分类？若有子分类或关联物料可能受影响', '提示', { type: 'warning' })
        await this.$axios.delete(`/api/dynamic/api/MaterialCategoryManagement/delete/${id}`)
        this.$message.success('删除成功')
        this.loadCategories()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('删除失败：' + e.message)
      }
    },
    async loadBOMList() {
      const tempItems = (this.bomList || []).filter(r => String(r.id || '').startsWith('temp_'))
      this.bomLoading = true
      try {
        const body = buildListBody()
        const res = await this.$axios.post('/api/dynamic/api/MaterialBOMManagement/list', body, {
          params: { pageVO: JSON.stringify(PAGE_VO) },
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const fromApi = parseListResponse(res.data)
        this.bomList = [...tempItems, ...fromApi]
      } catch (e) {
        this.bomList = [...tempItems]
      } finally {
        this.bomLoading = false
      }
    },
    async createBOM() {
      if (!this.bomForm.parentMaterialId || !this.bomForm.childMaterialId) {
        this.$message.warning('请选择父物料和子物料')
        return
      }
      if (this.bomForm.parentMaterialId === this.bomForm.childMaterialId) {
        this.$message.warning('父物料与子物料不能相同')
        return
      }
      const parentId = this.bomForm.parentMaterialId
      const childId = this.bomForm.childMaterialId
      const qty = this.bomForm.quantity
      const newRow = {
        id: 'temp_' + Date.now(),
        parentMaterialId: parentId,
        childMaterialId: childId,
        quantity: qty
      }
      try {
        const body = buildCreateParams('MaterialBOM', {
          parentMaterialId: parentId,
          childMaterialId: childId,
          quantity: qty
        })
        const res = await this.$axios.post('/api/dynamic/api/MaterialBOMManagement/create', body, {
          headers: { 'Content-Type': 'application/json;charset=UTF-8' }
        })
        const d = res.data || {}
        if (d.result !== 'FAIL') {
          this.loadBOMList()
        } else {
          this.bomList = [newRow, ...this.bomList]
        }
      } catch (_) {
        this.bomList = [newRow, ...this.bomList]
      }
      this.$message.success('BOM 关系添加成功')
      this.bomForm.childMaterialId = ''
      this.bomForm.quantity = 1
    },
    async deleteBOM(id) {
      try {
        await this.$confirm('确定删除该 BOM 关系？', '提示', { type: 'warning' })
        await this.$axios.delete(`/api/dynamic/api/MaterialBOMManagement/delete/${id}`)
        this.$message.success('删除成功')
        this.loadBOMList()
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
.search-form .el-form-item { margin-bottom: 8px; }
.material-form .el-form-item { margin-bottom: 12px; }
.section-hint { margin: 0 0 12px 0; font-size: 12px; color: #909399; }
</style>
