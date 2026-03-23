<template>
  <transition name="welcome-fade">
    <div v-show="visible" class="welcome-overlay">
      <canvas ref="canvas" class="fireworks-canvas"></canvas>
      <div class="welcome-text">
        <span class="text-main">欢迎使用丝路智联</span>
      </div>
    </div>
  </transition>
</template>

<script>
export default {
  name: 'WelcomeFireworks',
  props: {
    duration: { type: Number, default: 5000 }
  },
  data() {
    return {
      visible: true,
      fireworks: [],
      particles: [],
      animationId: null
    }
  },
  mounted() {
    this.initCanvas()
    this.launchFirework()
    this.animate()
    const interval = setInterval(() => this.launchFirework(), 400)
    setTimeout(() => {
      clearInterval(interval)
      this.visible = false
      if (this.animationId) cancelAnimationFrame(this.animationId)
      this.$emit('done')
    }, this.duration)
  },
  beforeDestroy() {
    if (this.animationId) cancelAnimationFrame(this.animationId)
    window.removeEventListener('resize', this.onResize)
  },
  methods: {
    initCanvas() {
      const canvas = this.$refs.canvas
      if (!canvas) return
      const dpr = window.devicePixelRatio || 1
      canvas.width = window.innerWidth * dpr
      canvas.height = window.innerHeight * dpr
      canvas.style.width = window.innerWidth + 'px'
      canvas.style.height = window.innerHeight + 'px'
      this.ctx = canvas.getContext('2d')
      this.ctx.scale(dpr, dpr)
      this.ctx.globalCompositeOperation = 'lighter'
      window.addEventListener('resize', this.onResize)
    },
    onResize() {
      const canvas = this.$refs.canvas
      if (!canvas) return
      const dpr = window.devicePixelRatio || 1
      canvas.width = window.innerWidth * dpr
      canvas.height = window.innerHeight * dpr
      canvas.style.width = window.innerWidth + 'px'
      canvas.style.height = window.innerHeight + 'px'
      this.ctx = canvas.getContext('2d')
      this.ctx.scale(dpr, dpr)
    },
    launchFirework() {
      const x = Math.random() * window.innerWidth * 0.6 + window.innerWidth * 0.2
      const y = window.innerHeight * 0.6 + Math.random() * 200
      const colors = ['#ff6b6b', '#4ecdc4', '#45b7d1', '#96ceb4', '#ffeaa7', '#dfe6e9', '#fd79a8', '#a29bfe']
      const color = colors[Math.floor(Math.random() * colors.length)]
      const particleCount = 80 + Math.floor(Math.random() * 40)
      const particles = []
      for (let i = 0; i < particleCount; i++) {
        const angle = (Math.PI * 2 / particleCount) * i + Math.random() * 0.5
        const speed = 3 + Math.random() * 8
        particles.push({
          x, y,
          vx: Math.cos(angle) * speed,
          vy: Math.sin(angle) * speed,
          life: 1,
          decay: 0.015 + Math.random() * 0.02,
          color,
          size: 1.5 + Math.random() * 1.5
        })
      }
      this.particles.push(...particles)
    },
    animate() {
      if (!this.ctx || !this.$refs.canvas) return
      this.ctx.clearRect(0, 0, window.innerWidth, window.innerHeight)
      this.particles = this.particles.filter(p => {
        p.x += p.vx
        p.y += p.vy
        p.vy += 0.15
        p.life -= p.decay
        if (p.life <= 0) return false
        this.ctx.globalAlpha = p.life
        this.ctx.fillStyle = p.color
        this.ctx.beginPath()
        this.ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
        this.ctx.fill()
        return true
      })
      this.ctx.globalAlpha = 1
      if (this.visible) {
        this.animationId = requestAnimationFrame(() => this.animate())
      }
    }
  }
}
</script>

<style scoped>
.welcome-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background: radial-gradient(ellipse at center, rgba(10,20,40,0.85) 0%, rgba(5,10,25,0.95) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.fireworks-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
.welcome-text {
  position: relative;
  z-index: 10;
  text-align: center;
}
.text-main {
  font-size: 48px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 0 20px rgba(255,215,0,0.8), 0 0 40px rgba(255,165,0,0.5);
  letter-spacing: 8px;
  animation: text-glow 1.5s ease-in-out infinite alternate;
}
@keyframes text-glow {
  from { text-shadow: 0 0 20px rgba(255,215,0,0.8), 0 0 40px rgba(255,165,0,0.5); }
  to { text-shadow: 0 0 30px rgba(255,255,200,1), 0 0 60px rgba(255,200,100,0.8); }
}
.welcome-fade-enter-active, .welcome-fade-leave-active {
  transition: opacity 1s ease;
}
.welcome-fade-leave-to {
  opacity: 0;
}
</style>
