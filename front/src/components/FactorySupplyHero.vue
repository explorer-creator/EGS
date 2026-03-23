<template>
  <section class="fs-hero" :class="'fs-hero--' + variant" role="banner">
    <div class="fs-hero__mesh" aria-hidden="true" />
    <div class="fs-hero__shine" aria-hidden="true" />
    <div class="fs-hero__inner">
      <div class="fs-hero__rail" />
      <div class="fs-hero__text">
        <span class="fs-hero__kicker">{{ kicker }}</span>
        <h1 class="fs-hero__title">{{ title }}</h1>
        <p class="fs-hero__desc">{{ description }}</p>
        <ul v-if="tags && tags.length" class="fs-hero__tags">
          <li v-for="(t, i) in tags" :key="i">{{ t }}</li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script>
export default {
  name: 'FactorySupplyHero',
  props: {
    /** factory | supply */
    variant: { type: String, default: 'factory' },
    kicker: { type: String, required: true },
    title: { type: String, required: true },
    description: { type: String, required: true },
    tags: { type: Array, default: () => [] }
  }
}
</script>

<style scoped>
.fs-hero {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  margin-bottom: 22px;
  padding: 28px 32px 26px;
  box-sizing: border-box;
  isolation: isolate;
}

.fs-hero__mesh {
  position: absolute;
  inset: 0;
  opacity: 0.45;
  background-image:
    linear-gradient(105deg, rgba(255, 255, 255, 0.04) 0%, transparent 42%),
    repeating-linear-gradient(
      -18deg,
      transparent,
      transparent 2px,
      rgba(255, 255, 255, 0.03) 2px,
      rgba(255, 255, 255, 0.03) 3px
    );
  pointer-events: none;
}

.fs-hero__shine {
  position: absolute;
  top: -40%;
  right: -15%;
  width: 55%;
  height: 180%;
  background: radial-gradient(ellipse at center, rgba(255, 255, 255, 0.14) 0%, transparent 65%);
  pointer-events: none;
}

.fs-hero__inner {
  position: relative;
  display: flex;
  align-items: stretch;
  gap: 20px;
  z-index: 1;
}

.fs-hero__rail {
  width: 4px;
  border-radius: 4px;
  flex-shrink: 0;
  align-self: stretch;
  min-height: 72px;
}

.fs-hero__text {
  flex: 1;
  min-width: 0;
}

.fs-hero__kicker {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  opacity: 0.88;
  margin-bottom: 10px;
}

.fs-hero__title {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.35;
  text-shadow: 0 2px 24px rgba(0, 0, 0, 0.25);
}

.fs-hero__desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  opacity: 0.92;
  max-width: 720px;
}

.fs-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}

.fs-hero__tags li {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(6px);
}

/* 智能工厂：深青 + 电青高光 */
.fs-hero--factory {
  color: #e8f4ff;
  background: linear-gradient(135deg, #071a2e 0%, #0c3d5c 48%, #0a2744 100%);
  box-shadow:
    0 4px 6px rgba(0, 20, 40, 0.35),
    0 20px 50px rgba(8, 47, 73, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.fs-hero--factory .fs-hero__rail {
  background: linear-gradient(180deg, #22d3ee 0%, #0ea5e9 55%, #0369a1 100%);
  box-shadow: 0 0 20px rgba(34, 211, 238, 0.45);
}

.fs-hero--factory .fs-hero__kicker {
  color: #7dd3fc;
}

/* 智慧供应链：靛紫 + 琥珀点缀 */
.fs-hero--supply {
  color: #f5f0e8;
  background: linear-gradient(135deg, #1a1033 0%, #312e81 42%, #1e1b4b 100%);
  box-shadow:
    0 4px 6px rgba(0, 0, 20, 0.4),
    0 22px 48px rgba(30, 27, 75, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.07);
}

.fs-hero--supply .fs-hero__rail {
  background: linear-gradient(180deg, #fbbf24 0%, #d97706 55%, #b45309 100%);
  box-shadow: 0 0 22px rgba(251, 191, 36, 0.35);
}

.fs-hero--supply .fs-hero__kicker {
  color: #fcd34d;
}
</style>
