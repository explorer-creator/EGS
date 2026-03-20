<template>
  <div class="guide-page">
    <el-card class="guide-card" shadow="hover">
      <div class="guide-header">
        <i class="el-icon-document"></i>
        <h2>软件使用指南</h2>
        <span class="subtitle">各模块功能说明与操作指引</span>
      </div>

      <el-collapse v-model="activeNames">
        <!-- 设备管理 -->
        <el-collapse-item title="设备管理" name="equipment">
          <h4>功能概述</h4>
          <p>设备管理用于维护企业生产设备信息，包括设备编码、名称、厂家、品牌、型号、供应商、生产日期、使用年限、折旧方式、位置、技术参数、备品备件等。</p>

          <h4>导出 CSV</h4>
          <p>点击「导出 CSV」可将当前列表数据导出为 Excel 可打开的 CSV 文件，文件编码为 UTF-8 BOM，中文显示正常。</p>

          <h4>批量删除</h4>
          <p>勾选表格中的多行，点击「批量删除」可一次性删除多条设备记录。删除前会弹出确认框。</p>

          <h4>批量导入</h4>
          <ul>
            <li><strong>CSV 格式</strong>：表头需与导出一致，第一行为列名</li>
            <li><strong>必填项</strong>：设备编码、设备名称</li>
            <li><strong>导入前校验</strong>：系统会校验并提示无效行数量（缺少必填项的行将被跳过）</li>
            <li><strong>确认框</strong>：导入前会弹出确认框，显示将导入的有效条数</li>
          </ul>
          <p><strong>示例 CSV 表头与数据：</strong></p>
          <pre class="code-block">设备编码,设备名称,生产厂家,品牌,规格型号,供应商,生产日期,使用年限,折旧方式,位置,技术参数,备品备件
E001,数控车床,XX厂,XX品牌,CK6140,,2020-01-01,10,直线法,车间A,,,
E002,加工中心,YY厂,YY品牌,VMC850,,2021-06-01,15,直线法,车间B,,,</pre>
          <p class="tip">建议：先导出当前列表，在导出的 CSV 基础上新增或修改数据后再导入，可确保表头格式正确。</p>
        </el-collapse-item>

        <!-- 物料管理 -->
        <el-collapse-item title="物料管理" name="material">
          <h4>功能概述</h4>
          <p>物料管理用于维护物料（零件）信息，包括物料编号、名称、规格型号、库存数量、供应商、分类、版本等。支持物料分类层级管理及 BOM 结构。</p>

          <h4>导出 CSV</h4>
          <p>点击「导出 CSV」可将物料列表导出，含物料编号、物料名称、规格型号、库存数量、供应商、分类、版本。</p>

          <h4>批量删除</h4>
          <p>勾选表格中的多行，点击「批量删除」可一次性删除多条物料记录。删除前会弹出确认框。</p>

          <h4>批量导入</h4>
          <ul>
            <li><strong>CSV 格式</strong>：表头需与导出一致</li>
            <li><strong>必填项</strong>：物料编号、物料名称</li>
            <li><strong>分类列</strong>：可填写分类名称或完整分类路径（如「电子元器件→无源分立元件」），系统会尝试匹配已有分类；留空则不计入分类</li>
            <li><strong>导入前校验</strong>：系统会校验并提示无效行数量</li>
            <li><strong>确认框</strong>：导入前会弹出确认框</li>
          </ul>
          <p><strong>示例 CSV 表头与数据：</strong></p>
          <pre class="code-block">物料编号,物料名称,规格型号,库存数量,供应商,分类,版本
M001,齿轮轴,φ20×100,50,供应商A,结构件→轴类,1.0
M002,轴承,6205,200,供应商B,,1.0</pre>
        </el-collapse-item>

        <!-- 工序管理 -->
        <el-collapse-item title="工序管理" name="procedure">
          <h4>功能概述</h4>
          <p>工序管理用于维护生产工艺工序，如毛坯制造、粗加工、精加工、检测、入库等。支持工序与设备、物料的关联。</p>

          <h4>导出 CSV</h4>
          <p>点击「导出 CSV」可将工序列表导出，含工序编号、工序名称、生产步骤、生产和检测设备、操作人员、开始时间、结束时间、工序描述。</p>

          <h4>批量删除</h4>
          <p>勾选表格中的多行，点击「批量删除」可一次性删除多道工序。删除前会弹出确认框。</p>

          <h4>批量导入</h4>
          <ul>
            <li><strong>CSV 格式</strong>：表头需与导出一致</li>
            <li><strong>必填项</strong>：工序编号、工序名称</li>
            <li><strong>导入前校验</strong>：系统会校验并提示无效行数量</li>
            <li><strong>确认框</strong>：导入前会弹出确认框</li>
          </ul>
          <p><strong>示例 CSV 表头与数据：</strong></p>
          <pre class="code-block">工序编号,工序名称,生产步骤,生产和检测设备,操作人员,开始时间,结束时间,工序描述
P001,毛坯制造,毛坯制造,毛坯制造设备,,,,
P002,粗加工,机械加工（粗加工）,粗加工设备,,,,
P003,精加工,轴类零件加工（精加工）,精加工设备,,,,
P004,检测,检测,检测设备,,,,
P005,入库,入库,-,,,</pre>
          <p class="tip">建议先导出「工序列表」得到标准表头，再按需新增或修改后导入。</p>
        </el-collapse-item>

        <!-- 工艺路线管理 -->
        <el-collapse-item title="工艺路线管理" name="plan">
          <h4>功能概述</h4>
          <p>工艺路线管理用于管理产品工艺路线（如「中心轮零件加工」v1.0），将多道工序按顺序串联形成完整工艺流程。</p>

          <h4>导出 CSV</h4>
          <ul>
            <li><strong>工艺路线列表</strong>：导出工艺编号、工艺名称、所属产品、版本、工艺描述、操作人员、设备使用情况</li>
            <li><strong>工序关联列表</strong>：导出当前选中工艺路线的工序顺序（顺序、工序名称、工序编号）</li>
          </ul>

          <h4>批量删除</h4>
          <p>在工艺路线列表中勾选多行，点击「批量删除」可一次性删除多条工艺路线。删除前会弹出确认框。</p>

          <h4>工序关联</h4>
          <p>选择工艺路线后，在「工序关联」区域按顺序添加工序（如毛坯制造→粗加工→精加工→检测→入库）。可查看工艺路线流程图（简图或 Mermaid）。</p>

          <h4>一键配置</h4>
          <p>点击「一键配置：新增工艺路线「中心轮零件加工」v1.0 并关联 5 道工序」，系统会自动创建工艺路线并关联预设的 5 道工序。需先在工序管理页面完成 5 道工序的新增。</p>
        </el-collapse-item>

        <!-- 通用说明 -->
        <el-collapse-item title="通用说明" name="common">
          <h4>Cookie 认证</h4>
          <p>若遇 403 鉴权失败，请先访问 xDM-F 后端（如 8003 端口）登录，在浏览器 F12 → Network 中复制 Cookie，粘贴到顶部「Cookie（遇 403 必填）」输入框并保存。</p>

          <h4>全局搜索</h4>
          <p>在顶部导航栏搜索框输入关键词，可跨设备、物料、工序、工艺路线进行搜索，点击结果可跳转到对应页面并高亮显示。</p>

          <h4>导出 CSV 编码</h4>
          <p>所有导出的 CSV 均使用 UTF-8 BOM 编码，Excel 可直接打开并正确显示中文。</p>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'GuidePage',
  data() {
    return {
      activeNames: ['equipment', 'material', 'procedure', 'plan', 'common']
    }
  }
}
</script>

<style scoped>
.guide-page {
  max-width: 800px;
  margin: 0 auto;
}
.guide-card {
  padding: 32px 40px;
}
.guide-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 2px solid #165DFF;
}
.guide-header i {
  font-size: 28px;
  color: #165DFF;
}
.guide-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}
.guide-header .subtitle {
  color: #909399;
  font-size: 13px;
  margin-left: 8px;
}
.guide-page h4 {
  margin: 16px 0 8px;
  font-size: 15px;
  color: #303133;
}
.guide-page h4:first-child {
  margin-top: 0;
}
.guide-page p {
  margin: 0 0 12px;
  line-height: 1.6;
  color: #606266;
}
.guide-page ul {
  margin: 0 0 12px;
  padding-left: 20px;
}
.guide-page li {
  margin-bottom: 6px;
  line-height: 1.5;
}
.code-block {
  background: #f5f7fa;
  padding: 12px 16px;
  border-radius: 4px;
  font-size: 13px;
  overflow-x: auto;
  margin: 8px 0 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
.tip {
  color: #909399;
  font-size: 13px;
  margin-top: 8px;
}
</style>
