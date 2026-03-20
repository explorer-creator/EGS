/**
 * 应用统一配置，支持环境变量覆盖，提升灵活性与可扩展性。
 * 开发环境：.env.development
 * 生产构建：.env.production 或 VITE_* 环境变量
 */
export const config = {
  /** 默认租户 ID（xDM-F 多租户场景可配置） */
  tenantId: import.meta.env.VITE_TENANT_ID || '3f5e31761ec249ea9dace6b96dd92e6a',
  /** API 基础路径（相对路径由 Vite 代理到后端） */
  apiBase: import.meta.env.VITE_API_BASE || '/api',
  /** 应用 ID（xDM-F 鉴权，可选覆盖） */
  applicationId: import.meta.env.VITE_APPLICATION_ID || '5aa040bab9484f2d8cb08125e85ab15b'
}
