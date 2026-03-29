/**
 * GitHub Pages 纯静态演示：拦截 /api 请求并返回本地快照数据（无 Spring Boot）。
 * 由 VITE_STATIC_DEMO=true 启用，见 docs/deployment/github-pages-static-demo.md
 */
import axios from 'axios'
import * as D from './staticDemoData'

function parseBody(config) {
  const raw = config.data
  if (raw == null || raw === '') return {}
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw)
    } catch {
      return {}
    }
  }
  if (typeof FormData !== 'undefined' && raw instanceof FormData) return { __formData: true }
  return raw
}

/** 合并 baseURL + url，去掉 /EGS 前缀，得到 /api/... */
export function normalizeApiPath(config) {
  const base = (config.baseURL || '').replace(/\/$/, '')
  let u = (config.url || '').split('?')[0]
  if (u.startsWith('http')) {
    try {
      return new URL(u).pathname
    } catch {
      return u
    }
  }
  const combined = (base + (u.startsWith('/') ? u : '/' + u)).replace(/\/+/g, '/')
  return combined.replace(/^\/EGS(?=\/)/i, '') || u
}

function wrapRows(rows) {
  return { result: 'SUCCESS', data: { rows: rows.map((r) => ({ ...r })) } }
}

function wrapSuccessParams(id) {
  return { result: 'SUCCESS', params: { id } }
}

let idSeq = 2000
function nextId(prefix) {
  return `${prefix}_${++idSeq}`
}

let equipmentRows = D.SEED_EQUIPMENT.map((r) => ({ ...r }))
let materialRows = D.SEED_MATERIAL.map((r) => ({ ...r }))
let procedureRows = D.SEED_PROCEDURE.map((r) => ({ ...r }))
let routeRows = D.SEED_ROUTE.map((r) => ({ ...r }))
let routeProcRows = D.SEED_ROUTE_PROC.map((r) => ({ ...r }))
let procEquipRows = D.SEED_PROC_EQUIP.map((r) => ({ ...r }))
let procMatRows = D.SEED_PROC_MAT.map((r) => ({ ...r }))
let userRows = D.SEED_USER.map((r) => ({ ...r }))

function rowsForEntity(entity) {
  switch (entity) {
    case 'EquipmentManagement':
      return equipmentRows
    case 'MaterialManagement':
      return materialRows
    case 'ProcedureManagement':
      return procedureRows
    case 'ProcessRoute':
      return routeRows
    case 'ProcessRouteProcedure':
      return routeProcRows
    case 'ProcedureEquipment':
      return procEquipRows
    case 'ProcedureMaterial':
      return procMatRows
    case 'User_Infor':
      return userRows
    default:
      return null
  }
}

function handleDynamicList(entity) {
  const store = rowsForEntity(entity)
  if (store) return wrapRows(store)
  return wrapRows([])
}

function handleDynamicCreate(entity, body) {
  const store = rowsForEntity(entity)
  const params = body && body.params ? body.params : {}
  if (!store) {
    return wrapSuccessParams(nextId('mock'))
  }
  const id = nextId('mock')
  const row = { ...params, id }
  if (entity === 'EquipmentManagement' && !row.equipment_code) row.equipment_code = 'EQ-' + idSeq
  if (entity === 'MaterialManagement' && !row.material_code) row.material_code = 'MAT-' + idSeq
  if (entity === 'ProcedureManagement' && !row.procedure_code) row.procedure_code = 'PROC-' + idSeq
  if (entity === 'ProcessRoute' && !row.route_code) row.route_code = 'ROUTE-' + idSeq
  store.push(row)
  return wrapSuccessParams(id)
}

function handleDynamicDelete(entity, id) {
  const store = rowsForEntity(entity)
  if (store) {
    const idx = store.findIndex((r) => String(r.id) === String(id))
    if (idx >= 0) store.splice(idx, 1)
  }
  return { result: 'SUCCESS', success: true, message: '删除成功' }
}

function handleLogin(body) {
  const username = body && body.username != null ? String(body.username).trim() : ''
  const password = body && body.password != null ? String(body.password) : ''
  if (!username || !password) {
    return { success: false, message: '用户名和密码不能为空' }
  }
  const u = userRows.find((x) => (x.user_name || x.username) === username)
  if (u) {
    const pwd = u.password || ''
    if (!pwd || pwd === password) {
      const safe = { ...u }
      delete safe.password
      return { success: true, user: safe }
    }
  }
  if (username === 'admin' && password === 'admin123') {
    return {
      success: true,
      user: { id: 'mock_user_1', user_name: 'admin', username: 'admin' }
    }
  }
  return { success: false, message: '用户名或密码错误' }
}

function scheduleOptimizeStub() {
  return {
    success: true,
    algorithm: '蚁群算法 ACO（静态演示）',
    vehicleType: 'TRUCK',
    bestRouteOrder: ['仓库', '客户1', '客户2', '客户3', '仓库'],
    legs: [
      { from: '仓库', to: '客户1', distanceKm: 12, restrictedEdge: false },
      { from: '客户1', to: '客户2', distanceKm: 10, restrictedEdge: false }
    ],
    totalDistanceKm: 45.2,
    estimatedCost: 128.5,
    constraints: { trafficFactor: '演示数据', truckRestrictionsApplied: true },
    rlNote: '静态站不调用后端计算。'
  }
}

function wmsHistory() {
  return {
    success: true,
    lines: [
      { sku: 'SKU-A100', name: '耳机单元', orderLines: 420 },
      { sku: 'SKU-B220', name: '充电宝电芯', orderLines: 380 }
    ]
  }
}

function wmsSlotOpt() {
  return {
    success: true,
    baselinePathIndex: 1200,
    optimizedPathIndex: 780,
    improvementPercent: 35,
    recommendations: [
      {
        sku: 'SKU-A100',
        skuName: '耳机单元',
        turnoverIndex: 420,
        recommendedSlot: 'L-01-01',
        distanceToOutboundM: 4.8,
        algorithm: '贪心匹配（演示）'
      }
    ]
  }
}

function defaultFallback(path, method) {
  if (path.includes('/dynamic/api/')) {
    if (method === 'delete') return { result: 'SUCCESS', success: true }
    if (method === 'post' && path.endsWith('/list')) return wrapRows([])
    if (method === 'post') return wrapSuccessParams(nextId('mock'))
  }
  return {
    success: true,
    demo: true,
    message: '静态演示：该接口无单独快照，未连接后端。',
    path,
    method
  }
}

function resolveStaticMock(config) {
  const method = (config.method || 'get').toLowerCase()
  const path = normalizeApiPath(config)
  const body = parseBody(config)

  if (method === 'post' && path === '/api/user/login') {
    return handleLogin(body)
  }

  if (method === 'post' && path === '/api/product-report') {
    return D.buildProductReport(body && body.product)
  }

  if (method === 'post' && path === '/api/product-context-demo') {
    return { success: true, summary: '静态演示：产品上下文分析占位。', demo: true }
  }

  if (method === 'post' && path === '/api/production-plan/optimal') {
    const pn = body && body.product_name != null ? String(body.product_name).trim() : '产品'
    const qty = body && body.quantity != null ? Number(body.quantity) : 1000
    return D.buildFallbackOptimalPlan(pn, qty)
  }

  if (method === 'post' && path === '/api/production-plan/export-excel') {
    const text = '静态演示站：请本地运行 Java 代理与 business-analysis 以生成真实 Excel。'
    return new Blob([text], { type: 'text/plain;charset=utf-8' })
  }

  if (method === 'post' && path === '/api/production-plan/export-pdf') {
    const text = '静态演示站：PDF 导出请使用本地完整环境。'
    return new Blob([text], { type: 'text/plain;charset=utf-8' })
  }

  if (method === 'post' && path === '/api/export/models-to-disk') {
    return { success: true, message: '静态演示：未写入磁盘。', path: '(demo)' }
  }

  if (method === 'post' && path === '/api/llm/product-query') {
    const product = body && body.product != null ? String(body.product).trim() : ''
    return {
      success: true,
      content: D.cannedProductMarkdown(product),
      product
    }
  }

  if (method === 'get' && path.startsWith('/api/llm/product-video')) {
    return {
      success: true,
      videos: [{ title: '制造工艺（演示）', bvid: 'BV1B4411h7xN', author: '演示' }],
      product: 'demo'
    }
  }

  if (method === 'get' && path.startsWith('/api/llm/product-companies')) {
    return { success: true, companies: D.DEMO_COMPANIES, product: 'demo' }
  }

  if (method === 'get' && path.startsWith('/api/llm/product-components')) {
    return { success: true, components: D.DEMO_COMPONENTS, product: 'demo' }
  }

  if (method === 'post' && path === '/api/llm/chat') {
    const msg = body && body.message != null ? String(body.message) : ''
    const explain = body && body.explainTerm === true
    return {
      success: true,
      content: explain
        ? '「' + msg + '」：专业名词的通俗解释（静态演示）。'
        : '你好！我是丝路智联小助手（静态演示模式）。配置千问 API 后可在本地体验完整对话。'
    }
  }

  if (method === 'post' && path === '/api/rag/chat') {
    return {
      success: true,
      answer:
        '（静态演示）未连接向量库与千问。你可以在本地启动 proxy 与 RAG 服务后使用完整检索增强问答。',
      retrievedPreview: ''
    }
  }

  if (method === 'get' && path === '/api/rag/status') {
    return { ok: true, knowledgeItems: 0, mode: 'static-demo', hint: 'GitHub Pages 纯静态' }
  }

  if (method === 'post' && path === '/api/tms-smart/delay-insight') {
    return {
      success: true,
      usedLlm: false,
      message: '静态演示：未调用千问。',
      explanation:
        '根据演示运单上下文，延误风险主要与峰谷时段路况与在途节点有关（规则合成说明）。',
      context: { tripId: (body && body.tripId) || 'T-20250316-01', demo: true }
    }
  }

  if (method === 'post' && path === '/api/tms-smart/trace-anchor') {
    return {
      success: true,
      tripId: (body && body.tripId) || 'T-20250316-01',
      anchor: 'sha256:demo_static_' + String(Date.now()),
      demo: true
    }
  }

  if (method === 'get' && path.startsWith('/api/tms-smart/schedule-optimize')) {
    return scheduleOptimizeStub()
  }

  if (method === 'get' && path.startsWith('/api/tms-smart/in-transit')) {
    return {
      success: true,
      vehicles: [
        {
          tripId: 'T-20250316-01',
          plateNo: '沪A·D12345',
          status: '在途',
          speedKmh: 72,
          demo: true
        }
      ]
    }
  }

  if (method === 'get' && path.startsWith('/api/tms-smart/eta-predictions')) {
    return {
      success: true,
      trips: [
        {
          tripId: 'T-20250316-01',
          originName: 'DC-华东仓',
          destName: '客户C-苏州',
          etaMin: 45,
          demo: true
        }
      ],
      summary: { totalTrips: 1, onTimeRatio: 0.9 },
      warnings: []
    }
  }

  if (method === 'get' && path.startsWith('/api/tms-smart/eta-ml-predict')) {
    return {
      success: true,
      predictions: [{ tripId: 'T-20250316-01', etaMin: 42, model: 'demo' }],
      deployment: null,
      dataDisclaimer: '静态演示：未接高德驾车与 PAI-EAS',
      maeTargetMinutes: 15,
      warnings: ['静态演示环境']
    }
  }

  if (method === 'get' && path.startsWith('/api/weather/open-meteo/current')) {
    return {
      success: true,
      temperature: 18.5,
      windSpeed: 3.2,
      weatherCode: 1,
      summary: '多云（演示）'
    }
  }

  if (method === 'get' && path.startsWith('/api/wms-smart/history-orders')) {
    return wmsHistory()
  }

  if (method === 'get' && path.startsWith('/api/wms-smart/slot-optimization')) {
    return wmsSlotOpt()
  }

  if (method === 'post' && path.startsWith('/api/wms-smart/apply-slot-plan')) {
    return { success: true, applied: true, demo: true }
  }

  if (method === 'get' && path.startsWith('/api/wms-smart/goods-to-person')) {
    return { success: true, tasks: [{ id: 'GT-001', sku: 'SKU-A100', status: 'queued' }] }
  }

  if (method === 'post' && path.startsWith('/api/wms-smart/dispatch-task')) {
    return { success: true, taskId: 'TASK-DEMO-1' }
  }

  if (method === 'get' && path.startsWith('/api/wms-smart/digital-twin')) {
    return { success: true, snapshot: { agv: 2, shuttle: 2, occupancy: 0.72 } }
  }

  if (method === 'get' && path === '/api/map/config') {
    return { amapConfigured: false }
  }

  if (method === 'get' && path.startsWith('/api/map/driving')) {
    return {
      success: true,
      distance: 12000,
      duration: 900,
      path: [
        [121.4, 31.2],
        [121.45, 31.25],
        [121.5, 31.3]
      ],
      demo: true
    }
  }

  if (method === 'get' && path.startsWith('/api/pet-rail/stations')) {
    return {
      success: true,
      stations: [
        '北京', '上海', '广州', '深圳', '杭州', '南京', '成都', '重庆', '武汉', '西安',
        '苏州', '天津', '郑州', '长沙', '济南', '青岛', '沈阳', '福州', '厦门', '昆明',
        '哈尔滨', '大连'
      ]
    }
  }

  if (method === 'get' && path.startsWith('/api/pet-rail/query')) {
    const p = config.params || {}
    const fromStation = (p.fromStation || '').trim() || '北京'
    const toStation = (p.toStation || '').trim() || '上海'
    const date = p.date || '2026-01-01'
    const petWeightKg = p.petWeightKg != null ? Number(petWeightKg) : 5
    return {
      success: true,
      fromStation,
      toStation,
      date,
      distanceKm: 1068,
      petWeightKg,
      disclaimer:
        '本结果为静态演示估算，非 12306 官方实时数据。票价、余票与宠物托运以车站规定为准。',
      reference: '12306.cn · 中国铁路客户服务中心',
      trains: [
        {
          trainNo: 'G1',
          trainType: '高铁',
          departTime: '08:00',
          arriveTime: '13:28',
          durationText: '约5小时28分',
          secondClassYuan: 553,
          petConsignmentYuan: 0,
          totalEstimateYuan: 553,
          petPolicyNote: '高铁一般不办理宠物托运；以下为出行参考（演示）'
        },
        {
          trainNo: 'D7',
          trainType: '动车',
          departTime: '09:12',
          arriveTime: '15:40',
          durationText: '约6小时28分',
          secondClassYuan: 412,
          petConsignmentYuan: 0,
          totalEstimateYuan: 412,
          petPolicyNote: '动车多数不办理宠物托运（演示）'
        }
      ]
    }
  }

  if (method === 'get' && path.startsWith('/api/warehouse-dispatch/records')) {
    return { success: true, records: [{ id: 'W1', sku: 'SKU-A100', qty: 10 }] }
  }

  if (method === 'post' && path.startsWith('/api/warehouse-dispatch/outbound')) {
    return { success: true, orderId: 'OUT-DEMO-1' }
  }

  if (method === 'get' && path.startsWith('/api/warehouse-dispatch/optimize')) {
    return { success: true, score: 0.88, demo: true }
  }

  if (method === 'get' && path.startsWith('/api/warehouse-dispatch/network')) {
    return { success: true, nodes: 5, edges: 8 }
  }

  if (method === 'get' && path.startsWith('/api/warehouse-dispatch/stress-summary')) {
    return { success: true, utilization: 0.71 }
  }

  if (method === 'post' && path.startsWith('/api/ai-chain/anchor-waybill')) {
    return { success: true, txHash: '0x' + 'demo'.repeat(8) }
  }

  if (method === 'get' && path.startsWith('/api/ai-chain/anchors')) {
    return { success: true, items: [{ id: 'a1', hash: '0xabc' }] }
  }

  if (method === 'post' && path.startsWith('/api/ai-chain/sla-evaluate')) {
    return { success: true, score: 92, demo: true }
  }

  if (method === 'get' && path.startsWith('/api/ai-chain/sla-events')) {
    return { success: true, events: [{ type: 'ONTIME', at: new Date().toISOString() }] }
  }

  if (method === 'post' && path.startsWith('/api/cv/inspect')) {
    return { success: true, label: 'OK', confidence: 0.95, demo: true }
  }

  if (method === 'post' && path.startsWith('/api/cv/sync-defects')) {
    return { success: true, synced: 0 }
  }

  if (method === 'post' && path.startsWith('/api/logistics-multimodal/parse-image')) {
    return { success: true, text: '演示：多模态解析占位', demo: true }
  }

  if (method === 'post' && path.startsWith('/api/logistics-multimodal/parse-text')) {
    return { success: true, structured: { intent: 'demo' } }
  }

  if (method === 'post' && path.startsWith('/api/logistics-multimodal/commit')) {
    return { success: true, id: 'MM-DEMO-1' }
  }

  if (method === 'get' && path.startsWith('/api/logistics-multimodal/recent')) {
    return { success: true, items: [] }
  }

  const dyn = /^\/api\/dynamic\/api\/([A-Za-z_]+)\/(list|create|update|delete)(?:\/([^/?]+))?$/.exec(path)
  if (dyn && method === 'post' && dyn[2] === 'list') {
    return handleDynamicList(dyn[1])
  }
  if (dyn && method === 'post' && dyn[2] === 'create') {
    return handleDynamicCreate(dyn[1], body)
  }
  if (dyn && method === 'post' && dyn[2] === 'update') {
    return { result: 'SUCCESS' }
  }
  if (dyn && method === 'delete' && dyn[2] === 'delete' && dyn[3]) {
    return handleDynamicDelete(dyn[1], dyn[3])
  }

  return defaultFallback(path, method)
}

function makeAxiosResponse(config, data) {
  const isBlob = typeof Blob !== 'undefined' && data instanceof Blob
  return {
    data,
    status: 200,
    statusText: 'OK',
    headers: isBlob ? { 'content-type': data.type || 'application/octet-stream' } : {},
    config,
    request: {}
  }
}

export function installStaticDemoMock() {
  if (import.meta.env.VITE_STATIC_DEMO !== 'true') return

  axios.interceptors.request.use((config) => {
    if (!config.url || (!config.url.startsWith('/api') && !config.url.includes('/api/'))) {
      return config
    }
    const data = resolveStaticMock(config)
    config.adapter = () => Promise.resolve(makeAxiosResponse(config, data))
    return config
  })
}
