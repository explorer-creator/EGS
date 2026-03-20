# xDM-F 工业数据建模与关联实体说明

基于 xDM-F 的**参考对象**、**关系实体**功能，构建设备、物料、工序、工艺及其关联关系。

---

## 一、基础实体（每错 1 个扣 4 分）

### 1. EquipmentManagement（设备）

| 属性名 | 类型 | 说明 |
|--------|------|------|
| equipment_code | String | 设备编码（唯一标识，注册时分配） |
| equipment_name | String | 设备名称 |
| manufacturer | String | 生产厂家 |
| brand | String | 品牌 |
| model | String | 规格型号 |
| supplier | String | 供应商 |
| productionDate | String/Date | 生产日期 |
| serviceLife | Integer | 使用年限 |
| depreciationMethod | String | 折旧方式 |
| location | String | 位置 |
| technical_params | String | 技术参数信息（扩展） |
| spare_parts_info | String | 备品备件信息（扩展） |

### 2. Material01（物料/Part）

| 属性名 | 类型 | 说明 |
|--------|------|------|
| material_code | String | 物料编号（唯一） |
| material_name | String | 物料名称 |
| model | String | 规格型号 |
| stock_quantity | Number | 库存数量 |
| supplier | String | 供应商 |
| categoryId | String/Reference | 分类（参考 MaterialCategory） |
| version | String | 版本号（如 V1.0） |
| description | String | 物料描述 |

### 3. WorkingProcedure（工序）

| 属性名 | 类型 | 说明 |
|--------|------|------|
| procedure_code | String | 工序编号 |
| procedure_name | String | 工序名称 |
| production_steps | String | 生产步骤 |
| production_inspection_equipment | String | 生产和检测设备（文本） |
| operator | String | 操作人员 |
| start_time | String/DateTime | 开始时间 |
| end_time | String/DateTime | 结束时间 |
| description | String | 工序描述 |

### 4. ProcessRoute（工艺/工艺路线）

| 属性名 | 类型 | 说明 |
|--------|------|------|
| route_code | String | 工艺编号 |
| route_name | String | 工艺名称 |
| product | String | 所属产品 |
| version | String | 版本 |
| description | String | 工艺描述 |
| operator | String | 操作人员 |
| operation_time | String/DateTime | 操作时间 |
| equipment_usage | String | 设备使用情况 |

---

## 二、关系实体（参考对象关联，每错 1 个扣 3 分）

### 1. ProcessRouteProcedure（工艺-工序关联）

工艺路线与工序的多对多关系，通过关系实体实现。

| 属性名 | 类型 | 说明 |
|--------|------|------|
| route_id | String 或 Reference(ProcessRoute) | 工艺路线 ID |
| procedure_id | String 或 Reference(WorkingProcedure) | 工序 ID |
| sequence_order | Integer | 顺序（1, 2, 3...） |

**xDM-F 参考对象配置**：若使用参考类型，则 `procedure` 参考 `WorkingProcedure`，`route` 参考 `ProcessRoute`。

### 2. ProcedureEquipment（工序-设备关联）

工序与设备的多对多关系。

| 属性名 | 类型 | 说明 |
|--------|------|------|
| procedure_id | String 或 Reference(WorkingProcedure) | 工序 ID |
| equipment_id | String 或 Reference(EquipmentManagement) | 设备 ID |
| usage_remark | String | 用量/说明（可选） |

**xDM-F 参考对象配置**：`procedure` 参考 `WorkingProcedure`，`equipment` 参考 `EquipmentManagement`。

### 3. ProcedureMaterial（工序-物料关联）

工序与物料的多对多关系。

| 属性名 | 类型 | 说明 |
|--------|------|------|
| procedure_id | String 或 Reference(WorkingProcedure) | 工序 ID |
| material_id | String 或 Reference(Material01) | 物料 ID |
| quantity | Number | 用量 |

**xDM-F 参考对象配置**：`procedure` 参考 `WorkingProcedure`，`material` 参考 `Material01`。

### 4. MaterialCategory（物料分类）

支持层级分类，如「电子元器件→无源分立元件→磁性元件」、「结构件→高强钢安装件」。

| 属性名 | 类型 | 说明 |
|--------|------|------|
| categoryCode | String | 分类编码 |
| categoryName | String | 分类名称 |
| parentId | String/Reference | 上级分类 ID（空为一级） |

### 5. MaterialBOM（物料清单/Part 组成关系）

Part 之间的组成关系：父物料由子物料组成。

| 属性名 | 类型 | 说明 |
|--------|------|------|
| parentMaterialId | String/Reference(Material01) | 父物料 ID |
| childMaterialId | String/Reference(Material01) | 子物料 ID |
| quantity | Number | 用量 |

### 6. MaterialVersion（物料版本）

| 属性名 | 类型 | 说明 |
|--------|------|------|
| materialId | String/Reference(Material01) | 物料 ID |
| version | String | 版本号 |
| remark | String | 版本说明 |

---

## 三、关联关系图

```
ProcessRoute (工艺路线)
    │
    └── ProcessRouteProcedure ──► WorkingProcedure (工序)
                                      │
                                      ├── ProcedureEquipment ──► EquipmentManagement (设备)
                                      │
                                      └── ProcedureMaterial ──► Material01 (物料)
```

---

## 四、xDM-F 设计态操作步骤

1. **创建基础实体**：EquipmentManagement、Material01、WorkingProcedure、ProcessRoute
2. **创建关系实体**：ProcessRouteProcedure、ProcedureEquipment、ProcedureMaterial
3. **配置参考对象**：
   - ProcessRouteProcedure：route → ProcessRoute，procedure → WorkingProcedure
   - ProcedureEquipment：procedure → WorkingProcedure，equipment → EquipmentManagement
   - ProcedureMaterial：procedure → WorkingProcedure，material → Material01
4. **发布并部署**到运行态

---

## 五、API 路径映射

| 前端路径 | xDM-F 实体 |
|----------|------------|
| /api/dynamic/api/EquipmentManagement/* | EquipmentManagement |
| /api/dynamic/api/MaterialManagement/* | Material01 |
| /api/dynamic/api/ProcedureManagement/* | WorkingProcedure |
| /api/dynamic/api/ProcessRoute/* | ProcessRoute |
| /api/dynamic/api/ProcessRouteProcedure/* | ProcessRouteProcedure |
| /api/dynamic/api/ProcedureEquipment/* | ProcedureEquipment |
| /api/dynamic/api/ProcedureMaterial/* | ProcedureMaterial |
| /api/dynamic/api/MaterialCategoryManagement/* | MaterialCategory |
| /api/dynamic/api/MaterialBOMManagement/* | MaterialBOM |
| /api/dynamic/api/MaterialVersionManagement/* | MaterialVersion |
| /api/dynamic/api/User_Infor/* | User_Infor（用户信息） |
| /api/user/login | 登录（查询 User_Infor 验证） |
| /api/dynamic/api/Feedback/* | Feedback（反馈） |

---

## 六、Feedback（反馈箱）

| 属性名 | 类型 | 说明 |
|--------|------|------|
| rating | Integer | 星级评价（1-5） |
| content | String | 文字意见 |

---

## 七、User_Infor（用户信息）

用于用户注册、登录及个人信息管理。

| 属性名 | 类型 | 说明 |
|--------|------|------|
| username | String | 用户名（唯一） |
| password | String | 密码 |
| gender | String | 性别（男/女/其他） |
| occupation | String | 身份职业 |
| department | String | 所在部门 |
| responsible_business | String | 负责业务 |
