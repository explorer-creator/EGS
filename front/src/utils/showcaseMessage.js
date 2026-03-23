/**
 * 计算机设计大赛 / 展示视频：抑制「爆红」错误条，避免代理未启动等打断录制。
 * 真实接口成功时仍走正常 success/info；失败时静默或仅控制台记录。
 */
import { Message } from 'element-ui'

let installed = false

export function installShowcaseMessage() {
  if (installed) return
  installed = true

  const origError = Message.error
  Message.error = function (options) {
    const text =
      typeof options === 'string'
        ? options
        : options && (options.message || options.msg || '')
    if (text) {
      try {
        console.debug('[展示模式] 已抑制错误提示:', text)
      } catch (e) {
        /* ignore */
      }
    }
    return null
  }

  // 保留引用，便于需要时恢复
  Message._originalError = origError
}
