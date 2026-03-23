<template>
  <div class="map-transport-page">
    <el-card class="intro-card" shadow="never">
      <h2><i class="el-icon-location-outline"></i> 运输线路与距离可视化</h2>
      <p class="intro-text">
        地图展示厂房、仓库、物料点；计算与您当前位置的<strong>直线距离</strong>；可选调用服务端<strong>高德驾车路径</strong>规划真实运输线路（需配置 AMAP_KEY）。
      </p>
      <el-tag v-if="amapReady" type="success" size="small">高德路径服务已配置</el-tag>
      <el-tag v-else type="info" size="small">未配置高德 Key，路线为直线示意</el-tag>
    </el-card>

    <!-- 城际运时：两市 + 交通方式 → 数学模型预估 -->
    <el-card class="city-estimate-card" shadow="hover">
      <div slot="header" class="city-est-header">
        <span><i class="el-icon-time"></i> 城际运时预估（数学建模）</span>
        <span class="city-est-sub">选择出发城市、目的城市与运输方式，自动计算球面距离、等效路网距离与在途+装卸时间</span>
      </div>
      <el-form inline label-width="100px" size="small" class="city-est-form">
        <el-form-item label="我的城市">
          <el-select v-model="cityFromName" placeholder="出发城市" filterable clearable style="width: 200px">
            <el-option v-for="c in chinaCities" :key="'f-' + c.name" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标城市">
          <el-select v-model="cityToName" placeholder="目的城市" filterable clearable style="width: 200px">
            <el-option v-for="c in chinaCities" :key="'t-' + c.name" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="运输方式">
          <el-radio-group v-model="transportMode">
            <el-radio label="road">公路干线</el-radio>
            <el-radio label="rail">铁路货运</el-radio>
            <el-radio label="air">航空货运</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-cpu" @click="estimateCityDelivery">预估运时</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="cityEstimate" type="success" :closable="false" show-icon class="city-est-result">
        <template slot="title">预估结果</template>
        <div class="est-body">
          <p><strong>{{ cityFromName }}</strong> → <strong>{{ cityToName }}</strong>　·　{{ cityEstimate.mode }}</p>
          <p>球面直线距离 <strong>{{ cityEstimate.straightKm.toFixed(0) }} km</strong>；等效运输距离 <strong>{{ cityEstimate.effectiveKm.toFixed(0) }} km</strong>（绕行系数 {{ cityEstimate.detour }}）</p>
          <p>在途约 <strong>{{ cityEstimate.transitHours.toFixed(1) }} h</strong>（按 {{ cityEstimate.speedKmh }} km/h）+ 装卸集散 <strong>{{ cityEstimate.handlingHours }} h</strong></p>
          <p class="est-total">综合预估运送时间：<strong>{{ cityEstimate.timeText }}</strong></p>
          <p class="est-formula">模型：{{ cityEstimate.formula }}</p>
        </div>
      </el-alert>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="hover" class="map-card">
          <div ref="mapContainer" class="leaflet-map" />
          <div class="map-toolbar">
            <el-button size="small" type="primary" icon="el-icon-position" :loading="locLoading" @click="locateMe">
              定位我的位置
            </el-button>
            <el-button size="small" @click="clearRoute">清除路线</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="side-card">
          <div slot="header">设施与您的距离</div>
          <p v-if="!userPos" class="hint">点击「定位我的位置」以计算距离</p>
          <el-table v-else :data="distanceRows" size="small" border max-height="220">
            <el-table-column prop="name" label="名称" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="60" />
            <el-table-column prop="dist" label="直线距离" width="100" />
          </el-table>
        </el-card>

        <el-card shadow="hover" class="side-card" style="margin-top:16px">
          <div slot="header">规划运输路线</div>
          <el-form label-width="80px" size="small">
            <el-form-item label="起点">
              <el-select v-model="routeFrom" placeholder="选择起点" filterable style="width:100%">
                <el-option v-for="f in facilities" :key="'o'+f.id" :label="f.name" :value="f.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="终点">
              <el-select v-model="routeTo" placeholder="选择终点" filterable style="width:100%">
                <el-option v-for="f in facilities" :key="'d'+f.id" :label="f.name" :value="f.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="routeLoading" @click="planRoute">规划路线</el-button>
            </el-form-item>
          </el-form>
          <p v-if="routeInfo" class="route-info">{{ routeInfo }}</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { haversineKm, formatDistance, latLngToAmap } from '../utils/geo'
import { DEFAULT_CENTER, FACILITIES, TYPE_COLORS } from '../data/mapFacilities'
import { CHINA_CITIES } from '../data/chinaCities'
import { estimateDelivery, formatDurationHours } from '../utils/deliveryEstimate'

// Vite 下修复 Leaflet 默认图标
import iconRetina from 'leaflet/dist/images/marker-icon-2x.png'
import iconUrl from 'leaflet/dist/images/marker-icon.png'
import shadowUrl from 'leaflet/dist/images/marker-shadow.png'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({ iconRetinaUrl: iconRetina, iconUrl, shadowUrl })

export default {
  name: 'MapTransportPage',
  data() {
    return {
      map: null,
      layers: { markers: [], routeLine: null, cityLine: null, cityMarkers: [] },
      facilities: FACILITIES,
      userPos: null,
      locLoading: false,
      routeFrom: '',
      routeTo: '',
      routeLoading: false,
      routeInfo: '',
      amapReady: false,
      chinaCities: CHINA_CITIES,
      cityFromName: '',
      cityToName: '',
      transportMode: 'road',
      cityEstimate: null
    }
  },
  computed: {
    distanceRows() {
      if (!this.userPos) return []
      const [lat, lng] = this.userPos
      return this.facilities
        .map(f => ({
          id: f.id,
          name: f.name,
          type: f.type,
          dist: formatDistance(haversineKm([lat, lng], [f.lat, f.lng])),
          km: haversineKm([lat, lng], [f.lat, f.lng])
        }))
        .sort((a, b) => a.km - b.km)
    }
  },
  mounted() {
    this.initMap()
    this.fetchMapConfig()
  },
  beforeDestroy() {
    if (this.map) {
      this.map.remove()
      this.map = null
    }
  },
  methods: {
    async fetchMapConfig() {
      try {
        const res = await this.$axios.get('/api/map/config')
        this.amapReady = !!(res.data && res.data.amapConfigured)
      } catch (e) {
        this.amapReady = false
      }
    },
    initMap() {
      this.map = L.map(this.$refs.mapContainer, {
        zoomControl: true
      }).setView(DEFAULT_CENTER, 11)

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap'
      }).addTo(this.map)

      this.facilities.forEach(f => {
        const color = TYPE_COLORS[f.type] || '#909399'
        const icon = L.divIcon({
          className: 'facility-marker',
          html: `<div style="background:${color};color:#fff;border-radius:50%;width:26px;height:26px;line-height:26px;text-align:center;font-size:11px;font-weight:bold;border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.4)">${f.type[0]}</div>`,
          iconSize: [26, 26],
          iconAnchor: [13, 13]
        })
        const m = L.marker([f.lat, f.lng], { icon })
          .bindPopup(`<b>${f.name}</b><br/>${f.type}<br/>${f.remark || ''}`)
          .addTo(this.map)
        this.layers.markers.push(m)
      })

      this.map.fitBounds(
        L.latLngBounds(this.facilities.map(f => [f.lat, f.lng])),
        { padding: [40, 40] }
      )
    },
    locateMe() {
      if (!navigator.geolocation) {
        this.$message.warning('浏览器不支持定位，已使用默认参考点')
        this.setUserMarker(DEFAULT_CENTER[0] + 0.02, DEFAULT_CENTER[1] + 0.02)
        return
      }
      this.locLoading = true
      navigator.geolocation.getCurrentPosition(
        pos => {
          this.locLoading = false
          this.setUserMarker(pos.coords.latitude, pos.coords.longitude)
          this.$message.success('已定位到当前位置')
        },
        () => {
          this.locLoading = false
          this.$message.warning('定位失败，使用地图中心附近作为参考点')
          this.setUserMarker(DEFAULT_CENTER[0] + 0.015, DEFAULT_CENTER[1] + 0.01)
        },
        { enableHighAccuracy: true, timeout: 12000 }
      )
    },
    setUserMarker(lat, lng) {
      this.userPos = [lat, lng]
      if (this.layers.userM) {
        this.map.removeLayer(this.layers.userM)
      }
      const icon = L.divIcon({
        className: 'user-marker',
        html: '<div style="background:#F56C6C;width:18px;height:18px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,.35)"></div>',
        iconSize: [18, 18],
        iconAnchor: [9, 9]
      })
      this.layers.userM = L.marker([lat, lng], { icon }).bindPopup('<b>我的位置</b>').addTo(this.map)
      this.map.panTo([lat, lng])
    },
    clearRoute() {
      if (this.layers.routeLine) {
        this.map.removeLayer(this.layers.routeLine)
        this.layers.routeLine = null
      }
      this.routeInfo = ''
    },
    clearCityOverlays() {
      if (this.layers.cityLine) {
        this.map.removeLayer(this.layers.cityLine)
        this.layers.cityLine = null
      }
      ;(this.layers.cityMarkers || []).forEach(m => this.map.removeLayer(m))
      this.layers.cityMarkers = []
    },
    estimateCityDelivery() {
      const a = this.chinaCities.find(c => c.name === this.cityFromName)
      const b = this.chinaCities.find(c => c.name === this.cityToName)
      if (!this.cityFromName || !this.cityToName) {
        this.$message.warning('请选择出发城市与目标城市')
        return
      }
      if (!a || !b) {
        this.$message.error('城市数据异常')
        return
      }
      if (a.name === b.name) {
        this.$message.warning('请选择两个不同的城市')
        return
      }
      const raw = estimateDelivery(a, b, this.transportMode)
      this.cityEstimate = {
        ...raw,
        timeText: formatDurationHours(raw.totalHours)
      }
      this.clearCityOverlays()
      const oIcon = L.divIcon({
        className: 'city-o',
        html: '<div style="background:#67C23A;color:#fff;border-radius:50%;width:28px;height:28px;line-height:28px;text-align:center;font-size:12px;font-weight:bold;border:2px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,.35)">起</div>',
        iconSize: [28, 28],
        iconAnchor: [14, 14]
      })
      const tIcon = L.divIcon({
        className: 'city-t',
        html: '<div style="background:#F56C6C;color:#fff;border-radius:50%;width:28px;height:28px;line-height:28px;text-align:center;font-size:12px;font-weight:bold;border:2px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,.35)">终</div>',
        iconSize: [28, 28],
        iconAnchor: [14, 14]
      })
      const m1 = L.marker([a.lat, a.lng], { icon: oIcon }).bindPopup(`<b>出发：${a.name}</b>`).addTo(this.map)
      const m2 = L.marker([b.lat, b.lng], { icon: tIcon }).bindPopup(`<b>目的：${b.name}</b>`).addTo(this.map)
      this.layers.cityMarkers = [m1, m2]
      this.layers.cityLine = L.polyline(
        [[a.lat, a.lng], [b.lat, b.lng]],
        { color: '#E6A23C', weight: 4, dashArray: '10,8', opacity: 0.9 }
      ).addTo(this.map)
      this.map.fitBounds(L.latLngBounds([[a.lat, a.lng], [b.lat, b.lng]]), { padding: [80, 80], maxZoom: 6 })
      this.$message.success('已根据模型预估运时，地图上为两市大圆示意连线')
    },
    async planRoute() {
      if (!this.routeFrom || !this.routeTo || this.routeFrom === this.routeTo) {
        this.$message.warning('请选择不同的起点和终点')
        return
      }
      const a = this.facilities.find(f => f.id === this.routeFrom)
      const b = this.facilities.find(f => f.id === this.routeTo)
      if (!a || !b) return

      this.clearRoute()
      this.routeLoading = true
      const origin = latLngToAmap(a.lat, a.lng)
      const dest = latLngToAmap(b.lat, b.lng)

      try {
        const res = await this.$axios.get('/api/map/driving', {
          params: { origin, destination: dest }
        })
        const data = res.data || res
        if (data.success && data.path && data.path.length) {
          const latlngs = data.path.map(([lng, lat]) => [lat, lng])
          this.layers.routeLine = L.polyline(latlngs, { color: '#165DFF', weight: 5, opacity: 0.85 }).addTo(this.map)
          this.map.fitBounds(this.layers.routeLine.getBounds(), { padding: [50, 50] })
          const km = (data.distance || 0) / 1000
          const min = Math.round((data.duration || 0) / 60)
          this.routeInfo = `驾车约 ${km.toFixed(2)} km，预估 ${min} 分钟（高德）`
          this.$message.success('路线已规划')
        } else {
          this.fallbackStraightLine(a, b)
          this.routeInfo = (data.message || '高德不可用') + '，已显示直线距离 ' + formatDistance(haversineKm([a.lat, a.lng], [b.lat, b.lng]))
        }
      } catch (e) {
        this.fallbackStraightLine(a, b)
        this.routeInfo = '接口异常，直线距离 ' + formatDistance(haversineKm([a.lat, a.lng], [b.lat, b.lng]))
      } finally {
        this.routeLoading = false
      }
    },
    fallbackStraightLine(a, b) {
      const latlngs = [
        [a.lat, a.lng],
        [b.lat, b.lng]
      ]
      this.layers.routeLine = L.polyline(latlngs, { color: '#909399', weight: 4, dashArray: '8,6' }).addTo(this.map)
      this.map.fitBounds(L.latLngBounds(latlngs), { padding: [60, 60] })
    }
  }
}
</script>

<style scoped>
.map-transport-page {
  max-width: 1200px;
  margin: 0 auto;
}
.intro-card {
  margin-bottom: 16px;
}
.intro-card h2 {
  margin: 0 0 8px;
  font-size: 1.2rem;
}
.intro-text {
  margin: 0 0 10px;
  color: #606266;
  font-size: 0.9rem;
  line-height: 1.6;
}
.map-card {
  position: relative;
}
.leaflet-map {
  height: 420px;
  width: 100%;
  border-radius: 8px;
  z-index: 0;
}
.map-toolbar {
  margin-top: 10px;
}
.side-card .hint {
  color: #909399;
  font-size: 13px;
  margin: 0 0 8px;
}
.route-info {
  font-size: 12px;
  color: #409eff;
  margin: 8px 0 0;
  line-height: 1.5;
}
.city-estimate-card {
  margin-bottom: 16px;
}
.city-est-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.city-est-sub {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}
.city-est-form {
  flex-wrap: wrap;
}
.city-est-result {
  margin-top: 12px;
}
.est-body p {
  margin: 6px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
.est-total {
  font-size: 15px !important;
  color: #303133 !important;
}
.est-formula {
  font-size: 12px !important;
  color: #909399 !important;
  font-family: monospace;
}
.city-o,
.city-t {
  background: transparent !important;
  border: none !important;
}
</style>

<style>
.facility-marker,
.user-marker {
  background: transparent !important;
  border: none !important;
}
</style>
