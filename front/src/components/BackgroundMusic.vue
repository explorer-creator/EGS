<template>
  <div class="bg-music-fab" :title="playing ? '暂停背景音乐' : '播放背景音乐'">
    <button type="button" class="bg-music-btn" :class="{ 'is-playing': playing }" @click="toggle" :aria-label="playing ? '暂停音乐' : '播放音乐'">
      <i :class="playing ? 'el-icon-video-pause' : 'el-icon-video-play'"></i>
      <span class="bg-music-label">音乐</span>
    </button>
    <audio
      ref="audio"
      :src="audioSrc"
      loop
      preload="metadata"
      @play="playing = true"
      @pause="playing = false"
    />
  </div>
</template>

<script>
export default {
  name: 'BackgroundMusic',
  data() {
    return {
      playing: false
    }
  },
  computed: {
    audioSrc() {
      const base = import.meta.env.BASE_URL || '/'
      return base.replace(/\/?$/, '/') + 'music/musicnice.mp3'
    }
  },
  methods: {
    toggle() {
      const a = this.$refs.audio
      if (!a) return
      if (a.paused) {
        a.play().catch(() => {
          const m = this.$message
          if (m && typeof m.warning === 'function') {
            m.warning('无法播放，请检查是否静音或拦截了音频')
          }
        })
      } else {
        a.pause()
      }
    }
  }
}
</script>

<style scoped>
.bg-music-fab {
  position: fixed;
  z-index: 1998;
  top: max(12px, env(safe-area-inset-top, 0px));
  right: max(12px, env(safe-area-inset-right, 0px));
}
.bg-music-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(22, 93, 255, 0.92), rgba(14, 75, 204, 0.95));
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(22, 93, 255, 0.35);
  transition: transform 0.15s, box-shadow 0.15s;
}
.bg-music-btn:hover {
  transform: scale(1.03);
  box-shadow: 0 6px 18px rgba(22, 93, 255, 0.45);
}
.bg-music-btn.is-playing {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.9), rgba(5, 150, 105, 0.95));
}
.bg-music-label {
  font-weight: 600;
  letter-spacing: 0.06em;
}
.bg-music-btn i {
  font-size: 16px;
}
</style>
