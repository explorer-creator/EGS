<template>
  <div class="mcr" :class="{ 'mcr--compact': compact }" role="region" :aria-label="ariaLabel">
    <div class="mcr-bg" aria-hidden="true" />
    <div class="mcr-inner">
      <p v-if="compact" class="mcr-compact-line">{{ compactLine }}</p>
      <div class="mcr-globe-wrap" aria-hidden="true">
        <svg class="mcr-globe" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="mcrGlobeG" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#38bdf8" />
              <stop offset="100%" stop-color="#6366f1" />
            </linearGradient>
          </defs>
          <circle cx="24" cy="24" r="20" fill="url(#mcrGlobeG)" opacity="0.35" />
          <path fill="none" stroke="rgba(255,255,255,0.85)" stroke-width="1.2" d="M8 24h32M24 8c-4 8-4 24 0 32M24 8c4 8 4 24 0 32" />
          <circle cx="24" cy="24" r="20" fill="none" stroke="rgba(255,255,255,0.5)" stroke-width="1.5" />
        </svg>
      </div>
      <!-- 抽象人物剪影：不同比例与肤色，象征多元团队（非写实肖像） -->
      <div class="mcr-people" aria-hidden="true">
        <svg class="mcr-svg" viewBox="0 0 220 72" xmlns="http://www.w3.org/2000/svg">
          <g v-for="(p, i) in silhouettes" :key="i" :transform="'translate(' + p.x + ',0)'">
            <ellipse :cx="p.cx" cy="14" rx="11" ry="12" :fill="p.skin" opacity="0.95" />
            <path
              :fill="p.cloth"
              d="M2 32 Q12 26 22 32 L26 68 L18 70 L14 44 L10 70 L2 68 Z"
              opacity="0.92"
            />
            <ellipse v-if="p.hat" :cx="p.cx" cy="6" rx="13" ry="4" :fill="p.hat" opacity="0.85" />
          </g>
        </svg>
      </div>
      <div class="mcr-flags" role="list">
        <span
          v-for="(f, i) in corridorFlags"
          :key="i"
          class="mcr-flag"
          role="listitem"
          :title="f.name + ' · ' + f.nameEn"
        >
          <span class="mcr-flag-emoji" aria-hidden="true">{{ f.emoji }}</span>
          <span class="mcr-flag-code">{{ f.code }}</span>
        </span>
      </div>
      <p v-if="caption" class="mcr-caption">{{ caption }}</p>
    </div>
  </div>
</template>

<script>
/**
 * 丝路/跨境主题：地球 + 多元人物剪影 + 中欧班列沿线国家标识
 * 采用抽象几何与旗帜符号，避免刻板形象。
 */
export default {
  name: 'MulticulturalRibbon',
  props: {
    compact: { type: Boolean, default: false },
    caption: { type: String, default: '' },
    compactLine: { type: String, default: '🌏 全球协作 · 多元文化 · 丝路沿线伙伴' },
    ariaLabel: { type: String, default: '全球协作与丝路沿线国家' }
  },
  data() {
    return {
      silhouettes: [
        { x: 0, cx: 14, skin: '#5c4033', cloth: '#1e3a5f', hat: null },
        { x: 44, cx: 14, skin: '#8d5524', cloth: '#7c2d12', hat: null },
        { x: 88, cx: 14, skin: '#c68642', cloth: '#14532d', hat: '#b45309' },
        { x: 132, cx: 14, skin: '#e0ac69', cloth: '#4c1d95', hat: null },
        { x: 176, cx: 14, skin: '#a1887f', cloth: '#0f766e', hat: null }
      ],
      corridorFlags: [
        { code: 'CN', emoji: '🇨🇳', name: '中国', nameEn: 'China' },
        { code: 'KZ', emoji: '🇰🇿', name: '哈萨克斯坦', nameEn: 'Kazakhstan' },
        { code: 'RU', emoji: '🇷🇺', name: '俄罗斯', nameEn: 'Russia' },
        { code: 'BY', emoji: '🇧🇾', name: '白俄罗斯', nameEn: 'Belarus' },
        { code: 'PL', emoji: '🇵🇱', name: '波兰', nameEn: 'Poland' },
        { code: 'DE', emoji: '🇩🇪', name: '德国', nameEn: 'Germany' },
        { code: 'NL', emoji: '🇳🇱', name: '荷兰', nameEn: 'Netherlands' },
        { code: 'FR', emoji: '🇫🇷', name: '法国', nameEn: 'France' }
      ]
    }
  }
}
</script>

<style scoped>
.mcr {
  position: relative;
  border-radius: 14px;
  margin-bottom: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.12);
}
.mcr--compact {
  margin-bottom: 8px;
  border-radius: 12px;
}
.mcr-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    120deg,
    rgba(30, 58, 138, 0.92) 0%,
    rgba(67, 56, 202, 0.88) 35%,
    rgba(124, 45, 18, 0.85) 70%,
    rgba(15, 118, 110, 0.9) 100%
  );
}
.mcr-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 20% 40%, rgba(251, 191, 36, 0.15) 0%, transparent 45%),
    radial-gradient(circle at 80% 60%, rgba(56, 189, 248, 0.12) 0%, transparent 40%);
  pointer-events: none;
}
.mcr-inner {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px 20px;
  padding: 14px 18px;
  z-index: 1;
  color: #f8fafc;
}
.mcr-compact-line {
  width: 100%;
  margin: 0 0 2px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
  opacity: 0.95;
}
.mcr--compact .mcr-compact-line {
  margin-bottom: 0;
}
.mcr--compact .mcr-inner {
  padding: 8px 14px;
  gap: 10px 14px;
}
.mcr--compact .mcr-compact-line ~ .mcr-globe-wrap {
  margin-top: 0;
}
.mcr-globe-wrap {
  flex-shrink: 0;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.25));
}
.mcr-globe {
  width: 40px;
  height: 40px;
  vertical-align: middle;
}
.mcr--compact .mcr-globe {
  width: 32px;
  height: 32px;
}
.mcr-people {
  flex: 1 1 180px;
  min-width: 0;
  max-width: 280px;
}
.mcr-svg {
  width: 100%;
  height: auto;
  max-height: 56px;
  display: block;
  opacity: 0.95;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.2));
}
.mcr--compact .mcr-svg {
  max-height: 40px;
  max-width: 200px;
}
/* 顶栏紧凑模式：保留地球与国旗带，省略人物剪影以省高 */
.mcr--compact .mcr-people {
  display: none;
}
.mcr--compact .mcr-flags {
  justify-content: flex-start;
}
.mcr-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  justify-content: flex-end;
  flex: 1 1 200px;
}
.mcr-flag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.25);
  font-size: 11px;
  cursor: default;
  transition: transform 0.15s ease, background 0.15s;
}
.mcr-flag:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.12);
}
.mcr-flag-emoji {
  font-size: 14px;
  line-height: 1;
}
.mcr-flag-code {
  font-weight: 700;
  letter-spacing: 0.06em;
  opacity: 0.95;
}
.mcr-caption {
  width: 100%;
  margin: 0;
  padding-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  opacity: 0.88;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
}
.mcr--compact .mcr-caption {
  display: none;
}
</style>
