/**
 * 跨页面共享临时数据（新建失败时加入的 temp_ 项），供工序-设备/物料关联、工艺路线等下拉选择
 */
const KEY_EQUIPMENT = 'temp_equipment_list'
const KEY_MATERIAL = 'temp_material_list'
const KEY_PROCEDURE = 'temp_procedure_list'
const KEY_ROUTE = 'temp_route_list'

export function getTempEquipmentList() {
  try {
    const s = localStorage.getItem(KEY_EQUIPMENT)
    return s ? JSON.parse(s) : []
  } catch {
    return []
  }
}

export function saveTempEquipment(item) {
  if (!item || !String(item.id || '').startsWith('temp_')) return
  const list = getTempEquipmentList()
  if (list.some(x => x.id === item.id)) return
  list.unshift(item)
  try {
    localStorage.setItem(KEY_EQUIPMENT, JSON.stringify(list))
  } catch (_) {}
}

export function getTempMaterialList() {
  try {
    const s = localStorage.getItem(KEY_MATERIAL)
    return s ? JSON.parse(s) : []
  } catch {
    return []
  }
}

export function saveTempMaterial(item) {
  if (!item || !String(item.id || '').startsWith('temp_')) return
  const list = getTempMaterialList()
  if (list.some(x => x.id === item.id)) return
  list.unshift(item)
  try {
    localStorage.setItem(KEY_MATERIAL, JSON.stringify(list))
  } catch (_) {}
}

export function getTempProcedureList() {
  try {
    const s = localStorage.getItem(KEY_PROCEDURE)
    return s ? JSON.parse(s) : []
  } catch {
    return []
  }
}

export function saveTempProcedure(item) {
  if (!item || !String(item.id || '').startsWith('temp_')) return
  const list = getTempProcedureList()
  if (list.some(x => x.id === item.id)) return
  list.unshift(item)
  try {
    localStorage.setItem(KEY_PROCEDURE, JSON.stringify(list))
  } catch (_) {}
}

export function getTempRouteList() {
  try {
    const s = localStorage.getItem(KEY_ROUTE)
    return s ? JSON.parse(s) : []
  } catch {
    return []
  }
}

export function saveTempRoute(item) {
  if (!item || !String(item.id || '').startsWith('temp_')) return
  const list = getTempRouteList()
  if (list.some(x => x.id === item.id)) return
  list.unshift(item)
  try {
    localStorage.setItem(KEY_ROUTE, JSON.stringify(list))
  } catch (_) {}
}
