<template>
  <section class="mg-wrapper">
    <!-- 标题区 -->
    <div class="mg-header">
      <div class="mg-icon-badge">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
          stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="3" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <polyline points="21 15 16 10 5 21" />
        </svg>
      </div>
      <div>
        <h2 class="mg-title">智能物料图片生成</h2>
        <p class="mg-subtitle">输入关键词，自动抓取 · 校验 · AI 生成</p>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="mg-input-row">
      <div class="mg-input-wrap">
        <svg class="mg-input-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
          fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          id="mg-keyword-input"
          v-model="keyword"
          class="mg-input"
          type="text"
          placeholder="输入图片关键词，例如：液压泵"
          maxlength="50"
          :disabled="loading"
          @keydown.enter="handleGenerate"
        />
        <span v-if="keyword" class="mg-input-clear" @click="keyword = ''">×</span>
      </div>

      <button
        id="mg-generate-btn"
        class="mg-btn"
        :class="{ 'mg-btn--loading': loading }"
        :disabled="loading || !keyword.trim()"
        @click="handleGenerate"
      >
        <span v-if="!loading" class="mg-btn-text">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <polygon points="5 3 19 12 5 21 5 3" />
          </svg>
          生 成
        </span>
        <span v-else class="mg-btn-text">
          <span class="mg-spinner"></span>
          生成中…
        </span>
      </button>
    </div>

    <!-- 错误提示 -->
    <transition name="mg-fade">
      <div v-if="errorMsg" class="mg-error" role="alert">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
          stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        {{ errorMsg }}
      </div>
    </transition>

    <!-- 图片结果区 -->
    <transition name="mg-slide-up">
      <div v-if="imageUrl || loading" class="mg-result-card">
        <!-- 骨架屏 Loading -->
        <div v-if="loading && !imageUrl" class="mg-skeleton">
          <div class="mg-skeleton-shimmer"></div>
          <p class="mg-loading-hint">
            <span class="mg-dot-pulse"></span>
            正在抓取网络图片并进行 AI 质量鉴定，最长约 15 秒…
          </p>
        </div>

        <!-- 图片展示 -->
        <div v-if="imageUrl" class="mg-img-container">
          <img
            :src="imageUrl"
            :alt="`${lastKeyword} 物料图`"
            class="mg-img"
            @load="onImgLoad"
            @error="onImgError"
          />
          <transition name="mg-fade">
            <div v-if="imgLoaded" class="mg-img-overlay">
              <span class="mg-tag">{{ lastKeyword }}</span>
              <a :href="imageUrl" :download="`${lastKeyword}.jpg`" class="mg-download-btn">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
                  stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="7 10 12 15 17 10" />
                  <line x1="12" y1="15" x2="12" y2="3" />
                </svg>
                下载
              </a>
            </div>
          </transition>
        </div>

        <!-- 元信息 -->
        <div v-if="imageUrl && imgLoaded" class="mg-meta">
          <span>✅ 生成成功</span>
          <span class="mg-meta-sep">·</span>
          <span>{{ lastKeyword }}</span>
          <span class="mg-meta-sep">·</span>
          <span>{{ elapsedTime }}s</span>
        </div>
      </div>
    </transition>

    <!-- 使用提示 -->
    <p class="mg-tips">
      💡 图片来自网络抓取 + AI 生成双通道，结果会带有随机因子，每次可能不同。
    </p>
  </section>
</template>

<script setup>
import { ref } from 'vue'

// ── 配置：后端 API 基础地址 ──────────────────────
// 开发时后端运行在 8000 端口，生产部署后改为实际域名
const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8000'
// ────────────────────────────────────────────────

const keyword    = ref('')
const loading    = ref(false)
const errorMsg   = ref('')
const imageUrl   = ref('')
const imgLoaded  = ref(false)
const lastKeyword = ref('')
const elapsedTime = ref(0)

let startTs = 0

async function handleGenerate() {
  const kw = keyword.value.trim()
  if (!kw || loading.value) return

  // 重置状态
  loading.value   = true
  errorMsg.value  = ''
  imageUrl.value  = ''
  imgLoaded.value = false
  lastKeyword.value = kw
  startTs = Date.now()

  try {
    // 注意：本接口最长可能耗时 15+ 秒，timeout 设为 30s
    const controller = new AbortController()
    const tid = setTimeout(() => controller.abort(), 30_000)

    const res = await fetch(`${API_BASE}/api/generate-image`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ keyword: kw }),
      signal: controller.signal,
    })
    clearTimeout(tid)

    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.detail ?? `服务器错误 (${res.status})`)
    }

    const data = await res.json()
    // 将相对路径拼接为完整 URL
    imageUrl.value = `${API_BASE}${data.image_url}`
    elapsedTime.value = ((Date.now() - startTs) / 1000).toFixed(1)

  } catch (e) {
    if (e.name === 'AbortError') {
      errorMsg.value = '请求超时（超过 30 秒），请稍后重试'
    } else {
      errorMsg.value = e.message || '未知错误，请检查后端服务是否启动'
    }
  } finally {
    loading.value = false
  }
}

function onImgLoad() {
  imgLoaded.value = true
}

function onImgError() {
  errorMsg.value = '图片加载失败，URL 可能已失效'
  imageUrl.value = ''
}
</script>

<style scoped>
/* ═══════════════════════════════════════════════════
   CSS 变量回退链：优先使用全局主题变量，否则回退到半透明值
   确保在深/浅色模式自动切换时均能正常显示
═══════════════════════════════════════════════════ */
.mg-wrapper {
  --_bg:      var(--color-background, rgba(255,255,255,0.55));
  --_border:  var(--color-border, rgba(180,180,200,0.35));
  --_text:    var(--color-text, #2c2c3a);
  --_sub:     var(--color-text-secondary, rgba(90,90,120,0.75));
  --_accent:  var(--color-accent, #7c6af7);
  --_accent2: var(--color-accent-secondary, #f76a9f);
  --_radius:  16px;
  --_shadow:  0 8px 32px rgba(80,60,180,0.12);
  --_glass-bg: rgba(255,255,255,0.55);
  --_glass-border: rgba(255,255,255,0.7);

  position: relative;
  max-width: 680px;
  margin: 0 auto;
  padding: 32px 28px 24px;
  border-radius: var(--_radius);
  background: var(--_glass-bg);
  border: 1px solid var(--_glass-border);
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  box-shadow: var(--_shadow);
  font-family: var(--font-family, 'Inter', 'PingFang SC', system-ui, sans-serif);
  color: var(--_text);
}

/* ── 标题区 ─────────────────────────────────── */
.mg-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.mg-icon-badge {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--_accent), var(--_accent2));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(124,106,247,0.35);
  color: #fff;
}

.mg-icon-badge svg { width: 22px; height: 22px; }

.mg-title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
  letter-spacing: 0.01em;
  background: linear-gradient(90deg, var(--_accent), var(--_accent2));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.mg-subtitle {
  margin: 4px 0 0;
  font-size: 0.78rem;
  color: var(--_sub);
}

/* ── 输入行 ─────────────────────────────────── */
.mg-input-row {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.mg-input-wrap {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
}

.mg-input-icon {
  position: absolute;
  left: 12px;
  width: 16px;
  height: 16px;
  color: var(--_sub);
  pointer-events: none;
  flex-shrink: 0;
}

.mg-input {
  width: 100%;
  padding: 11px 36px 11px 38px;
  border-radius: 10px;
  border: 1px solid var(--_border);
  background: rgba(255,255,255,0.45);
  color: var(--_text);
  font-size: 0.92rem;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  backdrop-filter: blur(6px);
}
.mg-input::placeholder { color: var(--_sub); }
.mg-input:focus {
  border-color: var(--_accent);
  box-shadow: 0 0 0 3px rgba(124,106,247,0.15);
}
.mg-input:disabled { opacity: 0.6; cursor: not-allowed; }

.mg-input-clear {
  position: absolute;
  right: 10px;
  color: var(--_sub);
  cursor: pointer;
  font-size: 1.1rem;
  line-height: 1;
  user-select: none;
  padding: 2px;
  transition: color 0.15s;
}
.mg-input-clear:hover { color: var(--_text); }

/* ── 按钮 ─────────────────────────────────── */
.mg-btn {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 11px 22px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, var(--_accent), var(--_accent2));
  color: #fff;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.2s, transform 0.15s, box-shadow 0.2s;
  box-shadow: 0 4px 14px rgba(124,106,247,0.30);
}
.mg-btn:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(124,106,247,0.40);
}
.mg-btn:active:not(:disabled) { transform: translateY(0); }
.mg-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }

.mg-btn-text {
  display: flex;
  align-items: center;
  gap: 6px;
}
.mg-btn-text svg { width: 14px; height: 14px; }

/* 旋转加载圆圈 */
.mg-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: mg-spin 0.7s linear infinite;
}
@keyframes mg-spin { to { transform: rotate(360deg); } }

/* ── 错误提示 ─────────────────────────────── */
.mg-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 8px;
  background: rgba(255, 80, 80, 0.10);
  border: 1px solid rgba(255, 80, 80, 0.25);
  color: #d93030;
  font-size: 0.85rem;
  margin-bottom: 12px;
}
.mg-error svg { width: 16px; height: 16px; flex-shrink: 0; }

/* ── 结果卡片 ─────────────────────────────── */
.mg-result-card {
  margin-top: 18px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--_border);
  background: rgba(255,255,255,0.38);
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 20px rgba(80,60,180,0.08);
}

/* 骨架屏 */
.mg-skeleton {
  height: 320px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(110deg,
    rgba(200,200,220,0.18) 0%,
    rgba(200,200,220,0.12) 50%,
    rgba(200,200,220,0.18) 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}
.mg-skeleton-shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(255,255,255,0.35) 50%,
    transparent 100%);
  animation: mg-shimmer 1.4s infinite;
}
@keyframes mg-shimmer {
  0%   { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}
.mg-loading-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.82rem;
  color: var(--_sub);
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 280px;
  line-height: 1.5;
}

/* 三点脉冲 */
.mg-dot-pulse {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--_accent);
  animation: mg-pulse 1.2s ease-in-out infinite;
  flex-shrink: 0;
}
@keyframes mg-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.5); opacity: 0.5; }
}

/* 图片容器 */
.mg-img-container {
  position: relative;
  overflow: hidden;
  max-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(240,240,255,0.15);
}
.mg-img {
  width: 100%;
  max-height: 420px;
  object-fit: contain;
  display: block;
  transition: transform 0.4s ease;
}
.mg-img-container:hover .mg-img { transform: scale(1.015); }

/* 悬停遮罩 */
.mg-img-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px;
  background: linear-gradient(to top, rgba(40,30,80,0.55), transparent);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mg-tag {
  font-size: 0.8rem;
  font-weight: 600;
  color: rgba(255,255,255,0.9);
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.25);
  padding: 3px 10px;
  border-radius: 6px;
  backdrop-filter: blur(4px);
}
.mg-download-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: 7px;
  background: rgba(255,255,255,0.18);
  border: 1px solid rgba(255,255,255,0.3);
  color: #fff;
  font-size: 0.8rem;
  text-decoration: none;
  transition: background 0.2s;
  backdrop-filter: blur(4px);
}
.mg-download-btn:hover { background: rgba(255,255,255,0.30); }
.mg-download-btn svg { width: 13px; height: 13px; }

/* 元信息行 */
.mg-meta {
  padding: 8px 16px;
  font-size: 0.78rem;
  color: var(--_sub);
  display: flex;
  align-items: center;
  gap: 4px;
  border-top: 1px solid var(--_border);
}
.mg-meta-sep { opacity: 0.4; }

/* ── 底部提示 ─────────────────────────────── */
.mg-tips {
  margin: 16px 0 0;
  font-size: 0.75rem;
  color: var(--_sub);
  line-height: 1.5;
}

/* ── 过渡动画 ─────────────────────────────── */
.mg-fade-enter-active, .mg-fade-leave-active { transition: opacity 0.3s; }
.mg-fade-enter-from, .mg-fade-leave-to { opacity: 0; }

.mg-slide-up-enter-active { transition: opacity 0.4s ease, transform 0.4s ease; }
.mg-slide-up-leave-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.mg-slide-up-enter-from   { opacity: 0; transform: translateY(12px); }
.mg-slide-up-leave-to     { opacity: 0; transform: translateY(-6px); }

/* ── 响应式 ─────────────────────────────── */
@media (max-width: 520px) {
  .mg-wrapper { padding: 20px 14px 18px; }
  .mg-input-row { flex-direction: column; }
  .mg-btn { justify-content: center; }
}
</style>
