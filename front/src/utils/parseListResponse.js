/**
 * 解析 xDM-F list 接口返回数据，兼容多种格式
 * @param {Object} res - axios 响应 res.data
 * @returns {Array} 数据数组
 */
export function parseListResponse(res) {
  if (!res) return []
  let data = res.data
  if (data == null) data = res
  if (Array.isArray(data)) return data
  // 顶层数组字段
  if (data && typeof data === 'object') {
    const arr = data.rows || data.list || data.data || data.records
    if (Array.isArray(arr)) return arr
    // 嵌套：data.data.rows、data.params.rows 等
    const inner = data.data || data.params
    if (inner && typeof inner === 'object') {
      const innerArr = inner.rows || inner.list || inner.data || inner.records
      if (Array.isArray(innerArr)) return innerArr
    }
  }
  return []
}
