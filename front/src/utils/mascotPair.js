/** 页面左右留白装饰图（public/page-mascots/） */
export const PAGE_MASCOT_FILES = ['1.png', '2.png', '3.png', '4.png', '5.png', '6.png']

/**
 * 按字符串键稳定选两张不同图（同键结果不变）。
 * @param {string} pageKey 如 currentPage 或 'LoginRegister:login'
 */
export function pageMascotPairForKey(pageKey) {
  const base = (import.meta.env.BASE_URL || '/').replace(/\/?$/, '/')
  const files = PAGE_MASCOT_FILES
  const n = files.length
  const page = String(pageKey || '')
  let h = 2166136261
  for (let i = 0; i < page.length; i++) {
    h ^= page.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  const u = h >>> 0
  let left = u % n
  let right = ((Math.imul(u, 1103515245) + 12345) >>> 0) % n
  if (right === left) right = (right + 1 + ((u >>> 16) % (n - 1 || 1))) % n
  return {
    left: base + 'page-mascots/' + files[left],
    right: base + 'page-mascots/' + files[right]
  }
}
