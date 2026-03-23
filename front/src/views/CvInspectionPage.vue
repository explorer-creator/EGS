<template>
  <div class="cv-inspection-page">
    <el-card class="cv-card" shadow="hover">
      <div class="cv-header">
        <i class="el-icon-camera"></i>
        <h2>PCB 裸板视觉质检</h2>
        <span class="subtitle">上传 PCB 图片，AI 识别漏焊、短路、断路等缺陷，缺陷坐标实时同步至工业软件元模型，触发自动报废流程</span>
      </div>

      <div class="cv-body">
        <div class="upload-area">
          <el-upload
            class="upload-dragger"
            drag
            :auto-upload="false"
            :show-file-list="false"
            :on-change="onFileChange"
            accept="image/*"
          >
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">将 PCB 图片拖到此处，或<em>点击上传</em></div>
            <div class="el-upload__tip">支持 JPG、PNG，建议尺寸 640×480 以上</div>
          </el-upload>
        </div>

        <div v-if="errorMsg" class="error-msg">
          <i class="el-icon-warning"></i> {{ errorMsg }}
        </div>

        <div v-if="inspecting" class="loading-area">
          <i class="el-icon-loading"></i> 正在检测缺陷...
        </div>

        <div v-else-if="imageUrl && defects.length >= 0" class="result-area">
          <div class="result-header">
            <span class="result-title">检测结果</span>
            <span class="defect-count" :class="{ hasDefect: defects.length > 0 }">
              {{ defects.length > 0 ? `发现 ${defects.length} 处缺陷` : '未发现缺陷' }}
            </span>
          </div>

          <div class="image-with-boxes">
            <div class="img-container" ref="imgContainer">
              <img ref="inspImage" :src="imageUrl" alt="PCB" @load="onImageLoad" />
              <canvas ref="defectCanvas" class="defect-canvas"></canvas>
            </div>
          </div>

          <div v-if="defects.length > 0" class="defect-list">
            <div class="demo-warning">
              <i class="el-icon-warning-outline"></i>
              <span><strong>模拟检测</strong>：当前为算法模拟结果，非产线级 AI 识别，<strong>请勿同步至生产系统</strong>。接入 PaddlePaddle / 昇腾 Atlas 后，将启用真实检测并开放同步功能。</span>
            </div>
            <h4>缺陷明细</h4>
            <el-table :data="defects" size="small" border>
              <el-table-column prop="type" label="缺陷类型" width="100" />
              <el-table-column label="坐标" width="180">
                <template slot-scope="scope">
                  ({{ scope.row.x }}, {{ scope.row.y }}) {{ scope.row.width }}×{{ scope.row.height }}
                </template>
              </el-table-column>
              <el-table-column label="置信度" width="100">
                <template slot-scope="scope">
                  {{ (scope.row.confidence * 100).toFixed(1) }}%
                </template>
              </el-table-column>
            </el-table>

            <div class="sync-area sync-disabled">
              <el-button type="primary" disabled>
                <i class="el-icon-connection"></i> 同步已禁用（模拟检测）
              </el-button>
              <p class="sync-tip">接入真实 CV 模型（PaddlePaddle / 昇腾 Atlas）后，方可同步缺陷至工业软件并触发报废</p>
            </div>
          </div>

          <div v-else-if="imageUrl" class="no-defect">
            <i class="el-icon-success"></i> 未检测到缺陷，PCB 合格
          </div>
        </div>

        <div v-else class="placeholder-area">
          <i class="el-icon-picture-outline"></i>
          <p>上传 PCB 裸板图片进行缺陷检测</p>
          <p class="hint">支持漏焊、短路、断路识别，可接入 PaddlePaddle、昇腾 Atlas 等</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'CvInspectionPage',
  data() {
    return {
      imageUrl: '',
      imageFile: null,
      defects: [],
      imageId: '',
      inspecting: false,
      syncing: false,
      errorMsg: '',
      imgNaturalW: 640,
      imgNaturalH: 480
    }
  },
  methods: {
    onFileChange(file) {
      if (!file || !file.raw) return
      this.imageUrl = ''
      this.defects = []
      this.errorMsg = ''

      const f = file.raw
      if (!f.type.startsWith('image/')) {
        this.errorMsg = '请上传图片文件'
        return
      }

      this.imageUrl = URL.createObjectURL(f)
      this.imageFile = f
      this.runInspection()
    },
    async runInspection() {
      if (!this.imageFile) return

      this.inspecting = true
      this.errorMsg = ''

      const form = new FormData()
      form.append('file', this.imageFile)

      try {
        const res = await this.$axios.post('/api/cv/inspect', form, {
          headers: form instanceof FormData ? {} : { 'Content-Type': 'multipart/form-data' }
        })
        const data = res.data || res
        if (data.success) {
          this.defects = data.defects || []
          this.imageId = data.imageId || ''
          this.$nextTick(() => this.drawBoxes())
        } else {
          this.errorMsg = data.message || '检测失败'
        }
      } catch (e) {
        this.errorMsg = e.response?.data?.message || '检测失败'
      } finally {
        this.inspecting = false
      }
    },
    onImageLoad() {
      const img = this.$refs.inspImage
      if (img) {
        this.imgNaturalW = img.naturalWidth || 640
        this.imgNaturalH = img.naturalHeight || 480
        this.$nextTick(() => this.drawBoxes())
      }
    },
    drawBoxes() {
      const canvas = this.$refs.defectCanvas
      const img = this.$refs.inspImage
      const container = this.$refs.imgContainer
      if (!canvas || !img || !container || this.defects.length === 0) return

      const rect = img.getBoundingClientRect()
      const scaleX = rect.width / this.imgNaturalW
      const scaleY = rect.height / this.imgNaturalH

      canvas.width = rect.width
      canvas.height = rect.height
      const ctx = canvas.getContext('2d')

      const colors = { 漏焊: '#f56c6c', 短路: '#e6a23c', 断路: '#909399' }
      this.defects.forEach((d, i) => {
        const x = (d.x || 0) * scaleX
        const y = (d.y || 0) * scaleY
        const w = (d.width || 0) * scaleX
        const h = (d.height || 0) * scaleY
        const color = colors[d.type] || '#165DFF'

        ctx.strokeStyle = color
        ctx.lineWidth = 2
        ctx.strokeRect(x, y, w, h)
        ctx.fillStyle = color
        ctx.font = '12px sans-serif'
        ctx.fillText(d.type + ' ' + ((d.confidence || 0) * 100).toFixed(0) + '%', x, y - 4)
      })
    },
    async syncToXdm() {
      if (this.defects.length === 0) {
        this.$message.info('无缺陷需同步')
        return
      }

      this.syncing = true
      try {
        const res = await this.$axios.post('/api/cv/sync-defects', {
          defects: this.defects,
          imageId: this.imageId
        })
        const data = res.data || res
        if (data.success) {
          this.$message.success(data.message || '同步成功')
        } else {
          this.$message.warning(data.message || '同步失败')
        }
      } catch (e) {
        this.$message.error(e.response?.data?.message || e.message || '同步失败')
      } finally {
        this.syncing = false
      }
    }
  }
}
</script>

<style scoped>
.cv-inspection-page {
  max-width: 900px;
  margin: 0 auto;
}
.cv-card {
  padding: 28px 36px;
}
.cv-header {
  margin-bottom: 24px;
  padding-bottom: 18px;
  border-bottom: 2px solid #165DFF;
}
.cv-header h2 {
  margin: 0 0 6px 0;
  font-size: 22px;
  color: #303133;
}
.cv-header .subtitle {
  font-size: 13px;
  color: #909399;
  display: block;
  margin-top: 4px;
}
.cv-header i {
  font-size: 26px;
  color: #165DFF;
  margin-right: 8px;
}
.upload-area {
  margin-bottom: 20px;
}
.upload-dragger >>> .el-upload-dragger {
  padding: 24px;
}
.upload-dragger >>> .el-upload__tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
.error-msg {
  padding: 12px 16px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 8px;
  color: #f56c6c;
  margin-bottom: 16px;
}
.loading-area {
  padding: 40px;
  text-align: center;
  color: #909399;
  font-size: 15px;
}
.result-area {
  margin-top: 20px;
}
.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.defect-count {
  font-size: 14px;
  color: #67c23a;
}
.defect-count.hasDefect {
  color: #f56c6c;
  font-weight: 600;
}
.image-with-boxes {
  margin-bottom: 20px;
}
.img-container {
  position: relative;
  display: inline-block;
  max-width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}
.img-container img {
  display: block;
  max-width: 100%;
  height: auto;
}
.defect-canvas {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
}
.defect-list {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
.defect-list h4 {
  margin: 0 0 12px 0;
  font-size: 15px;
  color: #303133;
}
.demo-warning {
  padding: 12px 16px;
  background: #fdf6ec;
  border: 1px solid #e6a23c;
  border-radius: 8px;
  color: #e6a23c;
  font-size: 13px;
  margin-bottom: 16px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.demo-warning i {
  font-size: 18px;
  flex-shrink: 0;
}
.demo-warning strong {
  color: #e6a23c;
}
.sync-area {
  margin-top: 20px;
  padding: 16px;
  background: #ecf5ff;
  border-radius: 8px;
}
.sync-area.sync-disabled {
  background: #f5f7fa;
  opacity: 0.9;
}
.sync-area.sync-disabled .el-button {
  cursor: not-allowed;
}
.sync-tip {
  margin: 8px 0 0 0;
  font-size: 12px;
  color: #606266;
}
.no-defect {
  padding: 24px;
  text-align: center;
  color: #67c23a;
  font-size: 16px;
}
.no-defect i {
  font-size: 32px;
  margin-right: 8px;
}
.placeholder-area {
  padding: 60px 40px;
  text-align: center;
  color: #909399;
}
.placeholder-area i {
  font-size: 48px;
  color: #dcdfe6;
  margin-bottom: 16px;
  display: block;
}
.placeholder-area .hint {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 8px;
}
</style>
