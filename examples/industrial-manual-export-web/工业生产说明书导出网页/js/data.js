/**
 * 根据产品关键词匹配行业模板；未匹配时使用通用电子制造绿色流程。
 */
function matchTemplateKey(input) {
  const s = String(input || "").trim().toLowerCase();
  const rules = [
    { key: "headphones", keys: ["耳机", "耳塞", "耳麦", "headphone", "earbud", "tws"] },
    { key: "phone", keys: ["手机", "智能手机", "phone", "smartphone"] },
    { key: "battery", keys: ["电池", "锂电", "battery", "pack"] },
    { key: "textile", keys: ["服装", "纺织", "面料", "textile", "fabric"] },
  ];
  for (const r of rules) {
    if (r.keys.some((k) => s.includes(k.toLowerCase()))) return r.key;
  }
  return "generic";
}

function getTemplate(key) {
  const T = TEMPLATES[key] || TEMPLATES.generic;
  return JSON.parse(JSON.stringify(T));
}

const TEMPLATES = {
  headphones: {
    industry: "消费电子 / 音频",
    flow: [
      { title: "原材料与矿产", desc: "负责任矿产采购、再生塑料与金属比例管控", green: "冲突矿产筛查 · 可追溯批次" },
      { title: "绿色采购", desc: "供应商 ESG 审核、低碳物流与包装减塑", green: "优先本地/铁路运输" },
      { title: "部件制造", desc: "注塑壳体、线缆押出、单元振膜成型", green: "注塑余热回收 · 低 VOC 胶" },
      { title: "电子组装", desc: "PCB SMT、单元焊接、声学测试工位", green: "无铅工艺 · AOI 全检" },
      { title: "总装与调校", desc: "左右单元配对、头梁/充电盒装配、固件烧录", green: "扭矩与声学数据存档" },
      { title: "检测与包装", desc: "电声曲线、续航、防水抽检；环保纸盒与说明书", green: "FSC 纸张 · 可降解袋" },
    ],
    equipment: [
      { name: "立式注塑机", spec: "50–120T，伺服节能", qty: "2 台", note: "壳体与耳塞套" },
      { name: "SMT 贴片线", spec: "0201–0603，双轨", qty: "1 条", note: "主控与电源管理" },
      { name: "回流焊炉", spec: "氮气/无铅", qty: "1 台", note: "与贴片线配套" },
      { name: "电声测试系统", spec: "人工耳 + 音频分析仪", qty: "2 套", note: "频响/THD/相位" },
      { name: "声学消声测试箱", spec: "本底噪声 ≤25dB", qty: "1 套", note: "研发与小批" },
      { name: "自动螺丝机/点胶机", spec: "扭矩可控", qty: "若干", note: "总装工位" },
    ],
    materials: [
      { code: "RM-ABS", name: "ABS/PC 改性粒子", unit: "kg", qty: "按订单", supplier: "认证绿色供应链", remark: "含再生比例可选" },
      { code: "RM-CU", name: "无氧铜线材 / 漆包线", unit: "m", qty: "BOM 展开", supplier: "可追溯", remark: "RoHS" },
      { code: "RM-DRV", name: "动圈/动铁单元", unit: "pcs", qty: "2/耳", supplier: "声学件厂", remark: "配对公差" },
      { code: "RM-PCB", name: "柔性/刚性 PCB 组件", unit: "pcs", qty: "1/副", supplier: "SMT 外协或自制", remark: "无铅" },
      { code: "PKG-BOX", name: "FSC 纸盒 + 大豆油墨印刷", unit: "套", qty: "1/副", supplier: "包装厂", remark: "减塑" },
    ],
    operations: [
      { seq: 10, name: "来料 IQC", workstation: "IQC", std: "AQL 抽样 + RoHS 快检", time: "0.5h/批" },
      { seq: 20, name: "注塑成型", workstation: "注塑", std: "模温/周期/首件确认", time: "按模穴" },
      { seq: 30, name: "SMT 贴片", workstation: "SMT", std: "SPI → 贴片 → AOI", time: "按线体节拍" },
      { seq: 40, name: "单元装配与测试", workstation: "声学", std: "曲线入库、配对筛选", time: "按工艺卡" },
      { seq: 50, name: "总装与老化", workstation: "总装", std: "充放电老化 + 功能检", time: "按订单" },
      { seq: 60, name: "OQC 与包装", workstation: "包装", std: "条码/说明书/防伪", time: "按节拍" },
    ],
    processParams: [
      { item: "注塑", param: "料温 220–250℃ / 模温 50–70℃", target: "尺寸稳定、无飞边", control: "SPC 控制图" },
      { item: "回流焊", param: "峰值 235–245℃", target: "润湿良好、立碑率 <0.3%", control: "炉温曲线每日校验" },
      { item: "电声测试", param: "94dB@1kHz 参考", target: "曲线在公差带内", control: "金样比对 + 数据追溯" },
    ],
    kpis: [
      { label: "综合能效指数", value: "A 级", hint: "对标行业标杆产线" },
      { label: "材料可回收率", value: "≥78%", hint: "壳体+金属+纸基" },
      { label: "一次合格率 (FPY)", value: "≥97%", hint: "关键工位统计" },
      { label: "单位碳排 (估算)", value: "↓12% YoY", hint: "绿电占比提升" },
    ],
    recommendations: [
      "优先提高再生塑料与 FSC 包装比例，在不影响可靠性的前提下降低原生料用量。",
      "对声学测试数据做批次聚类，优化单元配对策略以减少报废。",
      "缩短供应链半径，合并小批量订单以提升设备 OEE 与物流满载率。",
    ],
  },

  phone: {
    industry: "消费电子 / 移动终端",
    flow: [
      { title: "原材料与矿产", desc: "钴锂镍合规、屏幕玻璃与稀土元素溯源", green: "电池护照试点" },
      { title: "绿色采购", desc: "模组供应商碳足迹披露、海运优先", green: "包装去塑" },
      { title: "关键模组", desc: "屏幕贴合、中框 CNC、摄像头 AA 校准", green: "切削液循环" },
      { title: "SMT 与测试", desc: "主板贴片、射频校准、MMI", green: "低能耗老化方案" },
      { title: "总装", desc: "电池装配、防水点胶、整机耦合", green: "精密涂胶量控制" },
      { title: "终检与回收指引", desc: "整机 OQC、以旧换新标签与拆解说明", green: "易拆解设计" },
    ],
    equipment: [
      { name: "高速 SMT 线", spec: "01005 能力", qty: "多条", note: "主板" },
      { name: "自动贴合机", spec: "OCA 全贴合", qty: "按产能", note: "显示模组" },
      { name: "CNC 加工中心", spec: "铝合金中框", qty: "按产能", note: "切削液管理" },
      { name: "摄像头 AA 设备", spec: "6 轴主动对准", qty: "按线体", note: "成像一致性" },
      { name: "气密性测试仪", spec: "IP 等级", qty: "工位级", note: "防水" },
    ],
    materials: [
      { code: "RM-AL", name: "航空铝中框", unit: "pcs", qty: "1", supplier: "型材厂", remark: "阳极氧化" },
      { code: "RM-OLED", name: "OLED 模组", unit: "pcs", qty: "1", supplier: "面板厂", remark: "贴合良率关键" },
      { code: "RM-BAT", name: "锂离子封装电池", unit: "pcs", qty: "1", supplier: "认证供应商", remark: "UN38.3" },
    ],
    operations: [
      { seq: 10, name: "来料检验", workstation: "IQC", std: "关键物料全检/抽检", time: "按批次" },
      { seq: 30, name: "SMT", workstation: "贴片", std: "SPI/AOI/X-Ray", time: "节拍" },
      { seq: 50, name: "模组装配", workstation: "模组", std: "贴合/点胶/AA", time: "工艺卡" },
      { seq: 70, name: "总装与校准", workstation: "总装", std: "射频/传感器校准", time: "订单" },
      { seq: 90, name: "终检出货", workstation: "OQC", std: "MMI + 气密 + 外观", time: "节拍" },
    ],
    processParams: [
      { item: "回流焊", param: "无铅曲线", target: "空洞率受控", control: "每日曲线存档" },
      { item: "防水点胶", param: "胶量 mg 级", target: "IP 等级达标", control: "视觉复检" },
    ],
    kpis: [
      { label: "绿电使用占比", value: "目标 40%+", hint: "可按工厂披露" },
      { label: "包装减塑", value: "同比 -18%", hint: "纸浆模塑等" },
      { label: "一次合格率", value: "≥96%", hint: "整机测试" },
      { label: "返修率", value: "≤1.2%", hint: "售后滚动" },
    ],
    recommendations: [
      "推动模组供应商提供产品碳足迹（PCF）数据，便于整机核算与客户披露。",
      "优化老化与测试并行策略，降低单台能耗与周期时间。",
    ],
  },

  battery: {
    industry: "新能源 / 电池 PACK",
    flow: [
      { title: "原材料", desc: "正极/负极/隔膜/电解液合规采购", green: "供应商环境审计" },
      { title: "电极与电芯", desc: "涂布、辊压、卷绕/叠片、注液化成", green: "NMP 回收、低露点环境" },
      { title: "模组 PACK", desc: "焊接/激光焊、BMS 集成、绝缘测试", green: "烟尘与废气处理" },
      { title: "充放电与分容", desc: "化成、分容、OCV 筛选", green: "能量回馈型设备" },
      { title: "安全与出货", desc: "针刺/挤压抽检（按标准）、UN38.3 资料", green: "追溯码全链路" },
    ],
    equipment: [
      { name: "涂布机 / 辊压机", spec: "连续涂布", qty: "按产能", note: "电极制造" },
      { name: "卷绕或叠片机", spec: "高精度张力", qty: "按产能", note: "电芯成型" },
      { name: "激光焊接机", spec: "模组汇流排", qty: "多台", note: "PACK" },
      { name: "充放电化成柜", spec: "能量回馈", qty: "规模化", note: "节能关键" },
    ],
    materials: [
      { code: "RM-Li", name: "锂盐/电解液", unit: "批次", qty: "按配方", supplier: "化工合规", remark: "危化管控" },
      { code: "RM-CuAl", name: "铜箔/铝箔", unit: "卷", qty: "按订单", supplier: "金属厂", remark: "厚度公差" },
    ],
    operations: [
      { seq: 10, name: "来料检验", workstation: "IQC", std: "材料与批次一致性", time: "按批次" },
      { seq: 40, name: "电极制造", workstation: "电极", std: "涂布面密度 CV", time: "连续" },
      { seq: 70, name: "电芯装配", workstation: "装配", std: "露点/粉尘控制", time: "连续" },
      { seq: 100, name: "化成与分容", workstation: "化成", std: "容量/内阻分选", time: "长周期" },
    ],
    processParams: [
      { item: "干燥房", param: "露点 ≤-40℃", target: "水分控制", control: "在线监测" },
      { item: "焊接", param: "激光功率/速度", target: "焊透无虚焊", control: "首件 + 抽检" },
    ],
    kpis: [
      { label: "能量回馈效率", value: "≥85%", hint: "化成环节" },
      { label: "材料利用率", value: "持续提升", hint: "极片与边角料" },
      { label: "一次合格率", value: "≥98%", hint: "电芯段" },
      { label: "安全事故", value: "0 目标", hint: "危化与热失控" },
    ],
    recommendations: [
      "化成设备尽量采用能量回馈柜，显著降低用电强度。",
      "建立从原材料批次到 PACK 序列号的追溯体系，满足监管与客户审核。",
    ],
  },

  textile: {
    industry: "纺织印染",
    flow: [
      { title: "纤维与纺纱", desc: "棉花/化纤采购、纺纱、络筒", green: "有机棉/再生涤可选" },
      { title: "织造", desc: "梭织/针织、坯布检验", green: "节水织造工艺" },
      { title: "染整", desc: "前处理、染色、定型", green: "低盐低碱、中水回用" },
      { title: "后整理", desc: "功能整理（防水透气等）", green: "有害化学品零排放导向" },
      { title: "裁剪缝制", desc: "裁剪、缝制、整烫", green: "精益排产降低返工" },
      { title: "检验与包装", desc: "成衣检验、挂吊牌、可降解袋", green: "绿色标签披露" },
    ],
    equipment: [
      { name: "纺纱设备", spec: "环锭纺等", qty: "按规模", note: "纤维→纱线" },
      { name: "织机", spec: "喷水/喷气/圆机", qty: "按品类", note: "坯布" },
      { name: "染缸 / 定型机", spec: "温控精确", qty: "染整车间", note: "能耗大户" },
      { name: "裁剪与缝纫线", spec: "自动裁床可选", qty: "缝制车间", note: "面料利用率" },
    ],
    materials: [
      { code: "RM-FIB", name: "棉纱或涤纶丝", unit: "kg", qty: "订单 BOM", supplier: "纱厂", remark: "批次色牢度基础" },
      { code: "DYE", name: "染料与助剂", unit: "kg", qty: "配方", supplier: "化学品合规", remark: "ZDHC 导向" },
    ],
    operations: [
      { seq: 10, name: "来纱检验", workstation: "IQC", std: "强力/条干", time: "按批" },
      { seq: 35, name: "织造", workstation: "织", std: "疵点控制", time: "连续" },
      { seq: 60, name: "染整", workstation: "染整", std: "配方与浴比", time: "批次" },
      { seq: 90, name: "缝制包装", workstation: "成衣", std: "工艺单与针迹密度", time: "订单" },
    ],
    processParams: [
      { item: "染色", param: "温度/时间/pH", target: "色牢度与色差", control: "实验室对样" },
      { item: "定型", param: "温度/风速", target: "门幅与缩水率", control: "首件确认" },
    ],
    kpis: [
      { label: "单位产品取水量", value: "持续下降", hint: "中水回用" },
      { label: "化学品管理", value: "清单化", hint: "ZDHC/MRSL" },
      { label: "一次合格率", value: "≥95%", hint: "染整+成衣" },
      { label: "面料利用率", value: "优化裁剪方案", hint: "排版软件" },
    ],
    recommendations: [
      "染整环节优先采用低盐染色与中水回用，显著降低环境负荷。",
      "通过裁剪排版优化与标准码减少碎片损耗。",
    ],
  },

  generic: {
    industry: "通用离散制造（绿色制造导向）",
    flow: [
      { title: "原材料获取", desc: "矿产/农林/化工原料，合规与可追溯", green: "责任采购政策" },
      { title: "绿色采购与物流", desc: "供应商 ESG、包装减塑、运输优化", green: "合并发运" },
      { title: "加工与制造", desc: "机加/成型/涂装/装配等主流程", green: "节能设备与余热利用" },
      { title: "质量控制", desc: "来料/过程/成品检验与追溯", green: "数字化 SPC" },
      { title: "包装与仓储", desc: "环保材料、条码追溯、先进先出", green: "FSC/再生材料" },
      { title: "交付与改进", desc: "客户交付、售后数据回流、持续改进", green: "PDCA + 碳排监测" },
    ],
    equipment: [
      { name: "数控加工中心", spec: "按零件特征选型", qty: "按产能", note: "精密件" },
      { name: "注塑/压铸设备", spec: "伺服节能", qty: "按产能", note: "成型" },
      { name: "检测设备", spec: "三坐标/影像仪", qty: "实验室/产线", note: "质量保障" },
      { name: "装配线体", spec: "柔性工位", qty: "按订单", note: "人机工程" },
    ],
    materials: [
      { code: "RM-001", name: "主体结构材料", unit: "kg 或 pcs", qty: "按 BOM", supplier: "认证供应商", remark: "RoHS/REACH 视行业" },
      { code: "RM-002", name: "标准件与辅料", unit: "批", qty: "按 BOM", supplier: "多源", remark: "批次一致性" },
      { code: "PKG-001", name: "环保包装", unit: "套", qty: "按出货", supplier: "包装厂", remark: "减塑减体积" },
    ],
    operations: [
      { seq: 10, name: "来料检验 IQC", workstation: "来料", std: "检验指导书", time: "按批次" },
      { seq: 30, name: "关键工序加工", workstation: "生产", std: "作业指导书 + 首件", time: "按工艺" },
      { seq: 60, name: "过程检验 IPQC", workstation: "巡检", std: "控制计划", time: "按班次" },
      { seq: 90, name: "成品检验 OQC", workstation: "出货", std: "抽样标准", time: "按订单" },
    ],
    processParams: [
      { item: "关键过程", param: "依产品定义", target: "CTQ 达成", control: "SPC/防错" },
      { item: "能源管理", param: "单位产出能耗", target: "年度下降目标", control: "分项计量" },
    ],
    kpis: [
      { label: "绿色制造成熟度", value: "持续改进", hint: "对标 GB/T 绿色工厂" },
      { label: "材料可回收/可再生比例", value: "逐年提升", hint: "设计端介入" },
      { label: "一次合格率", value: "≥95%", hint: "全流程" },
      { label: "单位产值综合能耗", value: "同比下降", hint: "节能技改" },
    ],
    recommendations: [
      "将产品名细化到具体品类可获得更贴近行业的设备与工序模板。",
      "建议补充实际产能、良率与供应链数据，以生成可落地的排产与采购建议。",
    ],
  },
};
