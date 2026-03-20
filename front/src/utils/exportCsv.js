/**
 * 导出列表数据为 CSV（Excel 可直接打开）
 * @param {Array} data - 数据数组
 * @param {Array} columns - 列配置 [{ label, field }] 或 [{ label, getter: (row) => value }]
 * @param {string} filename - 文件名（不含扩展名）
 */
export function exportToCsv(data, columns, filename = 'export') {
  if (!Array.isArray(data) || !Array.isArray(columns) || columns.length === 0) {
    return
  }
  const escape = (v) => {
    if (v == null) return ''
    const s = String(v)
    if (/[",\n\r]/.test(s)) return '"' + s.replace(/"/g, '""') + '"'
    return s
  }
  const header = columns.map((c) => escape(c.label)).join(',')
  const rows = data.map((row) =>
    columns.map((c) => {
      const v = c.getter ? c.getter(row) : (row[c.field] ?? row[c.field?.replace(/_([a-z])/g, (_, g) => g.toUpperCase())] ?? '')
      return escape(v)
    }).join(',')
  )
  const csv = [header, ...rows].join('\r\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename + '.csv'
  a.click()
  URL.revokeObjectURL(url)
}
