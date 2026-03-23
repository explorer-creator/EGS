<template>
  <div class="pet-express-page">
    <el-card class="hero-card" shadow="hover">
      <div class="hero-inner">
        <div class="hero-icon">🐾</div>
        <div>
          <h2>爱宠专送 · 铁路出行参考</h2>
          <p class="hero-desc">
            选择出发站、到达站与出行日期，查询<strong>参考历时</strong>、<strong>二等座参考票价</strong>与<strong>宠物托运参考费用</strong>。
            与 12306 规则对齐的<strong>提示</strong>：高铁/动车通常不办理随车宠物托运；普速列车部分可办理，请以车站与
            <a href="https://www.12306.cn" target="_blank" rel="noopener">12306.cn</a> 公告为准。
          </p>
        </div>
      </div>
    </el-card>

    <el-card shadow="hover" class="query-card">
      <el-form inline label-width="100px" size="medium">
        <el-form-item label="出发站">
          <el-select v-model="form.from" placeholder="请选择" filterable style="width: 200px" @focus="loadStations">
            <el-option v-for="s in stations" :key="'f-'+s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="到达站">
          <el-select v-model="form.to" placeholder="请选择" filterable style="width: 200px" @focus="loadStations">
            <el-option v-for="s in stations" :key="'t-'+s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="出行日期">
          <el-date-picker
            v-model="form.date"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="选择日期"
            :picker-options="datePickerOpts"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="宠物体重(kg)">
          <el-input-number v-model="form.petKg" :min="1" :max="80" :step="0.5" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" :loading="loading" @click="query">查询车次与费用</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" style="margin-bottom:12px" />

      <template v-if="result && result.success">
        <el-alert type="warning" :closable="false" show-icon style="margin-bottom:12px">
          <template slot="title">{{ result.disclaimer }}</template>
        </el-alert>
        <p class="route-line">
          <strong>{{ result.fromStation }}</strong>
          <i class="el-icon-right"></i>
          <strong>{{ result.toStation }}</strong>
          <span class="meta">　出行日期 {{ result.date }}　·　参考营运里程约 {{ result.distanceKm }} km　·　宠物 {{ result.petWeightKg }} kg</span>
        </p>
        <el-table :data="result.trains" border stripe size="small" style="width:100%">
          <el-table-column prop="trainNo" label="车次" width="90" fixed />
          <el-table-column prop="trainType" label="类型" width="100" />
          <el-table-column prop="departTime" label="出发" width="90" />
          <el-table-column prop="arriveTime" label="到达" width="90" />
          <el-table-column prop="durationText" label="历时" width="130" />
          <el-table-column prop="secondClassYuan" label="二等座参考(元)" width="120" />
          <el-table-column prop="petConsignmentYuan" label="宠物托运参考(元)" width="140" />
          <el-table-column prop="totalEstimateYuan" label="合计参考(元)" width="110" />
          <el-table-column prop="petPolicyNote" label="托运/出行提示" min-width="220" show-overflow-tooltip />
        </el-table>
        <p class="ref-line">
          <i class="el-icon-link"></i> 官方渠道：<strong>{{ result.reference }}</strong>
        </p>
      </template>
    </el-card>
  </div>
</template>

<script>
function todayStr() {
  const d = new Date()
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

export default {
  name: 'PetExpressPage',
  data() {
    return {
      stations: [],
      form: {
        from: '北京',
        to: '上海',
        date: todayStr(),
        petKg: 5
      },
      loading: false,
      errorMsg: '',
      result: null,
      datePickerOpts: {
        disabledDate(time) {
          return time.getTime() < Date.now() - 86400000
        }
      }
    }
  },
  mounted() {
    this.loadStations()
  },
  methods: {
    async loadStations() {
      if (this.stations.length) return
      try {
        const res = await this.$axios.get('/api/pet-rail/stations')
        const data = res.data || res
        if (data.success && data.stations) {
          this.stations = data.stations
        }
      } catch (e) {
        this.stations = ['北京', '上海', '广州', '深圳', '杭州', '南京', '成都', '武汉', '西安', '郑州']
      }
    },
    async query() {
      this.errorMsg = ''
      this.result = null
      if (!this.form.from || !this.form.to) {
        this.errorMsg = '请选择出发站与到达站'
        return
      }
      if (!this.form.date) {
        this.errorMsg = '请选择出行日期'
        return
      }
      this.loading = true
      try {
        const res = await this.$axios.get('/api/pet-rail/query', {
          params: {
            fromStation: this.form.from,
            toStation: this.form.to,
            date: this.form.date,
            petWeightKg: this.form.petKg
          }
        })
        const data = res.data || res
        if (data.success) {
          this.result = data
          this.$message.success('已生成参考车次与费用')
        } else {
          this.errorMsg = data.message || '查询失败'
        }
      } catch (e) {
        this.errorMsg = (e.response && e.response.data && e.response.data.message) || e.message || '网络错误'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.pet-express-page {
  max-width: 1100px;
  margin: 0 auto;
}
.hero-card {
  margin-bottom: 16px;
  background: linear-gradient(135deg, #fff8f0 0%, #fff 50%, #f0f9ff 100%);
  border: 1px solid #fde2d2;
}
.hero-inner {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.hero-icon {
  font-size: 42px;
  line-height: 1;
}
.hero-inner h2 {
  margin: 0 0 8px;
  font-size: 1.35rem;
  color: #e6a23c;
}
.hero-desc {
  margin: 0;
  font-size: 0.9rem;
  color: #606266;
  line-height: 1.65;
}
.hero-desc a {
  color: #165dff;
}
.query-card {
  min-height: 200px;
}
.route-line {
  margin: 0 0 12px;
  font-size: 15px;
}
.route-line .meta {
  font-size: 13px;
  color: #909399;
  font-weight: normal;
}
.ref-line {
  margin: 12px 0 0;
  font-size: 13px;
  color: #909399;
}
</style>
