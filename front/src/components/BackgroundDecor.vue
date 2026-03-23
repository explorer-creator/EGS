<template>
  <div class="bg-decor">
    <!-- 渐变背景 -->
    <div class="bg-gradient"></div>
    <!-- 装饰元素层 -->
    <div class="decor-layer">
      <div v-for="(star, i) in stars" :key="'s' + i" class="star" :style="star.style">★</div>
      <div v-for="(planet, i) in planets" :key="'p' + i" class="planet" :style="planet.style"></div>
      <div v-for="(circle, i) in circles" :key="'c' + i" class="circle" :style="circle.style"></div>
      <div class="robot robot-1">🤖</div>
      <div class="robot robot-2">🤖</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BackgroundDecor',
  data() {
    const stars = []
    const planets = []
    const circles = []
    for (let i = 0; i < 20; i++) {
      stars.push({
        style: {
          left: Math.random() * 100 + '%',
          top: Math.random() * 100 + '%',
          fontSize: 8 + Math.random() * 12 + 'px',
          opacity: 0.3 + Math.random() * 0.5,
          animationDelay: Math.random() * 3 + 's'
        }
      })
    }
    for (let i = 0; i < 8; i++) {
      const size = 30 + Math.random() * 60
      planets.push({
        style: {
          left: Math.random() * 100 + '%',
          top: Math.random() * 100 + '%',
          width: size + 'px',
          height: size + 'px',
          animationDelay: Math.random() * 4 + 's'
        }
      })
    }
    for (let i = 0; i < 12; i++) {
      const size = 20 + Math.random() * 80
      circles.push({
        style: {
          left: Math.random() * 100 + '%',
          top: Math.random() * 100 + '%',
          width: size + 'px',
          height: size + 'px',
          animationDelay: Math.random() * 2 + 's'
        }
      })
    }
    return { stars, planets, circles }
  }
}
</script>

<style scoped>
.bg-decor {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}
.bg-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #e0f4ff 0%, #ffe4f0 25%, #fff9e6 50%, #f0e6ff 75%, #e0f4ff 100%);
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
}
@keyframes gradientShift {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}
.decor-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.star {
  position: absolute;
  color: #fff;
  text-shadow: 0 0 6px rgba(255, 255, 255, 0.8);
  transform: translate(-50%, -50%);
  animation: twinkle 2s ease-in-out infinite;
}
@keyframes twinkle {
  0%,
  100% {
    opacity: 0.4;
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.2);
  }
}
.planet {
  position: absolute;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.9), rgba(200, 180, 255, 0.4));
  box-shadow: inset -5px -5px 15px rgba(0, 0, 0, 0.1);
  animation: float 8s ease-in-out infinite;
}
.planet:nth-child(odd) {
  background: radial-gradient(circle at 30% 30%, rgba(255, 230, 200, 0.9), rgba(255, 180, 200, 0.4));
}
.planet:nth-child(3n) {
  background: radial-gradient(circle at 30% 30%, rgba(230, 255, 250, 0.9), rgba(180, 220, 255, 0.4));
}
@keyframes float {
  0%,
  100% {
    transform: translate(-50%, -50%) translate(0, 0) scale(1);
  }
  25% {
    transform: translate(-50%, -50%) translate(10px, -15px) scale(1.05);
  }
  50% {
    transform: translate(-50%, -50%) translate(-8px, 10px) scale(0.98);
  }
  75% {
    transform: translate(-50%, -50%) translate(5px, 5px) scale(1.02);
  }
}
.circle {
  position: absolute;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  border: 2px solid rgba(255, 255, 255, 0.5);
  background: transparent;
  animation: pulse 4s ease-in-out infinite;
}
@keyframes pulse {
  0%,
  100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.3;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.15);
    opacity: 0.6;
  }
}
.robot {
  position: absolute;
  font-size: 36px;
  opacity: 0.25;
  animation: robotFloat 6s ease-in-out infinite;
}
.robot-1 {
  left: 8%;
  top: 15%;
  animation-delay: 0s;
}
.robot-2 {
  right: 10%;
  bottom: 20%;
  font-size: 28px;
  animation-delay: 2s;
}
@keyframes robotFloat {
  0%,
  100% {
    transform: translateY(0) rotate(-5deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}
</style>
