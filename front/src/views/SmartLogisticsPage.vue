<template>
  <div class="smart-logistics-page" :class="{ 'smart-logistics-page--embedded': embedded }">
    <el-card class="intro-card" shadow="never">
      <h2><i class="el-icon-truck"></i> 智慧物流（仓储 · 分拣 · 路径规划）</h2>
      <p class="intro-text">
        面向制造与仓储联动的轻量 WMS：库区库位、入库/出库与库存可视化；分拣波次与任务队列；
        基于网格的 <strong>A*</strong> 路径规划与多停靠点顺序优化（最近邻），可用于 AGV/分拣机器人路径规划。
        业界智慧物流中的人工智能接口通常分为四类：<strong>视觉识别</strong>、<strong>自然语言处理</strong>、<strong>运筹优化</strong>与<strong>预测分析</strong>——详见下方「物流 AI 接口映射」页签。
      </p>
    </el-card>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- ========== AI + 3D 沙盘 ========== -->
      <el-tab-pane label="AI+物流 3D 沙盘" name="ai3d">
        <SmartLogistics3DStage />
      </el-tab-pane>

      <!-- ========== WMS ========== -->
      <el-tab-pane label="WMS 仓储管理" name="wms">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-card shadow="hover">
              <div slot="header">仓库 / 库区</div>
              <el-select v-model="currentWhId" placeholder="选择仓库" style="width:100%;margin-bottom:12px" @change="onWhChange">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
              <el-table :data="currentZones" size="small" border max-height="220" @row-click="onZoneClick">
                <el-table-column prop="code" label="库区编码" width="90" />
                <el-table-column prop="name" label="名称" />
                <el-table-column prop="type" label="类型" width="80" />
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card shadow="hover">
              <div slot="header">
                库位库存
                <span v-if="selectedZone" class="zone-tag">{{ selectedZone.name }}（{{ selectedZone.code }}）</span>
              </div>
              <el-table :data="locationRows" size="small" border max-height="280">
                <el-table-column prop="locCode" label="库位" width="100" />
                <el-table-column prop="sku" label="SKU/物料" min-width="120" />
                <el-table-column prop="qty" label="数量" width="80" />
                <el-table-column prop="maxQty" label="容量" width="70" />
                <el-table-column prop="status" label="状态" width="80">
                  <template slot-scope="scope">
                    <el-tag :type="scope.row.status === '满' ? 'danger' : scope.row.status === '空' ? 'info' : 'success'" size="mini">
                      {{ scope.row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-form inline size="small" class="wms-form">
                <el-form-item label="库位">
                  <el-input v-model="ioForm.locCode" placeholder="如 A-01-03" style="width:120px" />
                </el-form-item>
                <el-form-item label="SKU">
                  <el-input v-model="ioForm.sku" placeholder="物料编码" style="width:140px" />
                </el-form-item>
                <el-form-item label="数量">
                  <el-input-number v-model="ioForm.qty" :min="1" :max="9999" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" size="small" @click="stockIn">入库</el-button>
                  <el-button type="warning" size="small" @click="stockOut">出库</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="hover" style="margin-top:16px">
          <div slot="header">入库 / 出库流水（最近）</div>
          <el-table :data="ioLogs.slice(0, 15)" size="small" border>
            <el-table-column prop="time" label="时间" width="160" />
            <el-table-column prop="type" label="类型" width="70" />
            <el-table-column prop="locCode" label="库位" width="100" />
            <el-table-column prop="sku" label="SKU" />
            <el-table-column prop="qty" label="数量" width="70" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ========== 分拣 ========== -->
      <el-tab-pane label="分拣与波次" name="sort">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-card shadow="hover">
              <div slot="header">新建波次</div>
              <el-form label-width="80px" size="small">
                <el-form-item label="波次号">
                  <el-input v-model="waveForm.waveNo" placeholder="自动生成可留空" />
                </el-form-item>
                <el-form-item label="订单数">
                  <el-input-number v-model="waveForm.orderCount" :min="1" :max="50" />
                </el-form-item>
                <el-form-item label="SKU种类">
                  <el-input-number v-model="waveForm.skuCount" :min="1" :max="50" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="createWave">生成波次任务</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
          <el-col :span="16">
            <el-card shadow="hover">
              <div slot="header">分拣任务队列</div>
              <el-table :data="sortTasks" size="small" border>
                <el-table-column prop="id" label="任务ID" width="90" />
                <el-table-column prop="waveNo" label="波次" width="120" />
                <el-table-column prop="station" label="拣货站" width="100" />
                <el-table-column prop="skuCount" label="SKU数" width="70" />
                <el-table-column prop="status" label="状态" width="90">
                  <template slot-scope="scope">
                    <el-tag :type="scope.row.status === '已完成' ? 'success' : scope.row.status === '进行中' ? 'warning' : 'info'" size="mini">
                      {{ scope.row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="160">
                  <template slot-scope="scope">
                    <el-button v-if="scope.row.status === '待分拣'" type="text" size="small" @click="startSort(scope.row)">开始分拣</el-button>
                    <el-button v-if="scope.row.status === '进行中'" type="text" size="small" @click="finishSort(scope.row)">完成</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
        <el-alert type="info" :closable="false" show-icon style="margin-top:16px">
          <template slot="title">说明</template>
          波次将多订单合并为拣货任务，按库区/动线减少行走距离；可与上方 WMS 库位联动展示「按单拣货 / 边拣边分」策略。
        </el-alert>
      </el-tab-pane>

      <!-- ========== 物流 AI 接口（四类） ========== -->
      <el-tab-pane label="物流 AI 接口映射" name="aiApi">
        <el-alert type="success" :closable="false" show-icon class="ai-api-banner">
          <template slot="title">说明</template>
          下表归纳典型<strong>场景</strong>与可对接的<strong>云端/边缘 API 形态</strong>（含第三方商业服务示例），实际选型需结合数据出境、时延与成本评估。
        </el-alert>

        <h3 class="ai-h3"><i class="el-icon-view"></i> 一、视觉识别类</h3>
        <p class="ai-lead">主要用于包裹分拣、车辆调度、安防监控、库容分析等场景。</p>
        <el-table :data="visionRows" border size="small" stripe class="ai-table">
          <el-table-column prop="scene" label="场景" min-width="140" />
          <el-table-column prop="capability" label="能力说明" min-width="220" />
          <el-table-column prop="apiType" label="常见 API / 服务形态" min-width="200" />
          <el-table-column prop="note" label="补充" min-width="160" show-overflow-tooltip />
        </el-table>
        <ul class="ai-bullets">
          <li><strong>包裹 / 条码识别</strong>：调用 <strong>OCR API</strong>（如阿里云 OCR、腾讯云 OCR）识别面单上的条形码、二维码及手写/打印文字，驱动自动分拣；对<strong>破损包裹</strong>可叠加<strong>图像分类/缺陷检测 API</strong>（工业质检类模型）判断是否拦截复核。</li>
          <li><strong>车辆与人员监控</strong>：园区出入口调用<strong>车牌识别 API</strong>实现自动放行与登记；装卸区通过<strong>目标检测 API</strong>（如 YOLO 系列云端推理服务）识别未戴安全帽、人员倒地、车辆违规停放等行为并联动告警。</li>
          <li><strong>库容管理</strong>：通过<strong>视频分析 / 视觉计数 API</strong>对接仓内普通摄像头，实时估算货架占用率、托盘空置率，作为自动盘点与补货触发信号，减轻人工盘点强度。</li>
        </ul>

        <h3 class="ai-h3"><i class="el-icon-chat-dot-round"></i> 二、自然语言处理类</h3>
        <p class="ai-lead">主要用于<strong>客服</strong>、<strong>单据录入</strong>及<strong>调度交互</strong>；常见对接大模型、ASR 与信息抽取服务。</p>
        <el-table :data="nlpRows" border size="small" stripe class="ai-table">
          <el-table-column prop="scene" label="场景" min-width="130" />
          <el-table-column prop="capability" label="能力说明" min-width="260" />
          <el-table-column prop="apiType" label="常见 API / 服务形态" min-width="220" />
          <el-table-column prop="note" label="对接提示" min-width="140" show-overflow-tooltip />
        </el-table>
        <ul class="ai-bullets">
          <li><strong>智能客服</strong>：使用<strong>对话机器人 API</strong>（如通义千问等），自动处理「货到哪了」「何时派送」等高频查询；通过<strong>意图识别</strong>与<strong>实体提取</strong>（运单号、地址、时间窗）生成回复或查询 TMS/WMS，降低人工客服压力。</li>
          <li><strong>语音调度</strong>：调用<strong>语音转文字 API</strong>（如 Whisper API 或云厂商 ASR），将司机/调度员语音上报（如「货已装完，准备发车」）转为文本并<strong>自动录入 TMS</strong>；异常口述可实时转写并<strong>生成结构化日志</strong>（事件类型、车号、时间）供追溯。</li>
          <li><strong>单据处理</strong>：针对非标准化<strong>运单或合同</strong>，调用 <strong>NLP 信息抽取 API</strong>，自动提取发货方、收货方、品名、体积/重量等关键字段，写入业务系统，减少人工录入与差错。</li>
        </ul>

        <h3 class="ai-h3"><i class="el-icon-cpu"></i> 三、运筹优化类</h3>
        <p class="ai-lead">
          与路径、库存、排班、配载等决策问题对应。<strong>功能路径规划</strong>可调用<strong>高德 / 谷歌路线规划 API</strong>获取<strong>实时路况与预计时间</strong>；
          对<strong>多车、多约束</strong>的<strong>VRP</strong>，通常使用<strong>求解器</strong>（如 <strong>Gurobi</strong>、<strong>CPLEX</strong> 或开源 <strong>OR-Tools</strong>）进行数学优化，与单纯地图导航分工不同。
        </p>
        <el-table :data="orRows" border size="small" stripe class="ai-table">
          <el-table-column prop="scene" label="场景" min-width="130" />
          <el-table-column prop="capability" label="能力说明" min-width="210" />
          <el-table-column prop="apiType" label="算法 / API 形态" min-width="230" />
          <el-table-column prop="note" label="说明" min-width="110" show-overflow-tooltip />
        </el-table>
        <ul class="ai-bullets">
          <li><strong>地图路线与 ETA</strong>：调用<strong>高德</strong>、<strong>Google Directions</strong> 等<strong>路线规划 REST API</strong>，返回路径、实时/预测路况与分段预计时间，适用于单车导航、成本矩阵或与 TMS 联动展示 ETA。</li>
          <li><strong>VRP 与复杂调度</strong>：涉及多车、容量、时间窗、装卸顺序等时，宜建模为 VRP/VRPTW 等，用 <strong>Gurobi / CPLEX / OR-Tools</strong> 等求解；本页网格 <strong>A*</strong> 与「智慧 TMS」<strong>蚁群</strong>为示意实现，可替换为「地图 API + 求解器」链路。</li>
        </ul>

        <h3 class="ai-h3"><i class="el-icon-data-analysis"></i> 四、预测分析类</h3>
        <p class="ai-lead">
          基于历史数据与实时特征，输出时序预测、概率与<strong>置信区间</strong>。其中<strong>需求预测</strong>可对接<strong>时序预测 API</strong>（如 <strong>Amazon Forecast</strong>、<strong>Facebook Prophet</strong> 的云托管/集成版本），
          依据历史订单预测未来 <strong>1～7 天</strong>单量曲线，辅助<strong>运力储备</strong>与<strong>仓库排班</strong>。
        </p>
        <el-table :data="predRows" border size="small" stripe class="ai-table">
          <el-table-column prop="scene" label="场景" min-width="120" />
          <el-table-column prop="capability" label="能力说明" min-width="220" />
          <el-table-column prop="apiType" label="常见 API / 模型形态" min-width="230" />
          <el-table-column prop="note" label="说明" min-width="120" show-overflow-tooltip />
        </el-table>
        <ul class="ai-bullets">
          <li><strong>需求预测（单量）</strong>：将历史订单按日/小时聚合为<strong>时序序列</strong>，调用云端时序服务训练/推理，输出未来 <strong>1～7 天</strong>的预测单量及区间；结果可输入 TMS 做<strong>车辆与司机预留</strong>，输入 WMS/人力系统做<strong>分拣与库内排班</strong>。</li>
          <li><strong>典型服务形态</strong>：<strong>Amazon Forecast</strong>（含 DeepAR 等算法）、<strong>Prophet</strong> 在云上的托管推理（如 SageMaker、Azure、自建容器）、或同类 AutoML 时序 API；需保证历史数据长度与促销、节假日等<strong>外生变量</strong>可特征化。</li>
        </ul>
      </el-tab-pane>

      <!-- ========== 路径规划 ========== -->
      <el-tab-pane label="路径规划（A*）" name="path">
        <el-alert type="info" :closable="false" show-icon class="path-api-banner">
          <template slot="title">与真实业务路径能力的区别</template>
          下图为<strong>栅格地图上的 A*</strong>，用于理解避障与多停靠点顺序。<strong>实际道路</strong>中可调用<strong>高德 / 谷歌路线规划 API</strong>获取<strong>实时路况与预计时间</strong>；
          对<strong>多车、多约束</strong>的 <strong>VRP</strong>，通常使用 <strong>Gurobi、CPLEX</strong> 或开源 <strong>OR-Tools</strong> 等<strong>求解器</strong>建模求解，与地图 API 分工协作（详见「物流 AI 接口映射」→ 运筹优化类）。
        </el-alert>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-card shadow="hover">
              <div slot="header">操作说明</div>
              <ol class="path-help">
                <li>点击「障碍」后在地图上点击格子切换障碍。</li>
                <li>点击「起点」→ 点地图设起点；「终点」→ 点地图设终点。</li>
                <li>「多停靠点」：依次点击地图添加途经点，再计算顺序与总路径。</li>
                <li>黑色=障碍，绿=起点，红=终点，蓝=路径，橙=停靠点。</li>
              </ol>
              <el-radio-group v-model="mapMode" size="small" style="margin-bottom:10px">
                <el-radio-button label="obstacle">障碍</el-radio-button>
                <el-radio-button label="start">起点</el-radio-button>
                <el-radio-button label="goal">终点</el-radio-button>
                <el-radio-button label="stop">停靠点</el-radio-button>
              </el-radio-group>
              <div>
                <el-button type="primary" size="small" @click="runAstar">计算 A* 路径</el-button>
                <el-button size="small" @click="runMultiStop">多停靠点规划</el-button>
                <el-button size="small" @click="resetMap">重置地图</el-button>
              </div>
              <p v-if="pathResult" class="path-result">
                {{ pathResult }}
              </p>
            </el-card>
          </el-col>
          <el-col :span="16">
            <div class="map-wrap">
              <canvas
                ref="pathCanvas"
                class="path-canvas"
                width="640"
                height="360"
                @click="onCanvasClick"
              />
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import SmartLogistics3DStage from '../components/SmartLogistics3DStage.vue'
import { astarPath, nearestNeighborOrder } from '../utils/pathfinding'

const LS_KEY = 'egs_wms_logistics_v1'

export default {
  name: 'SmartLogisticsPage',
  components: { SmartLogistics3DStage },
  props: {
    /** 嵌入「丝路贸易态势」等大屏抽屉时使用：加宽布局、默认打开 3D 页签 */
    embedded: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      activeTab: 'wms',
      visionRows: [
        { scene: '包裹分拣', capability: '面单条码/二维码/文字识别，驱动分拣口', apiType: '通用 OCR、条码识别、快递面单专用 OCR', note: '可与 WMS 订单号对齐' },
        { scene: '破损件处理', capability: '外包装破损、变形等缺陷判别', apiType: '图像分类 / 分割 / 异常检测 API', note: '可与人工复核台联动' },
        { scene: '园区车辆', capability: '车牌识别、白名单放行、轨迹留痕', apiType: '车牌识别 API、道闸系统 SDK', note: '出入口与月台' },
        { scene: '装卸区安防', capability: '安全帽、入侵、倒地、违停检测', apiType: '目标检测（YOLO 等）云端/边缘推理', note: '需摄像头视角与隐私合规' },
        { scene: '库容与托盘', capability: '货位占用、托盘空置率估计', apiType: '视频分析 / 视觉计数 / 深度相机点云（可选）', note: '替代部分人工盘点' }
      ],
      nlpRows: [
        { scene: '智能客服', capability: '高频查件、派送时间、异常进度等问答与工单引导', apiType: '对话机器人 / Chat API（如千问）、意图识别、槽位填充', note: '可挂 RAG 知识库' },
        { scene: '语音调度', capability: '口述装车完成、发车、改派、异常上报转结构化指令', apiType: 'ASR（如 Whisper API、云 ASR）+ NLU/规则或 LLM 结构化输出', note: '对接本系统「智慧 TMS」' },
        { scene: '单据处理', capability: '非标准运单/合同字段抽取', apiType: '信息抽取 API（NER/关系抽取）、文档理解 + OCR 可选', note: '发货/收货/品名/方量' },
        { scene: '单证与邮件（扩展）', capability: '托书、装箱单、邮件指令解析', apiType: '文档理解、表格 OCR、NER', note: '与 WMS/TMS 主数据对齐' }
      ],
      orRows: [
        { scene: '地图路线/ETA', capability: '单车导航、实时路况、分段旅行时间', apiType: '高德路线规划 / Google Directions 等 REST API', note: '可生成距离矩阵' },
        { scene: 'VRP 复杂约束', capability: '多车、容量、时间窗、装卸顺序', apiType: 'Gurobi、CPLEX、OR-Tools（CP-SAT/路由）', note: 'MIP/VRPTW' },
        { scene: '仓内路径', capability: 'AGV/机器人避障与最短路', apiType: 'A*、D*、时窗路径', note: '本页为栅格示意' },
        { scene: '干线/城配', capability: '配载、限行、时效窗、启发式近似', apiType: '蚁群/遗传/列生成；参见「智慧 TMS」', note: '可衔接地图成本' },
        { scene: '库存与波次', capability: '安全库存、波次切割、储位分配', apiType: '随机规划、MIP、启发式 + 仿真', note: '—' },
        { scene: '多资源排班', capability: '司机、月台、分拣线排程', apiType: '约束规划、排产求解器', note: '—' }
      ],
      predRows: [
        { scene: '需求预测', capability: '未来 1～7 天单量/件量，用于运力与排班', apiType: 'Amazon Forecast、Prophet 云服务、DeepAR、Azure TS 等', note: '历史订单驱动' },
        { scene: 'ETA（ML）', capability: '路况+天气+驾驶习惯特征，分钟级 ETA', apiType: '模型部署 API（如 PAI-EAS）；「智慧 TMS」', note: '见 eta-ml-predict' },
        { scene: 'SKU/线路货量', capability: '细粒度销量或线路货量预测', apiType: 'Prophet、DeepAR、XGBoost、分层时序模型', note: '可下钻至 SKU' },
        { scene: '异常预警', capability: '延误、破损、设备故障前兆', apiType: '异常检测、生存分析、PHM 模型 API', note: '规则+模型' }
      ],
      warehouses: [],
      currentWhId: '',
      selectedZone: null,
      ioForm: { locCode: '', sku: '', qty: 10 },
      ioLogs: [],
      waveForm: { waveNo: '', orderCount: 5, skuCount: 8 },
      sortTasks: [],
      taskIdSeq: 1,
      // path
      mapMode: 'obstacle',
      gridRows: 12,
      gridCols: 20,
      cellSize: 14,
      grid: [],
      start: [1, 1],
      goal: [10, 18],
      stops: [],
      pathCells: [],
      pathResult: ''
    }
  },
  computed: {
    currentZones() {
      const w = this.warehouses.find(x => x.id === this.currentWhId)
      return w ? w.zones : []
    },
    locationRows() {
      if (!this.selectedZone) return []
      return this.selectedZone.locations || []
    }
  },
  created() {
    if (this.embedded) {
      this.activeTab = 'ai3d'
    }
  },
  mounted() {
    this.loadOrInitWms()
    this.initSortDemo()
    this.initGrid()
    this.$nextTick(() => this.drawPathCanvas())
  },
  methods: {
    loadOrInitWms() {
      try {
        const raw = localStorage.getItem(LS_KEY)
        if (raw) {
          const data = JSON.parse(raw)
          this.warehouses = data.warehouses
          this.ioLogs = data.ioLogs || []
          this.currentWhId = this.warehouses[0]?.id || ''
          this.selectedZone = this.warehouses[0]?.zones[0] || null
          return
        }
      } catch (e) {
        /* ignore */
      }
      this.warehouses = [
        {
          id: 'wh1',
          name: '中央仓（华东）',
          zones: [
            {
              id: 'z1',
              code: 'A',
              name: '原料库区',
              type: '存储',
              locations: [
                { locCode: 'A-01-01', sku: 'MAT-001', qty: 120, maxQty: 200, status: '正常' },
                { locCode: 'A-01-02', sku: 'MAT-002', qty: 200, maxQty: 200, status: '满' },
                { locCode: 'A-02-01', sku: '', qty: 0, maxQty: 150, status: '空' }
              ]
            },
            {
              id: 'z2',
              code: 'B',
              name: '成品库区',
              type: '存储',
              locations: [
                { locCode: 'B-01-01', sku: 'FG-100', qty: 80, maxQty: 100, status: '正常' },
                { locCode: 'B-01-02', sku: 'FG-101', qty: 45, maxQty: 100, status: '正常' }
              ]
            },
            {
              id: 'z3',
              code: 'S',
              name: '分拣暂存',
              type: '分拣',
              locations: [
                { locCode: 'S-01-01', sku: '待拣', qty: 30, maxQty: 80, status: '正常' }
              ]
            }
          ]
        }
      ]
      this.currentWhId = 'wh1'
      this.selectedZone = this.warehouses[0].zones[0]
      this.persistWms()
    },
    persistWms() {
      localStorage.setItem(LS_KEY, JSON.stringify({ warehouses: this.warehouses, ioLogs: this.ioLogs }))
    },
    onWhChange() {
      const w = this.warehouses.find(x => x.id === this.currentWhId)
      this.selectedZone = w?.zones[0] || null
    },
    onZoneClick(row) {
      this.selectedZone = row
    },
    findLocByCode(code) {
      for (const w of this.warehouses) {
        for (const z of w.zones) {
          const loc = (z.locations || []).find(l => l.locCode === code)
          if (loc) return { zone: z, loc }
        }
      }
      return null
    },
    stockIn() {
      const { locCode, sku, qty } = this.ioForm
      if (!locCode || !sku || !qty) {
        this.$message.warning('请填写库位、SKU、数量')
        return
      }
      const found = this.findLocByCode(locCode)
      if (!found) {
        this.$message.error('库位不存在')
        return
      }
      const { loc } = found
      const nq = loc.qty + qty
      if (nq > loc.maxQty) {
        this.$message.warning('超过库位容量')
        return
      }
      loc.qty = nq
      if (!loc.sku) loc.sku = sku
      loc.status = loc.qty >= loc.maxQty ? '满' : '正常'
      this.pushLog('入库', locCode, sku, qty)
      this.persistWms()
      this.$message.success('入库成功')
    },
    stockOut() {
      const { locCode, sku, qty } = this.ioForm
      if (!locCode || !qty) {
        this.$message.warning('请填写库位、数量')
        return
      }
      const found = this.findLocByCode(locCode)
      if (!found) {
        this.$message.error('库位不存在')
        return
      }
      const { loc } = found
      if (loc.qty < qty) {
        this.$message.warning('库存不足')
        return
      }
      loc.qty -= qty
      if (loc.qty === 0) {
        loc.sku = ''
        loc.status = '空'
      } else {
        loc.status = loc.qty >= loc.maxQty ? '满' : '正常'
      }
      this.pushLog('出库', locCode, sku || loc.sku, qty)
      this.persistWms()
      this.$message.success('出库成功')
    },
    pushLog(type, locCode, sku, qty) {
      const time = new Date().toLocaleString('zh-CN')
      this.ioLogs.unshift({ time, type, locCode, sku, qty })
      if (this.ioLogs.length > 100) this.ioLogs.pop()
      this.persistWms()
    },
    initSortDemo() {
      this.sortTasks = [
        { id: 'T001', waveNo: 'W20250321001', station: '拣货站-1', skuCount: 6, status: '待分拣' },
        { id: 'T002', waveNo: 'W20250321002', station: '拣货站-2', skuCount: 4, status: '进行中' }
      ]
    },
    createWave() {
      const waveNo = this.waveForm.waveNo || `W${Date.now()}`
      const id = `T${String(this.taskIdSeq++).padStart(3, '0')}`
      this.sortTasks.unshift({
        id,
        waveNo,
        station: `拣货站-${(this.sortTasks.length % 3) + 1}`,
        skuCount: this.waveForm.skuCount,
        status: '待分拣'
      })
      this.$message.success(`波次 ${waveNo} 已生成，任务 ${id}`)
    },
    startSort(row) {
      row.status = '进行中'
      this.$message.info(`任务 ${row.id} 开始分拣`)
    },
    finishSort(row) {
      row.status = '已完成'
      this.$message.success(`任务 ${row.id} 已完成`)
    },
    initGrid() {
      this.grid = []
      for (let r = 0; r < this.gridRows; r++) {
        const row = []
        for (let c = 0; c < this.gridCols; c++) row.push(0)
        this.grid.push(row)
      }
      // 示例障碍
      for (let c = 5; c < 12; c++) this.grid[5][c] = 1
      for (let r = 3; r < 8; r++) this.grid[r][14] = 1
    },
    resetMap() {
      this.initGrid()
      this.stops = []
      this.pathCells = []
      this.pathResult = ''
      this.start = [1, 1]
      this.goal = [10, 18]
      this.drawPathCanvas()
    },
    onCanvasClick(e) {
      const rect = this.$refs.pathCanvas.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top
      const c = Math.floor(x / this.cellSize)
      const r = Math.floor(y / this.cellSize)
      if (r < 0 || r >= this.gridRows || c < 0 || c >= this.gridCols) return
      if (this.mapMode === 'obstacle') {
        this.grid[r][c] = this.grid[r][c] ? 0 : 1
      } else if (this.mapMode === 'start') {
        this.start = [r, c]
      } else if (this.mapMode === 'goal') {
        this.goal = [r, c]
      } else if (this.mapMode === 'stop') {
        this.stops.push([r, c])
      }
      this.pathCells = []
      this.pathResult = ''
      this.drawPathCanvas()
    },
    drawPathCanvas() {
      const canvas = this.$refs.pathCanvas
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      const cs = this.cellSize
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      ctx.fillStyle = '#f5f7fa'
      for (let r = 0; r < this.gridRows; r++) {
        for (let c = 0; c < this.gridCols; c++) {
          ctx.strokeStyle = '#dcdfe6'
          ctx.strokeRect(c * cs, r * cs, cs, cs)
          if (this.grid[r][c] === 1) {
            ctx.fillStyle = '#303133'
            ctx.fillRect(c * cs + 1, r * cs + 1, cs - 2, cs - 2)
          }
        }
      }
      const drawCell = (r, c, color) => {
        ctx.fillStyle = color
        ctx.fillRect(c * cs + 2, r * cs + 2, cs - 4, cs - 4)
      }
      this.pathCells.forEach(([r, c]) => drawCell(r, c, '#409EFF'))
      this.stops.forEach(([r, c]) => drawCell(r, c, '#E6A23C'))
      drawCell(this.start[0], this.start[1], '#67C23A')
      drawCell(this.goal[0], this.goal[1], '#F56C6C')
    },
    runAstar() {
      const res = astarPath(this.grid, this.start, this.goal)
      if (!res) {
        this.pathResult = '无法到达：请检查障碍或起点终点'
        this.pathCells = []
        this.drawPathCanvas()
        this.$message.warning(this.pathResult)
        return
      }
      this.pathCells = res.path
      this.pathResult = `A* 路径步数：${res.length}，路径点数：${res.path.length}`
      this.drawPathCanvas()
      this.$message.success('路径已计算')
    },
    runMultiStop() {
      if (this.stops.length === 0) {
        this.$message.info('请先添加停靠点（模式选「停靠点」并点击地图）')
        return
      }
      const order = nearestNeighborOrder(this.start, [...this.stops])
      let fullPath = []
      let total = 0
      for (let i = 0; i < order.length - 1; i++) {
        const seg = astarPath(this.grid, order[i], order[i + 1])
        if (!seg) {
          this.$message.error('某段路径不可达，请调整障碍')
          return
        }
        if (i > 0) fullPath = fullPath.concat(seg.path.slice(1))
        else fullPath = fullPath.concat(seg.path)
        total += seg.length
      }
      this.pathCells = fullPath
      this.pathResult = `最近邻顺序：${order.length - 1} 个停靠点，累计步数约 ${total}`
      this.drawPathCanvas()
      this.$message.success('多停靠点路径已合并显示')
    }
  },
  watch: {
    pathCells() {
      this.$nextTick(() => this.drawPathCanvas())
    }
  }
}
</script>

<style scoped>
.smart-logistics-page {
  max-width: 1100px;
  margin: 0 auto;
}
.smart-logistics-page--embedded {
  max-width: none;
  margin: 0;
  padding: 0 4px 16px;
}
.smart-logistics-page--embedded >>> .el-tabs--border-card {
  box-shadow: none;
}
.intro-card {
  margin-bottom: 16px;
}

.intro-card h2 {
  margin: 0 0 8px;
  font-size: 1.25rem;
  color: #303133;
}

.intro-text {
  margin: 0;
  color: #606266;
  font-size: 0.9rem;
  line-height: 1.6;
}

.zone-tag {
  margin-left: 8px;
  color: #909399;
  font-size: 13px;
}

.wms-form {
  margin-top: 12px;
}

.path-help {
  margin: 0 0 12px;
  padding-left: 1.2rem;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.map-wrap {
  overflow: auto;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.path-canvas {
  display: block;
  cursor: crosshair;
}

.path-result {
  margin-top: 12px;
  font-size: 13px;
  color: #409eff;
}

.path-api-banner {
  margin-bottom: 16px;
  line-height: 1.65;
}

.ai-api-banner {
  margin-bottom: 16px;
}
.ai-h3 {
  margin: 20px 0 10px;
  font-size: 1.05rem;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}
.ai-h3:first-of-type {
  margin-top: 8px;
}
.ai-lead {
  margin: 0 0 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
.ai-table {
  margin-bottom: 12px;
}
.ai-bullets {
  margin: 0 0 8px;
  padding-left: 1.25rem;
  font-size: 13px;
  color: #606266;
  line-height: 1.75;
}
.ai-bullets li {
  margin-bottom: 8px;
}
</style>
