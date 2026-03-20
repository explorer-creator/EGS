/**
 * 解析 CSV 文件为对象数组
 * @param {File} file - CSV 文件
 * @param {Array} columns - 列配置 [{ label, field }]，用于将表头映射到字段名
 * @returns {Promise<Array<Object>>} 解析后的数据行
 */
export function parseCsvFile(file, columns) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const text = (e.target?.result || '').toString()
        const hasBom = text.charCodeAt(0) === 0xfeff
        const content = hasBom ? text.slice(1) : text
        const lines = content.split(/\r\n|\r|\n/).filter(Boolean)
        if (lines.length < 2) {
          resolve([])
          return
        }
        const headers = parseCsvLine(lines[0])
        const labelToField = {}
        if (Array.isArray(columns) && columns.length > 0) {
          columns.forEach((c, i) => {
            labelToField[c.label] = c.field
          })
        } else {
          headers.forEach((h, i) => { labelToField[h] = h })
        }
        const result = []
        for (let i = 1; i < lines.length; i++) {
          const cells = parseCsvLine(lines[i])
          const row = {}
          headers.forEach((h, j) => {
            const field = labelToField[h] || h
            row[field] = cells[j] != null ? String(cells[j]).trim() : ''
          })
          result.push(row)
        }
        resolve(result)
      } catch (err) {
        reject(err)
      }
    }
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsText(file, 'UTF-8')
  })
}

function parseCsvLine(line) {
  const result = []
  let i = 0
  while (i < line.length) {
    if (line[i] === '"') {
      let val = ''
      i++
      while (i < line.length) {
        if (line[i] === '"') {
          if (line[i + 1] === '"') {
            val += '"'
            i += 2
          } else {
            i++
            break
          }
        } else {
          val += line[i]
          i++
        }
      }
      result.push(val)
    } else {
      const start = i
      while (i < line.length && line[i] !== ',') i++
      result.push(line.slice(start, i).replace(/^"|"$/g, '').replace(/""/g, '"'))
      if (line[i] === ',') i++
    }
  }
  return result
}
