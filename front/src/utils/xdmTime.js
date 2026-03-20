/**
 * xDM-F 要求 createTime/lastUpdateTime 格式为 "yyyy-MM-dd HH:mm:ss"
 * ISO 格式 (2026-03-08T14:20:07.123Z) 会导致 IIT.01411000 参数校验失败
 */
export function formatXdmTime(date) {
  const d = date || new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
