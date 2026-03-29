/**
 * 静态演示站数据：与后端 XdmMockService.initDemoData() 及 ProductionPlanController.buildFallbackOptimalPlan 同源，
 * 便于「仅 GitHub Pages、无 Java」时展示你录入过的示例台账与方案。
 */

export const SEED_EQUIPMENT = [
  {
    id: 'mock_eq_1',
    equipment_code: 'EQ-001',
    equipment_name: 'SMT贴片机',
    manufacturer: '示例厂商',
    brand: '',
    model: 'SMT-100',
    location: '产线A'
  },
  {
    id: 'mock_eq_2',
    equipment_code: 'EQ-002',
    equipment_name: 'AOI检测机',
    manufacturer: '示例厂商',
    brand: '',
    model: 'AOI-200',
    location: '产线A'
  }
]

export const SEED_MATERIAL = [
  {
    id: 'mock_mat_1',
    material_code: 'MAT-001',
    material_name: 'PCB基板',
    model: '',
    stock_quantity: 100,
    supplier: ''
  },
  {
    id: 'mock_mat_2',
    material_code: 'MAT-002',
    material_name: '主控芯片',
    model: '',
    stock_quantity: 50,
    supplier: ''
  }
]

export const SEED_PROCEDURE = [
  {
    id: 'mock_proc_1',
    procedure_code: 'PROC-001',
    procedure_name: '贴片',
    production_steps: 'SMT贴片',
    production_inspection_equipment: 'AOI'
  },
  {
    id: 'mock_proc_2',
    procedure_code: 'PROC-002',
    procedure_name: '检测',
    production_steps: 'AOI检测',
    production_inspection_equipment: ''
  }
]

export const SEED_ROUTE = [
  {
    id: 'mock_route_1',
    route_code: 'ROUTE-001',
    route_name: '示例工艺路线',
    product: '示例产品',
    version: '1.0'
  }
]

/** 工艺路线-工序关联（与上面路线、工序 id 对应） */
export const SEED_ROUTE_PROC = [
  {
    id: 'mock_prp_1',
    route_id: 'mock_route_1',
    procedure_id: 'mock_proc_1',
    sequence: 1,
    route_code: 'ROUTE-001',
    procedure_code: 'PROC-001'
  }
]

export const SEED_PROC_EQUIP = []
export const SEED_PROC_MAT = []

export const SEED_USER = [
  {
    id: 'mock_user_1',
    user_name: 'admin',
    username: 'admin',
    password: 'admin123'
  }
]

/** 最优方案（与后端 fallback 结构一致） */
export function buildFallbackOptimalPlan(productName, quantity) {
  const product = productName || '产品'
  const qty = quantity || 1000
  return {
    success: true,
    product_name: product,
    quantity: qty,
    summary:
      '参考方案：涵盖 ' +
      product +
      ' 的完整生产流程，设备 2 台、物料 2 种、工序 2 道、工艺路线 1 条。（静态演示）',
    equipment: SEED_EQUIPMENT.map((e) => ({ ...e })),
    materials: SEED_MATERIAL.map((m) => ({
      material_code: m.material_code,
      material_name: m.material_name,
      model: m.model || '',
      quantity: 1,
      unit: m.material_name.includes('芯片') ? '颗' : '件',
      supplier: m.supplier || ''
    })),
    procedures: SEED_PROCEDURE.map((p) => ({
      procedure_code: p.procedure_code,
      procedure_name: p.procedure_name,
      production_steps: p.production_steps,
      production_inspection_equipment: p.production_inspection_equipment,
      operator: ''
    })),
    process_routes: [
      {
        route_code: 'ROUTE-001',
        route_name: product + '工艺路线',
        product,
        version: '1.0',
        description: '',
        procedure_sequence: ['PROC-001', 'PROC-002']
      }
    ],
    _fallback: true
  }
}

/** 产品报告（绿页用） */
export function buildProductReport(product) {
  const p = product || '产品'
  const base = new Date()
  const dates = []
  const carbon = []
  const pue = []
  for (let i = 29; i >= 0; i--) {
    const d = new Date(base)
    d.setDate(d.getDate() - i)
    dates.push(d.toISOString().slice(0, 10))
    carbon.push(Math.round(1200 - i * 12 + (i % 5) * 3))
    pue.push(+(1.8 - i * 0.015).toFixed(2))
  }
  return {
    success: true,
    green_summary:
      '针对「' +
      p +
      '」的绿色生产建议（静态演示）：优先使用谷电时段排产，监测 PUE 与碳排趋势，关注供应链碳强度与次品浪费折算。',
    green_dashboard: {
      dates,
      carbon_trend: carbon,
      pue_trend: pue,
      summary: {
        latest_co2_kg: carbon[carbon.length - 1],
        latest_pue: pue[pue.length - 1],
        reduction_pct: 15
      }
    }
  }
}

/** 产品问答 Markdown（无千问时） */
export function cannedProductMarkdown(product) {
  const p = product || '该产品'
  return (
    '### 「' +
    p +
    '」制造概览（静态演示）\n\n' +
    '1. **原材料**：与示例台账一致的 PCB 基板、主控芯片等。\n' +
    '2. **所用设备**：SMT 贴片机、AOI 检测机等。\n' +
    '3. **工序**：贴片 → 检测。\n' +
    '4. **工艺流程**：上板 → 贴片 → 回流 → AOI。\n' +
    '5. **物理原理**：锡膏熔化与润湿、光学成像对比度检测等。\n\n' +
    '完整大模型问答请在本地配置 `DASHSCOPE_API_KEY` 后运行主工程。'
  )
}

/** 节选企业（来自 product-companies.json 思路） */
export const DEMO_COMPANIES = [
  {
    name: '瑞声科技',
    intro: '全球领先的微型声学器件及精密制造方案提供商。',
    contact: '官网：https://www.aac.com',
    website: 'https://www.aac.com',
    tags: ['扬声器', '声学', '耳机']
  },
  {
    name: '国茂减速机',
    intro: '专业减速机研发制造企业，产品涵盖行星减速机等。',
    contact: '官网：https://www.guomaogroup.com',
    website: 'https://www.guomaogroup.com',
    tags: ['行星减速器', '齿轮']
  }
]

export const DEMO_COMPONENTS = [
  {
    name: '动圈扬声器',
    desc: '耳机核心发声单元',
    imageUrl: 'https://placehold.co/280x180/e8f4ff/165DFF?text=Speaker',
    tags: ['扬声器']
  },
  {
    name: '锂电池电芯',
    desc: '储能核心',
    imageUrl: 'https://placehold.co/280x180/e8f4ff/165DFF?text=Cell',
    tags: ['电芯']
  }
]
