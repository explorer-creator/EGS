package com.example.controller;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 智能仓储管理系统（WMS）API：
 * 动态货位规划（历史周转 + 贪心分配）、货到人设备与任务队列、数字孪生快照。
 */
@RestController
@RequestMapping("/api/wms-smart")
public class WmsSmartController {

    private static final AtomicLong TASK_SEQ = new AtomicLong(100);
    private final List<Map<String, Object>> taskQueue = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, String> appliedSkuSlot = new ConcurrentHashMap<>();

    /** 出库口参考点（米，用于距离打分） */
    private static final double OUTBOUND_X = 0;
    private static final double OUTBOUND_Y = 0;

    private static final List<Map<String, Object>> HISTORY_LINES = new ArrayList<>();
    private static final List<SlotDef> SLOTS = new ArrayList<>();
    private static final List<EquipmentDef> EQUIPMENT = new ArrayList<>();

    static {
        seedHistory();
    }

    private static void seedHistory() {
        String[][] raw = {
                {"SKU-A100", "耳机单元", "420"},
                {"SKU-B220", "充电宝电芯", "380"},
                {"SKU-C015", "PCB-A", "310"},
                {"SKU-D088", "结构件", "260"},
                {"SKU-E201", "线束", "195"},
                {"SKU-F332", "螺丝包", "150"},
                {"SKU-G009", "标签纸", "120"},
                {"SKU-H445", "泡棉", "95"}
        };
        for (String[] r : raw) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sku", r[0]);
            m.put("name", r[1]);
            m.put("orderLines", Integer.parseInt(r[2]));
            HISTORY_LINES.add(m);
        }
    }

    static {
        // 库位网格：越靠右上（远离原点）距离出库口越远 —— 曼哈顿距离示意
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 5; col++) {
                double x = col * 2.4;
                double y = row * 2.0;
                double dist = Math.abs(x - OUTBOUND_X) + Math.abs(y - OUTBOUND_Y);
                SLOTS.add(new SlotDef("L-" + String.format("%02d-%02d", row, col), row, col, x, y, dist));
            }
        }
        EQUIPMENT.add(new EquipmentDef("AGV-01", "AGV", 88, "IDLE", 3.2, 4.1));
        EQUIPMENT.add(new EquipmentDef("AGV-02", "AGV", 72, "CHARGING", 1.0, 0.5));
        EQUIPMENT.add(new EquipmentDef("SH-01", "SHUTTLE", 91, "MOVING", 8.5, 6.0));
        EQUIPMENT.add(new EquipmentDef("SH-02", "SHUTTLE", 85, "MOVING", 5.0, 3.0));
        EQUIPMENT.add(new EquipmentDef("ARM-01", "ROBOT_ARM", 100, "WORKING", 0.2, 0.3));
        EQUIPMENT.add(new EquipmentDef("ARM-02", "ROBOT_ARM", 96, "IDLE", 0.5, 0.2));
    }

    /** 历史订单行 */
    @GetMapping("/history-orders")
    public Map<String, Object> historyOrders() {
        return Map.of("success", true, "lines", new ArrayList<>(HISTORY_LINES));
    }

    /**
     * 动态货位规划：按历史周转率降序，将 SKU 分配到距出库口更近的货位（贪心匹配）。
     * 返回优化前后估算拣货路径指数及降幅。
     */
    @GetMapping("/slot-optimization")
    public Map<String, Object> slotOptimization() {
        List<Map<String, Object>> skuStats = new ArrayList<>(HISTORY_LINES);
        skuStats.sort((a, b) -> Integer.compare(
                (Integer) b.get("orderLines"),
                (Integer) a.get("orderLines")
        ));

        List<SlotDef> sortedSlots = new ArrayList<>(SLOTS);
        sortedSlots.sort(Comparator.comparingDouble(s -> s.distanceM));

        List<Map<String, Object>> recommendations = new ArrayList<>();
        int n = Math.min(skuStats.size(), sortedSlots.size());
        double baseline = 0, optimized = 0;
        for (int i = 0; i < skuStats.size(); i++) {
            Map<String, Object> sku = skuStats.get(i);
            String skuCode = (String) sku.get("sku");
            int turnover = (Integer) sku.get("orderLines");
            // 假设优化前随机分散：用最远档位的平均距离
            double defaultDist = SLOTS.stream().mapToDouble(s -> s.distanceM).average().orElse(10);
            baseline += turnover * defaultDist;

            if (i < n) {
                SlotDef slot = sortedSlots.get(i);
                optimized += turnover * slot.distanceM;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("sku", skuCode);
                row.put("skuName", sku.get("name"));
                row.put("turnoverIndex", turnover);
                row.put("recommendedSlot", slot.code);
                row.put("distanceToOutboundM", Math.round(slot.distanceM * 10) / 10.0);
                row.put("slotCoord", Map.of("x", slot.x, "y", slot.y));
                row.put("currentAppliedSlot", appliedSkuSlot.getOrDefault(skuCode, "—"));
                row.put("algorithm", "贪心匹配：高周转 SKU → 最短距离货位");
                recommendations.add(row);
            } else {
                optimized += turnover * defaultDist;
            }
        }

        double reductionPct = baseline > 0 ? Math.round((1 - optimized / baseline) * 1000) / 10.0 : 0;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("outboundRef", Map.of("x", OUTBOUND_X, "y", OUTBOUND_Y, "label", "出库口/拣选站"));
        out.put("recommendations", recommendations);
        out.put("metrics", Map.of(
                "baselinePathIndex", Math.round(baseline),
                "optimizedPathIndex", Math.round(optimized),
                "estimatedPathReductionPct", reductionPct,
                "slotCount", SLOTS.size(),
                "skuCount", skuStats.size()
        ));
        out.put("aiNote", "基于历史订单行数作为周转代理指标；实际生产可替换为 ABC 分类与深度学习需求预测。");
        return out;
    }

    /** 应用推荐货位（会话内内存） */
    @PostMapping("/apply-slot-plan")
    public Map<String, Object> applySlotPlan(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> opt = slotOptimization();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rec = (List<Map<String, Object>>) opt.get("recommendations");
        if (rec != null) {
            for (Map<String, Object> r : rec) {
                appliedSkuSlot.put((String) r.get("sku"), (String) r.get("recommendedSlot"));
            }
        }
        return Map.of("success", true, "message", "已应用当前推荐方案（会话内有效）", "applied", appliedSkuSlot.size());
    }

    /** 货到人：AGV / 四向车 / 机械臂状态与统一任务队列 */
    @GetMapping("/goods-to-person")
    public Map<String, Object> goodsToPerson() {
        List<Map<String, Object>> devices = new ArrayList<>();
        for (EquipmentDef e : EQUIPMENT) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id", e.id);
            d.put("type", e.type);
            d.put("typeLabel", typeLabel(e.type));
            d.put("batteryPct", e.battery);
            d.put("state", jitterState(e));
            d.put("position", Map.of("x", round1(e.x), "y", round1(e.y)));
            d.put("currentTask", pickTaskFor(e.id));
            devices.add(d);
        }

        synchronized (taskQueue) {
            if (taskQueue.isEmpty()) {
                pushDemoTasks();
            }
        }

        List<Map<String, Object>> tasks;
        synchronized (taskQueue) {
            tasks = taskQueue.stream().map(t -> new LinkedHashMap<>(t)).collect(Collectors.toList());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("devices", devices);
        out.put("tasks", tasks);
        out.put("summary", Map.of(
                "agvCount", EQUIPMENT.stream().filter(e -> "AGV".equals(e.type)).count(),
                "shuttleCount", EQUIPMENT.stream().filter(e -> "SHUTTLE".equals(e.type)).count(),
                "armCount", EQUIPMENT.stream().filter(e -> "ROBOT_ARM".equals(e.type)).count()
        ));
        return out;
    }

    @PostMapping("/dispatch-task")
    public Map<String, Object> dispatchTask(@RequestBody Map<String, Object> body) {
        String type = str(body.get("taskType"));
        String sku = str(body.get("sku"));
        if (type.isEmpty()) type = "OUTBOUND";
        if (sku.isEmpty()) sku = "SKU-DEMO";
        String id = "T-" + TASK_SEQ.getAndIncrement();
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", id);
        t.put("taskType", type);
        t.put("sku", sku);
        t.put("from", "L-01-01");
        t.put("to", "STATION-P01");
        t.put("status", "QUEUED");
        t.put("priority", 5);
        t.put("createdAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        synchronized (taskQueue) {
            taskQueue.add(0, t);
            if (taskQueue.size() > 30) taskQueue.subList(30, taskQueue.size()).clear();
        }
        return Map.of("success", true, "task", t);
    }

    /** 数字孪生：库区拥堵、设备健康、告警 */
    @GetMapping("/digital-twin")
    public Map<String, Object> digitalTwin() {
        long tick = System.currentTimeMillis() / 8000;
        List<Map<String, Object>> zones = new ArrayList<>();
        String[] names = {"收货区", "高架存储区", "拣选区", "复核打包区", "退货处理区"};
        for (int i = 0; i < names.length; i++) {
            int base = 20 + (i * 13 + (int) (tick % 5) * 7) % 55;
            int congestion = Math.min(100, base + (int) (Math.sin(tick + i) * 15));
            Map<String, Object> z = new LinkedHashMap<>();
            z.put("zoneId", "Z0" + (i + 1));
            z.put("name", names[i]);
            z.put("congestionPct", congestion);
            z.put("throughputPerHour", 80 + i * 12 + (int) (tick % 10));
            z.put("health", congestion > 85 ? "WARN" : congestion > 70 ? "ATTENTION" : "OK");
            zones.add(z);
        }

        List<Map<String, Object>> alerts = new ArrayList<>();
        for (EquipmentDef e : EQUIPMENT) {
            if (e.battery < 25 && "AGV".equals(e.type)) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("level", "WARN");
                a.put("source", e.id);
                a.put("message", "电量偏低，建议调度回充");
                alerts.add(a);
            }
        }
        if (!zones.isEmpty() && (Integer) zones.get(2).get("congestionPct") > 78) {
            alerts.add(Map.of("level", "INFO", "source", "Z03", "message", "拣选区流量偏高，已建议波次错峰"));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        out.put("zones", zones);
        out.put("alerts", alerts);
        out.put("layout", Map.of(
                "widthM", 48,
                "depthM", 32,
                "gridCols", 12,
                "gridRows", 8
        ));
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static String typeLabel(String t) {
        return switch (t) {
            case "AGV" -> "AGV";
            case "SHUTTLE" -> "四向穿梭车";
            case "ROBOT_ARM" -> "机械臂";
            default -> t;
        };
    }

    /** 保持设备表中的主状态，偶发细分态便于看板刷新 */
    private String jitterState(EquipmentDef e) {
        int tick = (int) (System.currentTimeMillis() / 4000);
        if ("CHARGING".equals(e.state)) return "CHARGING";
        if ("WORKING".equals(e.state)) return (tick % 2 == 0) ? "WORKING" : "LOADING";
        return switch ((e.id.hashCode() + tick) % 4) {
            case 0 -> "MOVING";
            case 1 -> "LOADING";
            case 2 -> "UNLOADING";
            default -> "IDLE";
        };
    }

    private String pickTaskFor(String equipId) {
        synchronized (taskQueue) {
            for (Map<String, Object> t : taskQueue) {
                String a = String.valueOf(t.getOrDefault("assignedTo", ""));
                if (equipId.equals(a)) return String.valueOf(t.get("id"));
            }
        }
        return taskQueue.isEmpty() ? "—" : "协同队列";
    }

    private void pushDemoTasks() {
        taskQueue.add(task("T-501", "OUTBOUND", "SKU-A100", "QUEUED", "AGV-01"));
        taskQueue.add(task("T-502", "INBOUND", "SKU-B220", "RUNNING", "SH-01"));
        taskQueue.add(task("T-503", "COUNT", "SKU-C015", "RUNNING", "ARM-01"));
        taskQueue.add(task("T-504", "OUTBOUND", "SKU-D088", "QUEUED", null));
    }

    private Map<String, Object> task(String id, String type, String sku, String status, String assigned) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", id);
        t.put("taskType", type);
        t.put("sku", sku);
        t.put("from", "L-02-03");
        t.put("to", "STATION-P01");
        t.put("status", status);
        t.put("priority", 3);
        t.put("assignedTo", assigned);
        t.put("createdAt", "2025-03-16 10:00:00");
        return t;
    }

    private record SlotDef(String code, int row, int col, double x, double y, double distanceM) {}

    private record EquipmentDef(String id, String type, int battery, String state, double x, double y) {}
}
