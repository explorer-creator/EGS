<template>
  <div class="pbl-shell">
    <div class="pbl-shell__bg" aria-hidden="true" />

    <div class="pbl-shell__body">
      <!-- 黑框：上排主入口 + 中间业务区 + 下排扩展入口 -->
      <div class="pbl-shell__frame">
        <nav class="pbl-shell__nav-row pbl-shell__nav-row--top" aria-label="主功能区">
          <button
            v-for="item in navMain"
            :key="item.key"
            type="button"
            :class="['pbl-shell__pill', { 'pbl-shell__pill--active': activeKey === item.key }]"
            @click="onNav(item.key)"
          >
            <i :class="item.icon" aria-hidden="true" />
            <span>{{ item.label }}</span>
          </button>
        </nav>

        <div class="pbl-shell__frame-inner">
          <slot />
        </div>

        <div class="pbl-shell__nav-split" role="separator" aria-hidden="true">
          <span class="pbl-shell__nav-split-label">{{ navSplitLabel }}</span>
        </div>

        <nav class="pbl-shell__nav-row pbl-shell__nav-row--bottom" aria-label="扩展区">
          <button
            v-for="item in navStory"
            :key="item.key"
            type="button"
            :class="['pbl-shell__pill', 'pbl-shell__pill--extra', { 'pbl-shell__pill--active': activeKey === item.key }]"
            @click="onNav(item.key)"
          >
            <i :class="item.icon" aria-hidden="true" />
            <span>{{ item.label }}</span>
          </button>
        </nav>
      </div>
    </div>

    <footer v-if="footerText" class="pbl-shell__foot">{{ footerText }}</footer>
  </div>
</template>

<script>
/**
 * EGS 主内容区布局：深色描边框 + 上/下两排入口，中间为业务 slot（与外侧蓝侧栏文案区分，不用绘本项目用语）。
 */
export default {
  name: 'PictureBookLikeShell',
  props: {
    activeKey: { type: String, default: '' },
    navMain: {
      type: Array,
      default: () => [
        { key: 'overview', label: '平台与展示', icon: 'el-icon-s-home' },
        { key: 'manufacture', label: '制造与协同', icon: 'el-icon-s-grid' },
        { key: 'prefs', label: '偏好与设置', icon: 'el-icon-setting' }
      ]
    },
    navStory: {
      type: Array,
      default: () => [{ key: 'extend', label: '丝路扩展', icon: 'el-icon-s-promotion' }]
    },
    navSplitLabel: { type: String, default: '专项能力' },
    footerText: {
      type: String,
      default: '丝路智联 · 演示布局；中间区域可替换为真实业务页面。'
    }
  },
  methods: {
    onNav(key) {
      this.$emit('update:activeKey', key)
      this.$emit('nav-change', key)
    }
  }
}
</script>

<style scoped>
.pbl-shell {
  --pbl-ink: #0f172a;
  --pbl-muted: #64748b;
  --pbl-accent: #165dff;
  --pbl-frame: rgba(15, 23, 42, 0.92);
  --pbl-radius: 14px;
  --pbl-font: system-ui, -apple-system, 'Segoe UI', sans-serif;

  min-height: 100%;
  display: flex;
  flex-direction: column;
  color: var(--pbl-ink);
  margin: 0;
  padding-bottom: 8px;
}

.pbl-shell__bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(ellipse 80% 50% at 10% 0%, rgba(22, 93, 255, 0.08), transparent 55%),
    radial-gradient(ellipse 70% 45% at 90% 10%, rgba(14, 75, 204, 0.07), transparent 50%),
    linear-gradient(168deg, #f8fafc 0%, #f1f5f9 45%, #eef2ff 100%);
}

.pbl-shell__body {
  position: relative;
  z-index: 1;
  flex: 1;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 8px 12px 0;
}

/* 黑框：深色描边 + 轻底，突出中间内容区 */
.pbl-shell__frame {
  border: 2px solid var(--pbl-frame);
  border-radius: var(--pbl-radius);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97) 0%, #ffffff 40%, #fafafa 100%);
  box-shadow:
    0 0 0 1px rgba(15, 23, 42, 0.06),
    0 16px 48px rgba(15, 23, 42, 0.08);
  padding: 14px 14px 16px;
}

.pbl-shell__nav-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 10px 12px;
}

.pbl-shell__nav-row--top {
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.pbl-shell__nav-row--bottom {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

.pbl-shell__pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 999px;
  border: 1px solid rgba(22, 93, 255, 0.22);
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  color: var(--pbl-ink);
  font-family: var(--pbl-font);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    border-color 0.15s ease,
    background 0.15s ease;
}

.pbl-shell__pill i {
  font-size: 16px;
  color: var(--pbl-accent);
}

.pbl-shell__pill:hover {
  border-color: rgba(22, 93, 255, 0.45);
  box-shadow: 0 4px 14px rgba(22, 93, 255, 0.12);
  transform: translateY(-1px);
}

.pbl-shell__pill--active {
  border-color: var(--pbl-accent);
  background: linear-gradient(180deg, rgba(22, 93, 255, 0.12) 0%, rgba(22, 93, 255, 0.06) 100%);
  box-shadow: 0 2px 10px rgba(22, 93, 255, 0.15);
}

.pbl-shell__pill--active i {
  color: #0e4bcc;
}

.pbl-shell__pill--extra {
  border-style: dashed;
  border-color: rgba(22, 93, 255, 0.28);
  background: rgba(248, 250, 252, 0.95);
}

.pbl-shell__pill--extra.pbl-shell__pill--active {
  border-style: solid;
}

.pbl-shell__frame-inner {
  min-height: 120px;
}

.pbl-shell__nav-split {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 4px 0 10px;
  padding: 0 4px;
}

.pbl-shell__nav-split::before,
.pbl-shell__nav-split::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(22, 93, 255, 0.25), transparent);
}

.pbl-shell__nav-split-label {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--pbl-muted);
  font-family: var(--pbl-font);
}

.pbl-shell__foot {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  padding: 12px 16px 16px;
  text-align: center;
  font-size: 12px;
  color: var(--pbl-muted);
  line-height: 1.6;
}

@media (max-width: 640px) {
  .pbl-shell__nav-row {
    justify-content: flex-start;
  }

  .pbl-shell__pill {
    flex: 1 1 calc(50% - 8px);
    min-width: 0;
    padding: 10px 12px;
    font-size: 13px;
  }
}
</style>
