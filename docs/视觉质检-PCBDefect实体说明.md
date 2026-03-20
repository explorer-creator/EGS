# 视觉质检 - PCBDefect 实体说明

## 功能概述

视觉质检模块支持 PCB 裸板缺陷识别（漏焊、短路、断路），并将缺陷坐标**实时同步至 xDM-F 元模型**，触发自动报废流程。

## xDM-F 实体设计

在 xDM-F 设计态创建 **PCBDefect** 实体，用于接收缺陷坐标并驱动报废流程。

### PCBDefect 实体属性

| 属性名 | 类型 | 说明 |
|--------|------|------|
| defect_type | String | 缺陷类型：漏焊、短路、断路 |
| defect_x | Number | 缺陷区域左上角 X 坐标 |
| defect_y | Number | 缺陷区域左上角 Y 坐标 |
| defect_width | Number | 缺陷区域宽度 |
| defect_height | Number | 缺陷区域高度 |
| image_id | String | 检测批次/图片 ID |
| status | String | 状态：scrap_pending（待报废） |
| inspection_time | String | 检测时间 |

### 备用实体

若 PCBDefect 未部署，系统会尝试 **DefectInspection** 实体（相同结构）。

## 接入真实 CV 模型

当前为**演示模式**（模拟检测）。可替换为：

### 1. 百度 PaddlePaddle

```java
// 调用 Python PaddleDetection 服务
// 或使用 Paddle Inference Java API
```

### 2. 昇腾 Atlas

```java
// 使用 Atlas CANN / MindSpore 推理
// 需部署 Atlas 硬件环境
```

### 3. ONNX Runtime

```java
// 将 Paddle/Atlas 模型导出为 ONNX
// 使用 org.apache.commons:onnx-runtime 推理
```

在 `CvInspectionController.simulateDefectDetection` 处替换为实际推理调用。

## 报废流程集成

缺陷同步后，可在 xDM-F 中配置工作流：

1. PCBDefect 创建 → 触发报废审批
2. 关联 MaterialManagement 更新库存状态
3. 通知 MES/ERP 系统
