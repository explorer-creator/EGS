<template>
  <div
    class="sac"
    :class="{ 'sac--hero': hero, 'sac--fill': fillContainer, 'sac--embedded': embedded }"
    role="region"
    aria-label="丝路智联主题轮播"
  >
    <div class="sac-frame" :class="{ 'sac-frame--fill': fillContainer }">
      <div
        class="sac-carousel-wrap"
        :class="{ 'sac-carousel-wrap--fill': fillContainer, 'sac-carousel-wrap--fixed': !fillContainer }"
        :style="wrapStyle"
      >
        <div class="sac-filmstrip" aria-roledescription="carousel">
          <!-- 胶片齿孔装饰 -->
          <div class="sac-filmstrip__perf sac-filmstrip__perf--left" aria-hidden="true" />
          <div class="sac-filmstrip__perf sac-filmstrip__perf--right" aria-hidden="true" />

          <div class="sac-filmstrip__stage">
          <div class="sac-filmstrip__row">
            <!-- 左 1/6：上一张，压暗 -->
            <button
              type="button"
              class="sac-filmstrip__cell sac-filmstrip__cell--side sac-filmstrip__cell--prev"
              :aria-label="'上一张：' + (slides[prevIndex] && slides[prevIndex].text)"
              @click="goPrev"
            >
              <div class="sac-filmstrip__cell-inner">
                <template v-if="!imgFailed[prevIndex]">
                  <img
                    class="sac-filmstrip__img"
                    :src="slideImgSrc(slides[prevIndex].file)"
                    :alt="slides[prevIndex].text"
                    loading="lazy"
                    @error="onImgError(prevIndex)"
                  />
                </template>
                <div v-else class="sac-filmstrip__fallback-mini">{{ prevIndex + 1 }}</div>
                <div class="sac-filmstrip__dim" aria-hidden="true" />
              </div>
            </button>

            <!-- 中 2/3：当前，正常亮度 -->
            <div class="sac-filmstrip__cell sac-filmstrip__cell--center" aria-current="true">
              <div class="sac-filmstrip__cell-inner">
                <template v-if="!imgFailed[activeIndex]">
                  <img
                    class="sac-filmstrip__img"
                    :src="slideImgSrc(slides[activeIndex].file)"
                    :alt="slides[activeIndex].text"
                    loading="eager"
                    @error="onImgError(activeIndex)"
                  />
                </template>
                <div v-else class="sac-filmstrip__fallback">
                  <span class="sac-filmstrip__fallback-num">{{ activeIndex + 1 }}</span>
                  <span class="sac-filmstrip__fallback-hint">将图片放入 public/carousel/{{ slides[activeIndex].file }}</span>
                </div>
              </div>
            </div>

            <!-- 右 1/6：下一张，压暗 -->
            <button
              type="button"
              class="sac-filmstrip__cell sac-filmstrip__cell--side sac-filmstrip__cell--next"
              :aria-label="'下一张：' + (slides[nextIndex] && slides[nextIndex].text)"
              @click="goNext"
            >
              <div class="sac-filmstrip__cell-inner">
                <template v-if="!imgFailed[nextIndex]">
                  <img
                    class="sac-filmstrip__img"
                    :src="slideImgSrc(slides[nextIndex].file)"
                    :alt="slides[nextIndex].text"
                    loading="lazy"
                    @error="onImgError(nextIndex)"
                  />
                </template>
                <div v-else class="sac-filmstrip__fallback-mini">{{ nextIndex + 1 }}</div>
                <div class="sac-filmstrip__dim" aria-hidden="true" />
              </div>
            </button>
          </div>

          <!-- 左右接缝处角色（白底图用 multiply 融入深色底；轻微缩放呼吸） -->
          <div class="sac-filmstrip__mascots" aria-hidden="true">
            <div class="sac-filmstrip__mascot sac-filmstrip__mascot--left">
              <div class="sac-filmstrip__mascot-pulse sac-filmstrip__mascot-pulse--delay">
                <img class="sac-filmstrip__mascot-img" :src="mascotLeftSrc" alt="" draggable="false" />
              </div>
            </div>
            <div class="sac-filmstrip__mascot sac-filmstrip__mascot--right">
              <div class="sac-filmstrip__mascot-pulse">
                <img class="sac-filmstrip__mascot-img" :src="mascotRightSrc" alt="" draggable="false" />
              </div>
            </div>
          </div>

          <div v-if="fillContainer" class="sac-typewriter sac-typewriter--overlay" aria-live="polite">
            <span class="sac-typewriter__text">{{ typedText }}</span>
            <span class="sac-typewriter__caret" aria-hidden="true" />
          </div>
          </div>

          <div class="sac-filmstrip__controls">
            <button type="button" class="sac-filmstrip__arrow sac-filmstrip__arrow--prev" aria-label="上一张" @click="goPrev">
              <i class="el-icon-arrow-left" />
            </button>
            <ul class="sac-filmstrip__dots" aria-label="幻灯片">
              <li v-for="(slide, idx) in slides" :key="idx">
                <button
                  type="button"
                  class="sac-filmstrip__dot"
                  :class="{ 'is-active': idx === activeIndex }"
                  :aria-label="'第 ' + (idx + 1) + ' 张：' + slide.text"
                  :aria-current="idx === activeIndex ? 'true' : undefined"
                  @click="goTo(idx)"
                />
              </li>
            </ul>
            <button type="button" class="sac-filmstrip__arrow sac-filmstrip__arrow--next" aria-label="下一张" @click="goNext">
              <i class="el-icon-arrow-right" />
            </button>
          </div>
        </div>

      </div>
      <div v-if="!fillContainer" class="sac-typewriter" aria-live="polite">
        <span class="sac-typewriter__text">{{ typedText }}</span>
        <span class="sac-typewriter__caret" aria-hidden="true" />
      </div>
    </div>
  </div>
</template>

<script>
/** 平台介绍页黑框轮播：每页一张图 + 对应一行打字机文案 */
/** 文件名需与磁盘格式一致（勿把 jpeg/webp 强行命名为 .png，否则部分环境加载失败） */
const SLIDES = [
  { file: '1.jpg', text: 'AI物流' },
  { file: '2.webp', text: 'AI工厂' },
  { file: '3.jpg', text: 'AI规划' },
  { file: '4.webp', text: 'AI+区块链' },
  { file: '5.png', text: 'Ai➕SSH' },
  { file: '6.webp', text: 'AI➕宠物' },
  { file: '7.webp', text: 'AI➕产品智慧问答' },
  { file: '8.jpg', text: 'AI+绿能排产' }
]

export default {
  name: 'SilkAiCarousel',
  props: {
    hero: { type: Boolean, default: false },
    fillContainer: { type: Boolean, default: false },
    embedded: { type: Boolean, default: false }
  },
  data() {
    return {
      slides: SLIDES,
      activeIndex: 0,
      typedText: '',
      typeTimer: null,
      imgFailed: {},
      autoplayTimer: null,
      autoplayMs: 6000
    }
  },
  computed: {
    prevIndex() {
      const n = this.slides.length
      return (this.activeIndex - 1 + n) % n
    },
    nextIndex() {
      const n = this.slides.length
      return (this.activeIndex + 1) % n
    },
    wrapStyle() {
      if (this.fillContainer) return {}
      const h = this.hero ? '440px' : '220px'
      return { minHeight: h, height: h }
    },
    mascotLeftSrc() {
      return this.baseUrl() + 'hero-mascots/mascot-left.png'
    },
    mascotRightSrc() {
      return this.baseUrl() + 'hero-mascots/mascot-right.png'
    }
  },
  watch: {
    activeIndex() {
      this.resetAutoplay()
    }
  },
  mounted() {
    this.runTypewriter(this.slides[0].text)
    this.startAutoplay()
  },
  beforeDestroy() {
    this.clearTypeTimer()
    this.stopAutoplay()
  },
  methods: {
    baseUrl() {
      const b = import.meta.env.BASE_URL || '/'
      return b.replace(/\/?$/, '/')
    },
    slideImgSrc(file) {
      return this.baseUrl() + 'carousel/' + file
    },
    onImgError(idx) {
      this.$set(this.imgFailed, idx, true)
    },
    goTo(index) {
      const n = this.slides.length
      if (index < 0 || index >= n) return
      this.activeIndex = index
      const t = this.slides[index] && this.slides[index].text
      if (t) this.runTypewriter(t)
    },
    goPrev() {
      const n = this.slides.length
      this.goTo((this.activeIndex - 1 + n) % n)
    },
    goNext() {
      const n = this.slides.length
      this.goTo((this.activeIndex + 1) % n)
    },
    startAutoplay() {
      this.stopAutoplay()
      this.autoplayTimer = setInterval(() => {
        this.goNext()
      }, this.autoplayMs)
    },
    stopAutoplay() {
      if (this.autoplayTimer) {
        clearInterval(this.autoplayTimer)
        this.autoplayTimer = null
      }
    },
    resetAutoplay() {
      this.startAutoplay()
    },
    clearTypeTimer() {
      if (this.typeTimer) {
        clearInterval(this.typeTimer)
        this.typeTimer = null
      }
    },
    runTypewriter(full) {
      this.clearTypeTimer()
      this.typedText = ''
      if (!full) return
      let i = 0
      const step = 60
      this.typeTimer = setInterval(() => {
        if (i < full.length) {
          this.typedText += full[i]
          i++
        } else {
          this.clearTypeTimer()
        }
      }, step)
    }
  }
}
</script>

<style scoped>
.sac {
  margin-bottom: 12px;
  border-radius: 14px;
  overflow: auto;
}
.sac.sac--hero {
  width: 100%;
  max-width: none;
  margin-left: 0;
  margin-right: 0;
  margin-bottom: 10px;
}
.sac.sac--fill {
  margin: 0;
  height: 100%;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: 0;
  overflow: hidden;
}
.sac.sac--embedded {
  margin-bottom: 0;
}

.sac-frame {
  border: 2px solid rgba(15, 23, 42, 0.92);
  border-radius: 14px;
  overflow: hidden;
  background: #0b1220;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.12);
}
.sac-frame.sac-frame--fill {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: none;
  border-radius: 0;
  box-shadow: none;
  background: #050508;
}
.sac-carousel-wrap--fill {
  flex: 1;
  min-height: 0;
  position: relative;
  display: flex;
  flex-direction: column;
}
.sac-carousel-wrap--fixed {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* —— 胶片式三栏：左右各 1/6 压暗，中央 2/3 高亮；图片 contain 完整显示 —— */
.sac-filmstrip {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #0d0d12 0%, #050508 50%, #0a0a10 100%);
  border-top: 10px solid #14141a;
  border-bottom: 10px solid #14141a;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    inset 0 -1px 0 rgba(0, 0, 0, 0.5);
}

.sac-filmstrip__perf {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 14px;
  z-index: 2;
  pointer-events: none;
  background-image: repeating-linear-gradient(
    180deg,
    transparent 0,
    transparent 10px,
    rgba(40, 40, 48, 0.95) 10px,
    rgba(40, 40, 48, 0.95) 18px
  );
  opacity: 0.9;
}
.sac-filmstrip__perf::after {
  content: '';
  position: absolute;
  top: 6px;
  bottom: 6px;
  left: 50%;
  transform: translateX(-50%);
  width: 6px;
  background: repeating-linear-gradient(
    180deg,
    #0a0a0e 0,
    #0a0a0e 5px,
    transparent 5px,
    transparent 12px
  );
  border-radius: 2px;
}
.sac-filmstrip__perf--left {
  left: 0;
}
.sac-filmstrip__perf--right {
  right: 0;
}

.sac-filmstrip__stage {
  flex: 1;
  min-height: 0;
  position: relative;
  display: flex;
  flex-direction: column;
}

/* 接缝处装饰角色：左/右与中栏分界；pointer-events 不挡点击侧栏 */
.sac-filmstrip__mascots {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
}
.sac-filmstrip__mascot {
  position: absolute;
  top: 44%;
  transform: translate(-50%, -50%);
}
.sac-filmstrip__mascot--left {
  /* 向中间带靠拢，避免贴在左右压暗区边缘 */
  left: calc(16.666% + min(5.5vw, 72px));
}
.sac-filmstrip__mascot--right {
  left: calc(83.333% - min(5.5vw, 72px));
}
.sac-filmstrip__mascot-pulse {
  transform-origin: 50% 88%;
  animation: sac-mascot-pulse 3.4s ease-in-out infinite;
}
.sac-filmstrip__mascot-pulse--delay {
  animation-delay: -1.7s;
}
@keyframes sac-mascot-pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.06);
  }
}
.sac-filmstrip__mascot-img {
  display: block;
  height: auto;
  width: auto;
  max-height: min(30vh, 200px);
  max-width: min(14vw, 120px);
  object-fit: contain;
  /* 白底 PNG：在深色背景上近似抠图 */
  mix-blend-mode: multiply;
  filter: brightness(1.12) contrast(1.05);
  opacity: 0.96;
}

.sac-filmstrip__row {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: row;
  align-items: stretch;
}

.sac-filmstrip__cell {
  position: relative;
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  border: none;
  background: #08080c;
}
.sac-filmstrip__cell--side {
  flex: 0 0 16.666%;
  width: 16.666%;
  cursor: pointer;
  overflow: hidden;
}
.sac-filmstrip__cell--center {
  flex: 0 0 66.666%;
  width: 66.666%;
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.sac-filmstrip__cell-inner {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
  box-sizing: border-box;
}

.sac-filmstrip__img {
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
  object-position: center;
  vertical-align: middle;
  user-select: none;
  pointer-events: none;
}

.sac-filmstrip__cell--side .sac-filmstrip__img {
  filter: brightness(0.42) contrast(0.92) saturate(0.85);
}

.sac-filmstrip__dim {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.38);
  pointer-events: none;
}

.sac-filmstrip__fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  text-align: center;
  color: rgba(248, 250, 252, 0.8);
  font-size: 13px;
}
.sac-filmstrip__fallback-num {
  font-size: 36px;
  font-weight: 800;
  opacity: 0.4;
}
.sac-filmstrip__fallback-hint {
  opacity: 0.65;
  max-width: 240px;
  line-height: 1.4;
}
.sac-filmstrip__fallback-mini {
  font-size: 22px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.25);
}

.sac-filmstrip__controls {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 12px 10px;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.35) 0%, rgba(10, 10, 14, 0.95) 100%);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.sac-filmstrip__arrow {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(20, 20, 28, 0.9);
  color: #e2e8f0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, border-color 0.15s;
}
.sac-filmstrip__arrow:hover {
  background: rgba(22, 93, 255, 0.45);
  border-color: rgba(255, 255, 255, 0.35);
}

.sac-filmstrip__dots {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 6px;
  list-style: none;
  margin: 0;
  padding: 0;
}
.sac-filmstrip__dot {
  width: 7px;
  height: 7px;
  padding: 0;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.28);
  cursor: pointer;
  transition: transform 0.15s, background 0.15s;
}
.sac-filmstrip__dot:hover {
  background: rgba(255, 255, 255, 0.5);
}
.sac-filmstrip__dot.is-active {
  width: 20px;
  border-radius: 4px;
  background: linear-gradient(90deg, #ff6b9d, #ffb347);
}

.sac-typewriter {
  min-height: 44px;
  padding: 10px 16px 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: linear-gradient(180deg, #020617 0%, #0f172a 100%);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.sac-typewriter--overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 4;
  min-height: 44px;
  padding: 10px 16px 14px;
  border-top: none;
  background: linear-gradient(0deg, rgba(0, 0, 0, 0.82) 0%, rgba(0, 0, 0, 0.25) 70%, transparent 100%);
  pointer-events: none;
}

.sac-typewriter__text {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #f8fafc;
  text-shadow:
    0 0 20px rgba(56, 189, 248, 0.35),
    0 2px 8px rgba(0, 0, 0, 0.85);
}

.sac-typewriter__caret {
  display: inline-block;
  width: 2px;
  height: 1.1em;
  margin-left: 2px;
  background: #38bdf8;
  animation: sac-blink 0.9s step-end infinite;
  vertical-align: -0.15em;
}

@keyframes sac-blink {
  50% {
    opacity: 0;
  }
}

@media screen and (max-width: 600px) {
  .sac-filmstrip__mascot-img {
    max-height: min(20vh, 120px);
    max-width: min(22vw, 88px);
  }
  .sac-filmstrip__mascot--left {
    left: calc(16.666% + min(4vw, 48px));
  }
  .sac-filmstrip__mascot--right {
    left: calc(83.333% - min(4vw, 48px));
  }
  .sac-typewriter__text {
    font-size: 13px;
    letter-spacing: 0.06em;
  }
  .sac-typewriter--overlay {
    padding: 8px 10px 12px;
    min-height: 40px;
  }
  .sac-filmstrip__arrow {
    width: 30px;
    height: 30px;
  }
  .sac-filmstrip__controls {
    padding: 6px 8px 8px;
    gap: 8px;
  }
  .sac-filmstrip__dot {
    width: 6px;
    height: 6px;
  }
  .sac-filmstrip__dot.is-active {
    width: 16px;
  }
}
</style>
