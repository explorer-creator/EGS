/**
 * 统一将接口错误转为用户友好文案（避免出现 403/Forbidden/鉴权 等字样）。
 */
export const FRIENDLY_UNAVAILABLE = '服务暂不可用，请稍后重试'

export function sanitizeApiError(err) {
  if (!err || !err.response) return err
  const st = err.response.status
  if (st === 403 || st === 401) {
    const d = err.response.data
    if (d && typeof d === 'object') {
      d.message = FRIENDLY_UNAVAILABLE
    } else {
      err.response.data = { message: FRIENDLY_UNAVAILABLE, result: 'FAIL' }
    }
  }
  return err
}

/** 从错误对象取展示用文案（不含 403/鉴权/forbidden 等） */
export function friendlyErrorMessage(err, fallback = FRIENDLY_UNAVAILABLE) {
  if (!err || !err.response) return err?.message || fallback
  const st = err.response.status
  if (st === 403 || st === 401) return FRIENDLY_UNAVAILABLE
  const d = err.response.data
  if (typeof d === 'string') {
    const s = d.toLowerCase()
    if (s.includes('forbidden') || /\b403\b/.test(d)) return FRIENDLY_UNAVAILABLE
    return d.length > 200 ? fallback : d
  }
  if (d && d.message) {
    const m = String(d.message)
    if (/403|forbidden|鉴权失败/i.test(m)) return FRIENDLY_UNAVAILABLE
    return m
  }
  return err.response.statusText || fallback
}
