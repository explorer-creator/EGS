<template>
  <div class="silk-road-panel-root">
    <!-- 右侧固定标签：点击展开大屏 -->
    <button
      type="button"
      class="edge-trigger"
      :class="{ 'is-open': drawerVisible }"
      @click="openDrawer"
      title="丝路智联 · 物流贸易态势（含智慧物流全景）"
    >
      <span class="edge-trigger__icon">
        <i class="el-icon-data-line"></i>
      </span>
      <span class="edge-trigger__text">丝路<br>贸易<br>态势</span>
    </button>

    <el-drawer
      :visible.sync="drawerVisible"
      direction="rtl"
      :with-header="false"
      size="100%"
      custom-class="silk-trade-drawer"
      append-to-body
      :modal-append-to-body="true"
      :wrapper-closable="true"
      @close="onDrawerClose"
    >
      <div class="dashboard">
        <header class="dash-header">
          <div class="dash-header__left">
            <span class="dash-badge">丝路智联</span>
            <h1 class="dash-title">物流贸易与国际线路 · 态势感知</h1>
            <p class="dash-sub">智慧物流 WMS · AI+3D 沙盘 · 3D 国际通道 · 国内低空物流视点 · 厚德载物 正道直行</p>
          </div>
          <div class="dash-header__right">
            <span class="dash-clock">{{ clockStr }}</span>
            <el-button type="text" class="dash-close" @click="drawerVisible = false">
              <i class="el-icon-close"></i> 关闭
            </el-button>
          </div>
        </header>

        <div class="dash-body">
          <el-tabs v-model="activeTab" type="border-card" class="dash-tabs" @tab-click="onTabChange">
            <el-tab-pane label="智慧物流 · 全景" name="logistics">
              <div class="pane-logistics">
                <SmartLogisticsPage :embedded="true" />
              </div>
            </el-tab-pane>
            <el-tab-pane label="国际线路 · 3D 地球" name="global">
              <div class="pane-global">
                <div ref="globeContainer" class="globe-canvas-wrap" />
                <aside class="route-side">
                  <h3><i class="el-icon-position"></i> 主要贸易通道</h3>
                  <ul class="route-list">
                    <li v-for="(r, i) in tradeRoutes" :key="i" @click="highlightRoute(i)">
                      <span class="route-name">{{ r.name }}</span>
                      <span class="route-meta">{{ r.volume }}</span>
                    </li>
                  </ul>
                  <p class="route-tip">拖动旋转地球 · 滚轮缩放 · 线路为中欧/中亚/海上丝路等示意</p>
                </aside>
              </div>
            </el-tab-pane>
            <el-tab-pane label="国内低空物流视点" name="china">
              <div class="pane-china">
                <div ref="leafletContainer" class="leaflet-canvas-wrap" />
                <div class="china-legend">
                  <h4>图例</h4>
                  <div><span class="lg lg-corridor"></span> 低空物流走廊（示意）</div>
                  <div><span class="lg lg-hub"></span> 枢纽 / 起降点</div>
                  <div><span class="lg lg-zone"></span> 重点空域网格</div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import SmartLogisticsPage from '../views/SmartLogisticsPage.vue'

/* Three.js 通过 CDN 动态加载，避免打入 legacy 包导致构建失败（与 @vitejs/plugin-legacy 兼容） */
const THREE_CDN = 'https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js'
const ORBIT_CDN = 'https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js'

/** 经纬度转球面坐标（Y 轴朝上） */
function latLngToVec3(THREE, lat, lng, r) {
  const phi = (90 - lat) * (Math.PI / 180)
  const theta = (lng + 180) * (Math.PI / 180)
  return new THREE.Vector3(
    -r * Math.sin(phi) * Math.cos(theta),
    r * Math.cos(phi),
    r * Math.sin(phi) * Math.sin(theta)
  )
}

function slerpVec(THREE, v0, v1, t) {
  const dot = v0.dot(v1)
  const theta = Math.acos(Math.min(1, Math.max(-1, dot)))
  if (theta < 1e-6) return v0.clone()
  const s0 = Math.sin((1 - t) * theta) / Math.sin(theta)
  const s1 = Math.sin(t * theta) / Math.sin(theta)
  return v0.clone().multiplyScalar(s0).add(v1.clone().multiplyScalar(s1)).normalize()
}

function arcPoints(THREE, lat1, lng1, lat2, lng2, segs, radius, lift) {
  const v0 = latLngToVec3(THREE, lat1, lng1, 1).normalize()
  const v1 = latLngToVec3(THREE, lat2, lng2, 1).normalize()
  const pts = []
  for (let i = 0; i <= segs; i++) {
    const t = i / segs
    const base = slerpVec(THREE, v0, v1, t)
    const h = 1 + lift * Math.sin(Math.PI * t)
    pts.push(base.multiplyScalar(radius * h))
  }
  return pts
}

export default {
  name: 'SilkRoadTradePanel',
  components: { SmartLogisticsPage },
  data() {
    return {
      drawerVisible: false,
      activeTab: 'logistics',
      clockStr: '',
      clockTimer: null,
      tradeRoutes: [
        { name: '中欧班列 · 西安—杜伊斯堡', volume: '周均 80+ 列', from: [34.3, 108.9], to: [51.4, 6.8] },
        { name: '海上丝路 · 上海—新加坡', volume: '集装箱干线', from: [31.2, 121.5], to: [1.3, 103.8] },
        { name: '西部陆海 · 重庆—钦州—东盟', volume: '铁海联运', from: [29.6, 106.5], to: [1.3, 103.8] },
        { name: '空中丝路 · 郑州—卢森堡', volume: '货运航线', from: [34.7, 113.6], to: [49.6, 6.1] },
        { name: '太平洋干线 · 上海—洛杉矶', volume: '跨太平洋', from: [31.2, 121.5], to: [34.0, -118.2] },
        { name: '中老铁路 · 昆明—万象', volume: '跨境铁路', from: [25.0, 102.7], to: [17.9, 102.6] }
      ],
      globeInited: false,
      leafletInited: false,
      three: {
        renderer: null,
        scene: null,
        camera: null,
        controls: null,
        animationId: null,
        routeLines: []
      },
      leafletMap: null
    }
  },
  mounted() {
    this.tickClock()
    this.clockTimer = setInterval(this.tickClock, 1000)
  },
  beforeDestroy() {
    if (this.clockTimer) clearInterval(this.clockTimer)
    this.disposeGlobe()
    this.disposeLeaflet()
  },
  methods: {
    tickClock() {
      const d = new Date()
      this.clockStr = d.toLocaleString('zh-CN', {
        weekday: 'short',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    },
    loadScript(src) {
      return new Promise((resolve, reject) => {
        if (document.querySelector(`script[data-src="${src}"]`)) {
          resolve()
          return
        }
        const s = document.createElement('script')
        s.src = src
        s.async = true
        s.dataset.src = src
        s.onload = () => resolve()
        s.onerror = () => reject(new Error('script load: ' + src))
        document.head.appendChild(s)
      })
    },
    async ensureThreeLib() {
      if (typeof window !== 'undefined' && window.THREE && window.THREE.OrbitControls) return
      await this.loadScript(THREE_CDN)
      await this.loadScript(ORBIT_CDN)
      if (!window.THREE) throw new Error('THREE not available')
    },
    openDrawer() {
      this.drawerVisible = true
      this.$nextTick(() => {
        setTimeout(() => {
          if (this.activeTab === 'global') this.initGlobe()
          if (this.activeTab === 'china') this.initLeaflet()
          /* logistics 页为 SmartLogisticsPage，内部自初始化 Three；无需在此处理 */
        }, 200)
      })
    },
    onDrawerClose() {
      /* 保留 WebGL 上下文可下次再开；也可在此 dispose 省内存 */
    },
    onTabChange() {
      this.$nextTick(() => {
        setTimeout(() => {
          if (this.activeTab === 'global') this.initGlobe()
          if (this.activeTab === 'china') this.initLeaflet()
        }, 200)
      })
    },
    highlightRoute(index) {
      if (!this.three.scene) return
      const dim = 0.32
      this.three.routeLines.forEach((obj, i) => {
        const on = i === index
        if (obj.main && obj.main.material) {
          obj.main.material.opacity = on ? 1 : dim
          obj.main.material.emissiveIntensity = on ? 0.95 : 0.22
        }
        if (obj.glow && obj.glow.material) {
          obj.glow.material.opacity = on ? 0.42 : 0.12
        }
      })
    },
    async initGlobe() {
      if (this.globeInited) {
        this.onResizeGlobe()
        return
      }
      const el = this.$refs.globeContainer
      if (!el) return

      try {
        await this.ensureThreeLib()
      } catch (e) {
        console.error(e)
        this.$message.warning('3D 资源加载失败，请检查网络后重试')
        return
      }

      const THREE = window.THREE
      const OrbitControls = THREE.OrbitControls
      if (!OrbitControls) {
        this.$message.warning('轨道控制器未加载')
        return
      }

      const w = el.clientWidth || 800
      const h = el.clientHeight || 520

      const scene = new THREE.Scene()
      scene.background = new THREE.Color(0x020814)

      const camera = new THREE.PerspectiveCamera(45, w / h, 0.1, 2000)
      camera.position.set(0, 120, 280)

      const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
      renderer.setSize(w, h)
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
      el.appendChild(renderer.domElement)

      const amb = new THREE.AmbientLight(0x6a9cff, 0.45)
      const dir = new THREE.DirectionalLight(0x00e5ff, 0.9)
      dir.position.set(80, 120, 60)
      scene.add(amb, dir)

      const R = 100
      const globe = new THREE.Mesh(
        new THREE.SphereGeometry(R, 64, 64),
        new THREE.MeshPhongMaterial({
          color: 0x0a1a2e,
          emissive: 0x001830,
          shininess: 12,
          wireframe: false
        })
      )
      scene.add(globe)

      const wire = new THREE.Mesh(
        new THREE.SphereGeometry(R + 0.4, 32, 32),
        new THREE.MeshBasicMaterial({ color: 0x1a6cff, wireframe: true, transparent: true, opacity: 0.12 })
      )
      scene.add(wire)

      const routeLines = []
      const colors = [0xe8c547, 0xc41e3a, 0x2d8a6e, 0x4a90d9, 0xd4a017, 0x8b2332]

      this.tradeRoutes.forEach((route, i) => {
        const pts = arcPoints(THREE, route.from[0], route.from[1], route.to[0], route.to[1], 64, R, 0.24)
        const col = colors[i % colors.length]
        try {
          const curve = new THREE.CatmullRomCurve3(pts)
          const rad = 1.35
          const tubeGeo = new THREE.TubeGeometry(curve, Math.max(32, pts.length * 2), rad, 10, false)
          const mat = new THREE.MeshPhongMaterial({
            color: col,
            emissive: col,
            emissiveIntensity: 0.42,
            transparent: true,
            opacity: 0.88,
            shininess: 95,
            side: THREE.DoubleSide
          })
          const tube = new THREE.Mesh(tubeGeo, mat)
          scene.add(tube)
          const glowGeo = new THREE.TubeGeometry(curve, Math.max(32, pts.length * 2), rad * 1.85, 8, false)
          const glowMat = new THREE.MeshBasicMaterial({
            color: col,
            transparent: true,
            opacity: 0.22,
            depthWrite: false
          })
          const glow = new THREE.Mesh(glowGeo, glowMat)
          scene.add(glow)
          routeLines.push({ main: tube, glow })
        } catch (e) {
          console.warn('route tube', e)
        }
      })
      this.three.routeLines = routeLines

      const controls = new OrbitControls(camera, renderer.domElement)
      controls.enableDamping = true
      controls.dampingFactor = 0.05
      controls.minDistance = 160
      controls.maxDistance = 420
      controls.autoRotate = true
      controls.autoRotateSpeed = 0.35

      this.three.scene = scene
      this.three.camera = camera
      this.three.renderer = renderer
      this.three.controls = controls

      const animate = () => {
        this.three.animationId = requestAnimationFrame(animate)
        controls.update()
        renderer.render(scene, camera)
      }
      animate()

      this.globeInited = true
      window.addEventListener('resize', this.onResizeGlobe)
    },
    onResizeGlobe() {
      const el = this.$refs.globeContainer
      if (!el || !this.three.renderer || !this.three.camera) return
      const w = el.clientWidth
      const h = el.clientHeight
      if (w < 10 || h < 10) return
      this.three.camera.aspect = w / h
      this.three.camera.updateProjectionMatrix()
      this.three.renderer.setSize(w, h)
    },
    disposeGlobe() {
      window.removeEventListener('resize', this.onResizeGlobe)
      if (this.three.animationId) cancelAnimationFrame(this.three.animationId)
      if (this.three.renderer) {
        this.three.renderer.dispose()
        if (this.three.renderer.domElement && this.three.renderer.domElement.parentNode) {
          this.three.renderer.domElement.parentNode.removeChild(this.three.renderer.domElement)
        }
      }
      this.globeInited = false
      this.three = {
        renderer: null,
        scene: null,
        camera: null,
        controls: null,
        animationId: null,
        routeLines: []
      }
    },
    initLeaflet() {
      if (this.leafletInited) {
        this.leafletMap && this.leafletMap.invalidateSize()
        return
      }
      const el = this.$refs.leafletContainer
      if (!el) return

      const map = L.map(el, {
        center: [32.0, 108.0],
        zoom: 5,
        minZoom: 4,
        maxZoom: 10,
        zoomControl: true
      })

      L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; OpenStreetMap &copy; CARTO',
        subdomains: 'abcd',
        maxZoom: 19
      }).addTo(map)

      /* 低空走廊示意：长三角、珠三角、成渝等折线 */
      const corridors = [
        { color: '#00f5ff', latlngs: [[31.2, 121.5], [30.5, 120.2], [29.9, 121.8], [31.2, 121.5]] },
        { color: '#7cff00', latlngs: [[23.1, 113.3], [22.5, 114.0], [22.8, 113.5], [23.1, 113.3]] },
        { color: '#ffaa00', latlngs: [[30.6, 104.0], [29.5, 106.5], [30.6, 104.0]] },
        { color: '#ff66cc', latlngs: [[39.9, 116.4], [38.9, 121.6], [31.2, 121.5]] }
      ]

      corridors.forEach((c) => {
        L.polyline(c.latlngs, {
          color: c.color,
          weight: 3,
          opacity: 0.85,
          dashArray: '10, 8'
        }).addTo(map)
      })

      const hubs = [
        { name: '上海', lat: 31.2, lng: 121.5 },
        { name: '深圳', lat: 22.6, lng: 114.0 },
        { name: '成都', lat: 30.6, lng: 104.0 },
        { name: '西安', lat: 34.3, lng: 108.9 },
        { name: '郑州', lat: 34.7, lng: 113.6 }
      ]
      hubs.forEach((h) => {
        L.circleMarker([h.lat, h.lng], {
          radius: 8,
          color: '#00e5ff',
          fillColor: '#00e5ff',
          fillOpacity: 0.6,
          weight: 2
        })
          .bindTooltip(h.name, { permanent: false, direction: 'top' })
          .addTo(map)
      })

      /* 网格空域示意 */
      const zone = [
        [33, 119],
        [34, 121],
        [33, 123],
        [32, 122]
      ]
      L.polygon(zone, {
        color: '#4a9cff',
        weight: 1,
        fillOpacity: 0.08
      }).addTo(map)

      this.leafletMap = map
      this.leafletInited = true
      setTimeout(() => map.invalidateSize(), 300)
    },
    disposeLeaflet() {
      if (this.leafletMap) {
        this.leafletMap.remove()
        this.leafletMap = null
      }
      this.leafletInited = false
    }
  }
}
</script>

<style scoped>
.silk-road-panel-root {
  position: relative;
}

.edge-trigger {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2000;
  width: 44px;
  min-height: 120px;
  padding: 10px 6px;
  border: none;
  border-radius: 12px 0 0 12px;
  background: linear-gradient(180deg, #0a1628 0%, #132a52 50%, #0d1f3d 100%);
  box-shadow: -4px 0 24px rgba(0, 180, 255, 0.35), inset 0 0 0 1px rgba(0, 229, 255, 0.35);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #b8e8ff;
  font-size: 12px;
  line-height: 1.25;
  transition: width 0.2s, box-shadow 0.2s;
}
.edge-trigger:hover {
  width: 48px;
  box-shadow: -6px 0 32px rgba(0, 200, 255, 0.45);
  color: #fff;
}
.edge-trigger.is-open {
  opacity: 0.35;
  pointer-events: none;
}
.edge-trigger__icon {
  font-size: 18px;
  color: #00e5ff;
}
.edge-trigger__text {
  font-weight: 600;
  letter-spacing: 0.08em;
}

.dashboard {
  min-height: 100vh;
  background: radial-gradient(ellipse 120% 80% at 50% 0%, #0a1a33 0%, #020510 55%, #0a0a12 100%);
  color: #e8f4ff;
  padding: 0;
  box-sizing: border-box;
}

.dash-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px 24px;
  border-bottom: 2px solid rgba(196, 30, 58, 0.4);
  background: linear-gradient(90deg, rgba(90, 20, 28, 0.35) 0%, rgba(0, 60, 100, 0.22) 45%, transparent 100%);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
}
.dash-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  background: rgba(196, 30, 58, 0.28);
  border: 1px solid rgba(232, 197, 71, 0.55);
  color: #f3e5b8;
  font-size: 12px;
  margin-bottom: 8px;
}
.dash-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-shadow: 0 0 20px rgba(0, 200, 255, 0.45);
}
.dash-sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: #8ab4d4;
}
.dash-header__right {
  text-align: right;
}
.dash-clock {
  display: block;
  font-family: Consolas, monospace;
  color: #7cf9ff;
  font-size: 13px;
  margin-bottom: 4px;
}
.dash-close {
  color: #8ab4d4 !important;
}
.dash-close:hover {
  color: #fff !important;
}

.dash-body {
  padding: 12px 16px 18px;
  height: calc(100vh - 100px);
  box-sizing: border-box;
}

.dash-tabs {
  height: 100%;
  background: transparent !important;
  border: none !important;
}
.dash-tabs >>> .el-tabs__header {
  background: rgba(0, 40, 80, 0.35);
  border: 1px solid rgba(0, 180, 255, 0.25);
  border-radius: 8px;
}
.dash-tabs >>> .el-tabs__item {
  color: #8ab4d4 !important;
}
.dash-tabs >>> .el-tabs__item.is-active {
  color: #00e5ff !important;
}
.dash-tabs >>> .el-tabs__content {
  padding: 12px 0 0;
  height: calc(100% - 56px);
}
.dash-tabs >>> .el-tab-pane {
  height: 100%;
}

.pane-global {
  display: flex;
  gap: 16px;
  height: calc(100vh - 200px);
  min-height: 420px;
}
.globe-canvas-wrap {
  flex: 1;
  min-width: 0;
  border-radius: 8px;
  border: 1px solid rgba(0, 200, 255, 0.25);
  overflow: hidden;
  background: #020814;
}

.route-side {
  width: 240px;
  flex-shrink: 0;
  padding: 12px;
  border-radius: 8px;
  background: rgba(0, 30, 60, 0.45);
  border: 1px solid rgba(0, 200, 255, 0.2);
  font-size: 13px;
}
.route-side h3 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #7cf9ff;
  font-weight: 600;
}
.route-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.route-list li {
  padding: 8px 10px;
  margin-bottom: 6px;
  border-radius: 6px;
  background: rgba(0, 60, 120, 0.35);
  cursor: pointer;
  border: 1px solid transparent;
  transition: border 0.2s, background 0.2s;
}
.route-list li:hover {
  border-color: rgba(0, 229, 255, 0.45);
  background: rgba(0, 80, 140, 0.45);
}
.route-name {
  display: block;
  color: #e8f4ff;
  font-weight: 500;
}
.route-meta {
  font-size: 11px;
  color: #6a9cc8;
}
.route-tip {
  margin-top: 12px;
  font-size: 11px;
  color: #5a8ab0;
  line-height: 1.5;
}

.pane-logistics {
  height: calc(100vh - 120px);
  min-height: 480px;
  overflow: auto;
  overflow-x: hidden;
  padding: 4px 8px 24px;
  box-sizing: border-box;
  background: linear-gradient(180deg, rgba(254, 252, 248, 0.98) 0%, #f8fafc 100%);
  border-radius: 8px;
  border: 1px solid rgba(0, 200, 255, 0.25);
}

.pane-china {
  position: relative;
  height: calc(100vh - 200px);
  min-height: 420px;
}
.leaflet-canvas-wrap {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  border: 1px solid rgba(0, 200, 255, 0.25);
  overflow: hidden;
}
.china-legend {
  position: absolute;
  bottom: 24px;
  left: 24px;
  z-index: 1000;
  padding: 12px 14px;
  background: rgba(2, 8, 20, 0.82);
  border: 1px solid rgba(0, 200, 255, 0.35);
  border-radius: 8px;
  font-size: 12px;
  color: #b8d4e8;
}
.china-legend h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #7cf9ff;
}
.lg {
  display: inline-block;
  width: 14px;
  height: 4px;
  margin-right: 6px;
  vertical-align: middle;
}
.lg-corridor {
  background: linear-gradient(90deg, #00f5ff, #7cff00);
  border-radius: 2px;
}
.lg-hub {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #00e5ff;
}
.lg-zone {
  width: 12px;
  height: 12px;
  border: 1px solid #4a9cff;
  background: rgba(74, 156, 255, 0.2);
}

@media (max-width: 900px) {
  .pane-global {
    flex-direction: column;
  }
  .route-side {
    width: 100%;
  }
}
</style>

<style>
/* 抽屉全屏深色（append-to-body） */
.silk-trade-drawer.el-drawer {
  background: #020510 !important;
}
.silk-trade-drawer .el-drawer__body {
  padding: 0 !important;
  height: 100%;
  overflow: hidden;
}
</style>
