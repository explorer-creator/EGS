<template>
  <div class="sl3d">
    <div class="sl3d-hero">
      <div class="sl3d-hero__title">
        <span class="sl3d-hero__mark">丝路智联</span>
        <h3 class="sl3d-h3">智慧物流 · 物联网数字孪生沙盘</h3>
      </div>
      <p class="sl3d-hero__sub">崇实笃行 · 数智赋能产业链 · 数据与可视化</p>
      <div class="sl3d-marquee" aria-hidden="true">
        <div class="sl3d-marquee__inner">
          <span v-for="(t, i) in tickerDup" :key="'m'+i" class="sl3d-marquee__item">{{ t }}</span>
        </div>
      </div>
    </div>

    <div class="sl3d-metrics">
      <div v-for="m in iotMetrics" :key="m.id" class="sl3d-metric">
        <div class="sl3d-metric__label">{{ m.label }}</div>
        <div class="sl3d-metric__val">
          <strong>{{ m.displayVal }}</strong><small>{{ m.unit }}</small>
        </div>
        <div class="sl3d-metric__bar"><i :style="{ width: m.pct + '%' }" /></div>
      </div>
    </div>

    <div class="sl3d-intro">
      <h4 class="sl3d-h4"><i class="el-icon-cpu"></i> AI 能力映射</h4>
      <ul class="sl3d-scenes">
        <li><strong>视觉</strong>：包裹/条码识别、库容视频分析、装卸区安全检测。</li>
        <li><strong>语言与多模态</strong>：智能客服、语音调度、运单与合同信息抽取。</li>
        <li><strong>运筹优化</strong>：波次与储位、AGV/AMR 路径、干线 VRP、配载。</li>
        <li><strong>预测</strong>：单量与波峰预测、ETA 置信带、异常预警。</li>
      </ul>
      <h4 class="sl3d-h4 sl3d-h4--iot"><i class="el-icon-connection"></i> 智慧物流物联网（示意）</h4>
      <ul class="sl3d-scenes sl3d-scenes--iot">
        <li><strong>RFID / 视觉门</strong>：托盘与批次级追溯，出入库自动核对。</li>
        <li><strong>温湿度与能耗传感</strong>：冷链区与月台环境联动告警。</li>
        <li><strong>北斗 / UWB 定位</strong>：载具与人员电子围栏、禁区预警。</li>
        <li><strong>5G / 工业网关</strong>：AGV、堆垛机与边缘推理低时延回传。</li>
      </ul>
      <p class="sl3d-tip">
        下方<strong>孪生视窗</strong>可旋转观察；<strong>拖拽卡片</strong>将 AI / 物联能力映射到作业区；地面<strong>红色管线</strong>为 AGV 规划路径示意。
      </p>
    </div>

    <div ref="viewport" class="sl3d-viewport" @mousemove="onMouseMove" @mouseup="endDrag" @mouseleave="endDrag">
      <div class="sl3d-scanlines" aria-hidden="true" />
      <div class="sl3d-frame-corner sl3d-frame-corner--tl" />
      <div class="sl3d-frame-corner sl3d-frame-corner--tr" />
      <div class="sl3d-frame-corner sl3d-frame-corner--bl" />
      <div class="sl3d-frame-corner sl3d-frame-corner--br" />
      <div ref="threeMount" class="sl3d-canvas-host" />

      <div class="sl3d-overlay sl3d-overlay--left">
        <div class="sl3d-panel">
          <div class="sl3d-panel__t">物联态势</div>
          <div class="sl3d-panel__row"><span>边缘节点</span><b>{{ liveStats.edge }}</b></div>
          <div class="sl3d-panel__row"><span>数据点/秒</span><b>{{ liveStats.dps }}</b></div>
          <div class="sl3d-panel__row"><span>今日告警</span><b class="ok">{{ liveStats.alerts }}</b></div>
        </div>
        <div class="sl3d-panel sl3d-panel--chart">
          <div class="sl3d-panel__t">库内吞吐（件/h）</div>
          <div class="sl3d-bars">
            <div v-for="(b, i) in throughputBars" :key="'tb'+i" class="sl3d-bar" :style="{ height: b + '%' }" />
          </div>
        </div>
        <div class="sl3d-panel sl3d-panel--spark">
          <div class="sl3d-panel__t">7 日履约趋势（指数）</div>
          <svg class="sl3d-spark" viewBox="0 0 120 40" preserveAspectRatio="none">
            <defs>
              <linearGradient id="sl3dSparkFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#fbbf24" stop-opacity="0.45" />
                <stop offset="100%" stop-color="#b45309" stop-opacity="0" />
              </linearGradient>
            </defs>
            <path :d="sparkArea" fill="url(#sl3dSparkFill)" />
            <path :d="sparkLine" fill="none" stroke="#fcd34d" stroke-width="1.2" />
          </svg>
        </div>
      </div>

      <div class="sl3d-overlay sl3d-overlay--right">
        <div class="sl3d-panel sl3d-panel--chain">
          <div class="sl3d-panel__t">
            <i class="el-icon-link"></i> 联盟链 · 货物指纹上链（SHA-256）
          </div>
          <div class="sl3d-chain">
            <div v-for="b in chainBlocks" :key="b.id" class="sl3d-block">
              <div class="sl3d-block__row">
                <span class="sl3d-block__idx">#{{ b.idx }}</span>
                <span class="sl3d-block__time">{{ b.time }}</span>
              </div>
              <div class="sl3d-block__cargo">{{ b.cargo }}</div>
              <div class="sl3d-block__hash" :title="b.fullHash">{{ b.hash }}</div>
              <div class="sl3d-block__prev">prev ⋯ {{ b.prevShort }}</div>
            </div>
          </div>
        </div>
        <CargoCryptoPanel />
      </div>

      <div
        v-for="c in cards"
        :key="c.id"
        class="sl3d-card"
        :class="{ 'sl3d-card--active': activeCardId === c.id, 'sl3d-card--drag': draggingId === c.id }"
        :style="{ left: c.x + '%', top: c.y + '%', borderColor: c.color }"
        @mousedown.prevent="startDrag($event, c)"
        @click.stop="onCardClick(c)"
      >
        <span class="sl3d-card__icon">{{ c.icon }}</span>
        <div class="sl3d-card__body">
          <strong>{{ c.title }}</strong>
          <span class="sl3d-card__sub">{{ c.sub }}</span>
        </div>
      </div>

      <div class="sl3d-hud">
        <span>AGV：<em>{{ agvState }}</em></span>
        <span v-if="activeCardId">模块：<em>{{ activeTitle }}</em></span>
        <span>孪生帧率：<em>{{ fpsHint }}</em></span>
        <span class="sl3d-hud__pill">链高度 <em>{{ chainHeight }}</em></span>
      </div>
    </div>
  </div>
</template>

<script>
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import CargoCryptoPanel from './CargoCryptoPanel.vue'
import { sha256Hex } from '../utils/cargoCrypto.js'

export default {
  name: 'SmartLogistics3DStage',
  components: { CargoCryptoPanel },
  data() {
    return {
      draggingId: null,
      dragOffset: { x: 0, y: 0 },
      activeCardId: null,
      agvState: '巡航',
      cards: [
        { id: 'v', icon: '👁', title: '视觉智能', sub: 'OCR / 检测', color: '#22c55e', x: 6, y: 8, speedMul: 1 },
        { id: 'n', icon: '💬', title: 'NLP / 语音', sub: '调度与客服', color: '#3b82f6', x: 6, y: 22, speedMul: 1 },
        { id: 'o', icon: '⚙', title: '运筹优化', sub: '路径 · 波次', color: '#eab308', x: 72, y: 8, speedMul: 1.6 },
        { id: 'p', icon: '📈', title: '预测分析', sub: '单量 · ETA', color: '#a855f7', x: 72, y: 22, speedMul: 0.85 },
        { id: 'rfid', icon: '📡', title: 'RFID 追溯', sub: '批次绑定', color: '#c41e3a', x: 8, y: 48, speedMul: 1.1 },
        { id: 'sensor', icon: '🌡', title: '温湿传感', sub: '冷链', color: '#0d9488', x: 8, y: 62, speedMul: 1 },
        { id: 'gnss', icon: '🛰', title: '北斗 / UWB', sub: '围栏', color: '#d97706', x: 70, y: 48, speedMul: 1.2 },
        { id: 'iotgw', icon: '📶', title: '5G 工业网关', sub: '低时延', color: '#64748b', x: 70, y: 62, speedMul: 1.15 }
      ],
      iotMetrics: [
        { id: 'm1', label: '在线感知设备', unit: '台', base: 118, displayVal: 118, pct: 78 },
        { id: 'm2', label: '物联网数据点', unit: '点/秒', base: 4200, displayVal: 4200, pct: 65 },
        { id: 'm3', label: '在途载具', unit: '辆', base: 36, displayVal: 36, pct: 52 },
        { id: 'm4', label: '合规巡检完成率', unit: '%', base: 99, displayVal: 99, pct: 99 }
      ],
      liveStats: { edge: 14, dps: 8420, alerts: 0 },
      throughputBars: [42, 68, 55, 72, 48, 80, 62],
      sparkSeries: [62, 58, 71, 69, 74, 78, 81],
      chainBlocks: [],
      chainHeight: 128406,
      chainSeq: 0,
      prevChainHash: '0'.repeat(64),
      tickerLines: [
        '● 中欧班列舱单哈希已同步至联盟节点',
        '● 冷链箱 EGS-COLD-09 温控签名验证通过',
        '● AGV 路径规划结果 Merkle 根更新',
        '● 跨境运单 AES-128 密文上链存证（演示）',
        '● 口岸单证 OCR 特征向量入池'
      ],
      fpsHint: '60',
      fpsLast: 0,
      fpsFrames: 0,
      metricTimer: null,
      chainTimer: null,
      scene: null,
      camera: null,
      renderer: null,
      controls: null,
      agv: null,
      animPath: [],
      animT: 0,
      speedScale: 1,
      raf: null,
      resizeObs: null
    }
  },
  computed: {
    activeTitle() {
      const c = this.cards.find((x) => x.id === this.activeCardId)
      return c ? c.title : ''
    },
    tickerDup() {
      return [...this.tickerLines, ...this.tickerLines]
    },
    sparkLine() {
      const s = this.sparkSeries
      if (!s.length) return ''
      const w = 120
      const h = 40
      const pad = 4
      const min = Math.min(...s) - 2
      const max = Math.max(...s) + 2
      const n = Math.max(1, s.length - 1)
      return s
        .map((v, i) => {
          const x = pad + (i / n) * (w - pad * 2)
          const y = h - pad - ((v - min) / (max - min || 1)) * (h - pad * 2)
          return `${i === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`
        })
        .join(' ')
    },
    sparkArea() {
      const line = this.sparkLine
      if (!line) return ''
      return `${line} L 116 36 L 4 36 Z`
    }
  },
  mounted() {
    this.fpsLast = performance.now()
    this.seedChain()
    this.$nextTick(() => this.initThree())
    let t = 0
      this.metricTimer = setInterval(() => {
      t += 1
      this.iotMetrics.forEach((m) => {
        const jitter = Math.sin(t / 3 + m.id.charCodeAt(1)) * (m.base * 0.02)
        let v = Math.max(0, Math.round(m.base + jitter))
        if (m.unit === '%') v = Math.min(100, Math.max(96, v))
        m.displayVal = v
        m.pct = Math.min(100, Math.round(45 + (v % 47) + Math.abs(jitter)))
      })
      this.liveStats = {
        edge: 12 + (t % 5),
        dps: 8000 + (t % 120) * 15,
        alerts: t % 17 === 0 ? 1 : 0
      }
      const arr = []
      for (let i = 0; i < 7; i++) arr.push(35 + Math.round(40 + 25 * Math.sin(t / 4 + i)))
      this.throughputBars = arr
      const spark = []
      for (let i = 0; i < 7; i++) spark.push(55 + Math.round(22 * Math.sin(t / 5 + i * 0.7)))
      this.sparkSeries = spark
    }, 1800)
    this.chainTimer = setInterval(() => this.appendChainBlock(), 3200)
  },
  beforeDestroy() {
    if (this.metricTimer) clearInterval(this.metricTimer)
    if (this.chainTimer) clearInterval(this.chainTimer)
    if (this.raf) cancelAnimationFrame(this.raf)
    if (this.resizeObs) this.resizeObs.disconnect()
    window.removeEventListener('mouseup', this.endDrag)
    if (this.controls) this.controls.dispose()
    if (this.renderer) {
      this.renderer.dispose()
      const host = this.$refs.threeMount
      if (host && this.renderer.domElement.parentNode === host) {
        host.removeChild(this.renderer.domElement)
      }
    }
  },
  methods: {
    initThree() {
      const host = this.$refs.threeMount
      if (!host) return

      const scene = new THREE.Scene()
      scene.background = new THREE.Color(0x120a0a)
      scene.fog = new THREE.Fog(0x120a0a, 14, 40)

      const camera = new THREE.PerspectiveCamera(48, 16 / 9, 0.1, 200)
      camera.position.set(10, 11, 14)
      camera.lookAt(0, 0, 0)

      const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false })
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
      renderer.shadowMap.enabled = true
      renderer.shadowMap.type = THREE.PCFSoftShadowMap
      host.appendChild(renderer.domElement)

      const controls = new OrbitControls(camera, renderer.domElement)
      controls.enableDamping = true
      controls.dampingFactor = 0.06
      controls.maxPolarAngle = Math.PI / 2.05
      controls.minDistance = 8
      controls.maxDistance = 28
      controls.target.set(0, 0.5, 0)

      const hemi = new THREE.HemisphereLight(0x9fc7ff, 0x1a1a2e, 0.85)
      scene.add(hemi)
      const dir = new THREE.DirectionalLight(0xffffff, 1.05)
      dir.position.set(8, 18, 10)
      dir.castShadow = true
      dir.shadow.mapSize.set(2048, 2048)
      dir.shadow.camera.near = 0.5
      dir.shadow.camera.far = 50
      dir.shadow.camera.left = -15
      dir.shadow.camera.right = 15
      dir.shadow.camera.top = 15
      dir.shadow.camera.bottom = -15
      scene.add(dir)

      const floor = new THREE.Mesh(
        new THREE.PlaneGeometry(32, 24),
        new THREE.MeshStandardMaterial({ color: 0x1f1814, metalness: 0.15, roughness: 0.88 })
      )
      floor.rotation.x = -Math.PI / 2
      floor.receiveShadow = true
      scene.add(floor)

      const grid = new THREE.GridHelper(32, 32, 0x7f1d1d, 0x292524)
      grid.position.y = 0.01
      scene.add(grid)

      const rackMat = new THREE.MeshStandardMaterial({ color: 0x475569, metalness: 0.35, roughness: 0.55 })
      const boxMat = new THREE.MeshStandardMaterial({ color: 0xc2410c, metalness: 0.15, roughness: 0.7 })
      for (let i = -2; i <= 2; i++) {
        for (let j = 0; j < 3; j++) {
          const rack = new THREE.Mesh(new THREE.BoxGeometry(2.2, 3.5, 1.2), rackMat)
          rack.position.set(i * 4.2, 1.75, -6 - j * 2.2)
          rack.castShadow = true
          rack.receiveShadow = true
          scene.add(rack)
          const box = new THREE.Mesh(new THREE.BoxGeometry(1.4, 0.6, 0.9), boxMat)
          box.position.set(i * 4.2 + 0.3, 2.4, -6 - j * 2.2 + 0.4)
          box.castShadow = true
          scene.add(box)
        }
      }

      const agv = new THREE.Group()
      const body = new THREE.Mesh(
        new THREE.BoxGeometry(1.4, 0.45, 2),
        new THREE.MeshStandardMaterial({ color: 0xf59e0b, metalness: 0.5, roughness: 0.35, emissive: 0x451a03, emissiveIntensity: 0.15 })
      )
      body.position.y = 0.35
      body.castShadow = true
      agv.add(body)
      const wheelGeo = new THREE.CylinderGeometry(0.22, 0.22, 0.12, 16)
      const wheelMat = new THREE.MeshStandardMaterial({ color: 0x171717, roughness: 0.9 })
      ;[
        [-0.55, 0.22, 0.65],
        [0.55, 0.22, 0.65],
        [-0.55, 0.22, -0.65],
        [0.55, 0.22, -0.65]
      ].forEach((p) => {
        const w = new THREE.Mesh(wheelGeo, wheelMat)
        w.rotation.z = Math.PI / 2
        w.position.set(p[0], p[1], p[2])
        w.castShadow = true
        agv.add(w)
      })
      agv.position.set(-6, 0, 5)
      scene.add(agv)

      const curve = new THREE.CatmullRomCurve3(
        [
          new THREE.Vector3(-6, 0.2, 5),
          new THREE.Vector3(-2, 0.2, 6),
          new THREE.Vector3(3, 0.2, 4),
          new THREE.Vector3(6, 0.2, 0),
          new THREE.Vector3(4, 0.2, -4),
          new THREE.Vector3(0, 0.2, -5),
          new THREE.Vector3(-5, 0.2, -2),
          new THREE.Vector3(-6, 0.2, 5)
        ],
        true,
        'catmullrom',
        0.35
      )
      this.animPath = curve.getPoints(200)

      try {
        const pathTube = new THREE.Mesh(
          new THREE.TubeGeometry(curve, 220, 0.16, 10, true),
          new THREE.MeshStandardMaterial({
            color: 0xb91c1c,
            emissive: 0x450a0a,
            emissiveIntensity: 0.45,
            metalness: 0.25,
            roughness: 0.75,
            transparent: true,
            opacity: 0.92
          })
        )
        pathTube.position.y = 0.06
        scene.add(pathTube)
        const pathGlow = new THREE.Mesh(
          new THREE.TubeGeometry(curve, 220, 0.32, 8, true),
          new THREE.MeshBasicMaterial({
            color: 0xdc2626,
            transparent: true,
            opacity: 0.18,
            depthWrite: false
          })
        )
        pathGlow.position.y = 0.06
        scene.add(pathGlow)
      } catch (e) {
        console.warn('path tube', e)
      }

      this.scene = scene
      this.camera = camera
      this.renderer = renderer
      this.controls = controls
      this.agv = agv

      const resize = () => {
        const el = this.$refs.viewport
        if (!el) return
        const w = el.clientWidth
        const h = Math.max(320, Math.round((w * 9) / 16))
        camera.aspect = w / h
        camera.updateProjectionMatrix()
        renderer.setSize(w, h)
      }
      resize()
      this.resizeObs = new ResizeObserver(resize)
      this.resizeObs.observe(this.$refs.viewport)

      const tick = () => {
        const now = performance.now()
        this.fpsFrames++
        if (now - this.fpsLast >= 1000) {
          this.fpsHint = String(Math.min(144, Math.round((this.fpsFrames * 1000) / (now - this.fpsLast))))
          this.fpsFrames = 0
          this.fpsLast = now
        }
        this.animT += 0.0012 * this.speedScale
        if (this.animT > 1) this.animT -= 1
        const idx = Math.floor(this.animT * (this.animPath.length - 1))
        const p = this.animPath[idx]
        const p2 = this.animPath[(idx + 1) % this.animPath.length]
        if (p && p2 && this.agv) {
          this.agv.position.set(p.x, 0, p.z)
          this.agv.lookAt(p2.x, 0.2, p2.z)
        }
        controls.update()
        renderer.render(scene, camera)
        this.raf = requestAnimationFrame(tick)
      }
      tick()

      window.addEventListener('mouseup', this.endDrag)
    },
    startDrag(e, card) {
      this.draggingId = card.id
      const vp = this.$refs.viewport.getBoundingClientRect()
      const cx = (card.x / 100) * vp.width + vp.left
      const cy = (card.y / 100) * vp.height + vp.top
      this.dragOffset = { x: e.clientX - cx, y: e.clientY - cy }
    },
    onMouseMove(e) {
      if (!this.draggingId) return
      const card = this.cards.find((c) => c.id === this.draggingId)
      if (!card) return
      const vp = this.$refs.viewport.getBoundingClientRect()
      let x = ((e.clientX - this.dragOffset.x - vp.left) / vp.width) * 100
      let y = ((e.clientY - this.dragOffset.y - vp.top) / vp.height) * 100
      x = Math.max(2, Math.min(78, x))
      y = Math.max(4, Math.min(72, y))
      card.x = x
      card.y = y
    },
    endDrag() {
      this.draggingId = null
    },
    async seedChain() {
      const samples = [
        { cargo: '运单 SF902→阿拉木图 · 哈希锚定' },
        { cargo: '托盘 RFID-EU-7712 · 跨境溯源' },
        { cargo: '冷链箱 TH-℃ 签名批次' }
      ]
      const blocks = []
      let prev = this.prevChainHash
      for (let i = 0; i < samples.length; i++) {
        const payload = `${prev}|${samples[i].cargo}|${i}`
        const fullHash = await sha256Hex(payload)
        blocks.push({
          id: 'seed-' + i,
          idx: this.chainHeight + i,
          time: this.fmtTime(),
          cargo: samples[i].cargo,
          hash: fullHash.slice(0, 18) + '…',
          fullHash,
          prevShort: i === 0 ? '0000…0000' : prev.slice(0, 14) + '…'
        })
        prev = fullHash
      }
      this.chainBlocks = blocks.reverse()
      this.prevChainHash = prev
      this.chainHeight += samples.length
    },
    fmtTime() {
      const d = new Date()
      return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d
        .getSeconds()
        .toString()
        .padStart(2, '0')}`
    },
    async appendChainBlock() {
      const cargos = [
        '整车备件 · 霍尔果斯口岸',
        '电商小包 · 9610 清单',
        '大宗矿产 · 铁路大票',
        '医药冷链 · GDP 温控链',
        '数字提单 · 智能合约锁定'
      ]
      const cargo = cargos[this.chainSeq % cargos.length]
      this.chainSeq++
      this.chainHeight++
      const payload = `${this.prevChainHash}|${cargo}|${Date.now()}`
      const fullHash = await sha256Hex(payload)
      const prevShort = this.prevChainHash.slice(0, 14) + '…'
      this.prevChainHash = fullHash
      const block = {
        id: 'b-' + this.chainSeq,
        idx: this.chainHeight,
        time: this.fmtTime(),
        cargo,
        hash: fullHash.slice(0, 20) + '…',
        fullHash,
        prevShort
      }
      const next = [block, ...this.chainBlocks].slice(0, 3)
      this.chainBlocks = next
    },
    onCardClick(c) {
      this.activeCardId = c.id
      this.speedScale = c.speedMul || 1
      const map = {
        v: '视觉巡检中',
        n: '语音指令待机',
        o: '路径重算中',
        p: '预测模式',
        rfid: 'RFID 核对中',
        sensor: '环境传感联动',
        gnss: '定位围栏监视',
        iotgw: '网关低时延回传'
      }
      this.agvState = map[c.id] || '巡航'
    }
  }
}
</script>

<style scoped>
.sl3d {
  max-width: 1100px;
  margin: 0 auto;
}
.sl3d-hero {
  text-align: center;
  margin-bottom: 14px;
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(127, 29, 29, 0.06) 0%, rgba(254, 243, 199, 0.35) 50%, rgba(254, 242, 242, 0.5) 100%);
  border: 1px solid rgba(180, 83, 9, 0.2);
  box-shadow: 0 8px 28px rgba(127, 29, 29, 0.06);
}
.sl3d-hero__title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.sl3d-hero__mark {
  display: inline-block;
  padding: 2px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #fef3c7;
  background: linear-gradient(90deg, #991b1b, #b45309);
  border: 1px solid rgba(250, 204, 21, 0.45);
}
.sl3d-hero__sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: #64748b;
  letter-spacing: 0.08em;
}
.sl3d-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 14px;
}
@media (max-width: 900px) {
  .sl3d-metrics {
    grid-template-columns: repeat(2, 1fr);
  }
}
.sl3d-metric {
  padding: 10px 12px;
  border-radius: 10px;
  background: linear-gradient(180deg, #fffefb 0%, #fff 100%);
  border: 1px solid rgba(180, 83, 9, 0.15);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
}
.sl3d-metric__label {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}
.sl3d-metric__val {
  font-size: 18px;
  color: #7f1d1d;
}
.sl3d-metric__val small {
  font-size: 11px;
  margin-left: 2px;
  color: #94a3b8;
}
.sl3d-metric__bar {
  height: 4px;
  border-radius: 4px;
  background: #f1f5f9;
  margin-top: 8px;
  overflow: hidden;
}
.sl3d-metric__bar > i {
  display: block;
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #b45309, #dc2626);
  transition: width 0.6s ease;
}
.sl3d-intro {
  margin-bottom: 16px;
}
.sl3d-h3 {
  margin: 0;
  font-size: 18px;
  color: #1c1917;
  font-weight: 700;
  letter-spacing: 0.04em;
}
.sl3d-h4 {
  margin: 12px 0 8px;
  font-size: 14px;
  color: #7f1d1d;
  font-weight: 600;
}
.sl3d-h4--iot {
  margin-top: 16px;
  color: #0f766e;
}
.sl3d-scenes--iot {
  border-left: 3px solid rgba(13, 148, 136, 0.35);
  padding-left: 12px;
}
.sl3d-scenes {
  margin: 0 0 10px;
  padding-left: 20px;
  color: #475569;
  font-size: 13px;
  line-height: 1.75;
}
.sl3d-tip {
  margin: 0;
  font-size: 12px;
  color: #57534e;
  line-height: 1.65;
  padding: 10px 12px;
  background: linear-gradient(90deg, rgba(254, 243, 199, 0.35), #fff);
  border-radius: 8px;
  border-left: 4px solid #b45309;
}
.sl3d-overlay {
  position: absolute;
  top: 10px;
  z-index: 3;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}
.sl3d-overlay--left {
  left: 10px;
  max-width: 42%;
}
.sl3d-panel {
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(28, 25, 23, 0.78);
  border: 1px solid rgba(212, 175, 55, 0.28);
  color: #fef3c7;
  font-size: 11px;
  line-height: 1.45;
}
.sl3d-panel__t {
  font-weight: 700;
  letter-spacing: 0.06em;
  margin-bottom: 6px;
  color: #fcd34d;
  font-size: 11px;
}
.sl3d-panel__row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 4px;
}
.sl3d-panel__row b {
  color: #fff;
  font-family: ui-monospace, monospace;
}
.sl3d-panel__row b.ok {
  color: #86efac;
}
.sl3d-panel--chart {
  min-width: 140px;
}
.sl3d-bars {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 52px;
  margin-top: 4px;
}
.sl3d-bar {
  flex: 1;
  min-width: 6px;
  border-radius: 3px 3px 0 0;
  background: linear-gradient(180deg, #fbbf24, #b45309);
  opacity: 0.9;
  transition: height 0.5s ease;
}
.sl3d-viewport {
  position: relative;
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 50px rgba(69, 10, 10, 0.25), inset 0 0 0 1px rgba(212, 175, 55, 0.25);
  background: #120a0a;
  aspect-ratio: 16 / 9;
  min-height: 320px;
}
.sl3d-canvas-host {
  position: absolute;
  inset: 0;
}
.sl3d-canvas-host >>> canvas {
  display: block;
  width: 100% !important;
  height: 100% !important;
}
.sl3d-card {
  position: absolute;
  z-index: 4;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 148px;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.82);
  border: 2px solid #64748b;
  color: #f8fafc;
  cursor: grab;
  user-select: none;
  backdrop-filter: blur(8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
  transition: box-shadow 0.15s, transform 0.15s;
}
.sl3d-card:hover {
  box-shadow: 0 12px 32px rgba(22, 93, 255, 0.25);
}
.sl3d-card--drag {
  cursor: grabbing;
  z-index: 10;
  transform: scale(1.02);
}
.sl3d-card--active {
  box-shadow: 0 0 0 2px rgba(212, 175, 55, 0.65), 0 12px 36px rgba(127, 29, 29, 0.35);
}
.sl3d-card__icon {
  font-size: 20px;
  line-height: 1;
}
.sl3d-card__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.sl3d-card__body strong {
  font-size: 13px;
}
.sl3d-card__sub {
  font-size: 11px;
  opacity: 0.8;
}
.sl3d-hud {
  position: absolute;
  left: 12px;
  bottom: 10px;
  z-index: 3;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.65);
  color: #e2e8f0;
  font-size: 12px;
  pointer-events: none;
}
.sl3d-hud em {
  color: #fde68a;
  font-style: normal;
  font-weight: 600;
}
.sl3d-hud__pill {
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.25);
  border: 1px solid rgba(165, 180, 252, 0.35);
}
.sl3d-hud__pill em {
  color: #c4b5fd;
}

/* 顶部滚动资讯 */
.sl3d-marquee {
  margin-top: 12px;
  overflow: hidden;
  border-radius: 8px;
  border: 1px solid rgba(180, 83, 9, 0.2);
  background: rgba(254, 243, 199, 0.25);
  height: 28px;
  line-height: 28px;
}
.sl3d-marquee__inner {
  display: inline-flex;
  white-space: nowrap;
  animation: sl3d-marquee 28s linear infinite;
}
.sl3d-marquee__item {
  padding: 0 32px;
  font-size: 11px;
  color: #78350f;
  letter-spacing: 0.06em;
}
@keyframes sl3d-marquee {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}

/* 视窗特效 */
.sl3d-scanlines {
  pointer-events: none;
  position: absolute;
  inset: 0;
  z-index: 2;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(0, 0, 0, 0.04) 2px,
    rgba(0, 0, 0, 0.04) 3px
  );
  mix-blend-mode: overlay;
  opacity: 0.35;
}
.sl3d-frame-corner {
  pointer-events: none;
  position: absolute;
  width: 28px;
  height: 28px;
  z-index: 2;
  border-color: rgba(251, 191, 36, 0.55);
  border-style: solid;
}
.sl3d-frame-corner--tl {
  top: 10px;
  left: 10px;
  border-width: 2px 0 0 2px;
}
.sl3d-frame-corner--tr {
  top: 10px;
  right: 10px;
  border-width: 2px 2px 0 0;
}
.sl3d-frame-corner--bl {
  bottom: 10px;
  left: 10px;
  border-width: 0 0 2px 2px;
}
.sl3d-frame-corner--br {
  bottom: 10px;
  right: 10px;
  border-width: 0 2px 2px 0;
}

.sl3d-overlay--right {
  right: 10px;
  left: auto;
  max-width: 38%;
  min-width: 200px;
  align-items: stretch;
  pointer-events: none;
}
@media (max-width: 900px) {
  .sl3d-overlay--right {
    max-width: 44%;
    min-width: 160px;
  }
}
.sl3d-panel--chain {
  pointer-events: auto;
  max-height: 200px;
  overflow-y: auto;
  background: linear-gradient(160deg, rgba(15, 23, 42, 0.88), rgba(30, 27, 75, 0.82));
  border-color: rgba(129, 140, 248, 0.35);
}
.sl3d-panel--spark {
  min-width: 140px;
}
.sl3d-spark {
  width: 100%;
  height: 44px;
  display: block;
  margin-top: 4px;
}
.sl3d-chain {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.sl3d-block {
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(167, 139, 250, 0.25);
  font-size: 10px;
  line-height: 1.45;
}
.sl3d-block__row {
  display: flex;
  justify-content: space-between;
  color: #a5b4fc;
  margin-bottom: 4px;
}
.sl3d-block__idx {
  font-weight: 700;
  color: #fcd34d;
}
.sl3d-block__time {
  font-family: ui-monospace, monospace;
  opacity: 0.85;
}
.sl3d-block__cargo {
  color: #e0e7ff;
  font-size: 11px;
  margin-bottom: 4px;
}
.sl3d-block__hash {
  font-family: ui-monospace, Consolas, monospace;
  color: #86efac;
  word-break: break-all;
  font-size: 10px;
}
.sl3d-block__prev {
  margin-top: 4px;
  color: #64748b;
  font-size: 9px;
}
</style>
