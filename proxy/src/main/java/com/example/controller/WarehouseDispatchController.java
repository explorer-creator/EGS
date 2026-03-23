package com.example.controller;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 仓库调度：出仓记录、多仓多中转网络最短路径、节点压力与可行性分析
 */
@RestController
@RequestMapping("/api/warehouse-dispatch")
public class WarehouseDispatchController {

    private static final AtomicLong ID = new AtomicLong(1);
    private final Map<String, OutboundRecord> records = new ConcurrentHashMap<>();

    /** 节点：id -> [名称, 类型 WAREHOUSE/HUB/REGION, 日处理能力(件)] */
    private static final Map<String, NodeMeta> NODES = new LinkedHashMap<>();
    private static final List<Edge> EDGES = new ArrayList<>();

    static {
        NODES.put("W1", new NodeMeta("华东中心仓", "WAREHOUSE", 8000));
        NODES.put("W2", new NodeMeta("华南中心仓", "WAREHOUSE", 6000));
        NODES.put("W3", new NodeMeta("西南中心仓", "WAREHOUSE", 5000));
        NODES.put("H1", new NodeMeta("郑州中转", "HUB", 12000));
        NODES.put("H2", new NodeMeta("武汉中转", "HUB", 10000));
        NODES.put("H3", new NodeMeta("长沙中转", "HUB", 9000));
        NODES.put("H4", new NodeMeta("贵阳中转", "HUB", 7000));
        NODES.put("R1", new NodeMeta("华北片区", "REGION", 999999));
        NODES.put("R2", new NodeMeta("华东片区", "REGION", 999999));
        NODES.put("R3", new NodeMeta("华中片区", "REGION", 999999));
        NODES.put("R4", new NodeMeta("华南片区", "REGION", 999999));
        NODES.put("R5", new NodeMeta("西南片区", "REGION", 999999));

        // 仓 -> 中转（成本=公里量级）
        addE("W1", "H1", 620, 5000);
        addE("W1", "H2", 480, 5000);
        addE("W2", "H2", 520, 4500);
        addE("W2", "H3", 380, 4500);
        addE("W3", "H3", 450, 4000);
        addE("W3", "H4", 280, 4000);
        // 中转互联
        addE("H1", "H2", 350, 8000);
        addE("H2", "H3", 290, 8000);
        addE("H3", "H4", 420, 7000);
        // 中转 -> 片区
        addE("H1", "R1", 400, 6000);
        addE("H1", "R2", 550, 6000);
        addE("H2", "R2", 380, 5500);
        addE("H2", "R3", 220, 5500);
        addE("H3", "R3", 200, 5000);
        addE("H3", "R4", 350, 5000);
        addE("H4", "R4", 400, 4500);
        addE("H4", "R5", 180, 4500);
    }

    private static void addE(String from, String to, int costKm, int edgeCap) {
        EDGES.add(new Edge(from, to, costKm, edgeCap));
    }

    @GetMapping("/network")
    public Map<String, Object> network() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Map.Entry<String, NodeMeta> e : NODES.entrySet()) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", e.getKey());
            n.put("name", e.getValue().name);
            n.put("type", e.getValue().type);
            n.put("dailyCapacity", e.getValue().dailyCapacity);
            nodes.add(n);
        }
        List<Map<String, Object>> links = new ArrayList<>();
        for (Edge ed : EDGES) {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("source", ed.from);
            l.put("target", ed.to);
            l.put("costKm", ed.costKm);
            l.put("edgeCapacity", ed.edgeCap);
            links.add(l);
        }
        return Map.of("success", true, "nodes", nodes, "links", links);
    }

    /** 登记出仓 */
    @PostMapping("/outbound")
    public Map<String, Object> outbound(@RequestBody Map<String, Object> body) {
        String wh = str(body.get("warehouseId"));
        String goods = str(body.get("goodsCode"));
        String dest = str(body.get("destRegionId"));
        double weight = doubleVal(body.get("weightKg"), 1);
        String timeStr = str(body.get("outboundTime"));
        if (wh.isEmpty() || goods.isEmpty() || dest.isEmpty()) {
            return Map.of("success", false, "message", "请填写仓库、货物编码、目的片区");
        }
        if (!NODES.containsKey(wh) || !NODES.get(wh).type.equals("WAREHOUSE")) {
            return Map.of("success", false, "message", "无效的起始仓库");
        }
        if (!NODES.containsKey(dest) || !NODES.get(dest).type.equals("REGION")) {
            return Map.of("success", false, "message", "无效的目的片区");
        }
        String id = "OB-" + ID.getAndIncrement();
        OutboundRecord r = new OutboundRecord();
        r.id = id;
        r.warehouseId = wh;
        r.goodsCode = goods;
        r.destRegionId = dest;
        r.weightKg = weight;
        r.outboundTime = timeStr.isEmpty() ? LocalDateTime.now().toString() : timeStr;
        records.put(id, r);
        return Map.of("success", true, "id", id, "message", "出仓已记录");
    }

    @GetMapping("/records")
    public Map<String, Object> listRecords() {
        List<OutboundRecord> list = new ArrayList<>(records.values());
        list.sort(Comparator.comparing(a -> a.id));
        Collections.reverse(list);
        return Map.of("success", true, "records", list);
    }

    /**
     * 最优路径：Dijkstra 最小成本；结合当前出仓记录估算节点压力
     */
    @GetMapping("/optimize")
    public Map<String, Object> optimize(
            @RequestParam String fromWarehouse,
            @RequestParam String toRegion,
            @RequestParam(defaultValue = "100") double goodsWeightKg
    ) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!NODES.containsKey(fromWarehouse) || !NODES.get(fromWarehouse).type.equals("WAREHOUSE")) {
            out.put("success", false);
            out.put("message", "无效仓库");
            return out;
        }
        if (!NODES.containsKey(toRegion) || !NODES.get(toRegion).type.equals("REGION")) {
            out.put("success", false);
            out.put("message", "无效目的片区");
            return out;
        }
        List<String> path = dijkstra(fromWarehouse, toRegion);
        if (path == null || path.isEmpty()) {
            out.put("success", false);
            out.put("message", "无可达路径，请检查网络");
            return out;
        }
        int totalKm = pathCost(path);
        double truckKmh = 55;
        double hubHours = 2.0 * (path.size() - 2);
        double transitHours = totalKm / truckKmh + hubHours;

        Map<String, Double> loadOnNode = new HashMap<>();
        for (OutboundRecord r : records.values()) {
            List<String> p = dijkstra(r.warehouseId, r.destRegionId);
            if (p == null) continue;
            for (String node : p) {
                if ("REGION".equals(NODES.get(node).type)) continue;
                loadOnNode.merge(node, r.weightKg, Double::sum);
            }
        }
        for (String node : path) {
            if (!"REGION".equals(NODES.get(node).type)) {
                loadOnNode.merge(node, goodsWeightKg, Double::sum);
            }
        }

        List<Map<String, Object>> analysis = new ArrayList<>();
        for (String nodeId : path) {
            NodeMeta meta = NODES.get(nodeId);
            if ("REGION".equals(meta.type)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("nodeId", nodeId);
                row.put("name", meta.name);
                row.put("type", meta.type);
                row.put("pressurePct", 0);
                row.put("feasibilityScore", 100);
                row.put("note", "目的节点，不累计干线压力");
                analysis.add(row);
                continue;
            }
            double load = loadOnNode.getOrDefault(nodeId, 0.0);
            double cap = meta.dailyCapacity;
            double pressure = Math.min(100, (load / Math.max(1, cap)) * 100);
            double feas = Math.max(0, Math.min(100, 100 - pressure - (pressure > 75 ? 8 : 0)));
            String note = pressure > 85 ? "压力偏高，建议分流或增开班次" : pressure > 60 ? "关注排队与装卸时效" : "运力充足";
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("nodeId", nodeId);
            row.put("name", meta.name);
            row.put("type", meta.type);
            row.put("pressurePct", Math.round(pressure * 10) / 10.0);
            row.put("feasibilityScore", Math.round(feas * 10) / 10.0);
            row.put("loadKg", Math.round(load * 10) / 10.0);
            row.put("capacityUnits", cap);
            row.put("note", note);
            analysis.add(row);
        }

        out.put("success", true);
        out.put("path", path);
        List<String> labels = new ArrayList<>();
        for (String id : path) labels.add(NODES.get(id).name);
        out.put("pathLabels", labels);
        out.put("totalCostKm", totalKm);
        out.put("estimateTransitHours", Math.round(transitHours * 10) / 10.0);
        out.put("nodeAnalysis", analysis);
        out.put("goodsWeightKg", goodsWeightKg);
        return out;
    }

    @GetMapping("/stress-summary")
    public Map<String, Object> stressSummary() {
        Map<String, Double> loadOnNode = new HashMap<>();
        for (OutboundRecord r : records.values()) {
            List<String> p = dijkstra(r.warehouseId, r.destRegionId);
            if (p == null) continue;
            for (String node : p) {
                if ("REGION".equals(NODES.get(node).type)) continue;
                loadOnNode.merge(node, r.weightKg, Double::sum);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, NodeMeta> e : NODES.entrySet()) {
            if ("REGION".equals(e.getValue().type)) continue;
            String id = e.getKey();
            double load = loadOnNode.getOrDefault(id, 0.0);
            double cap = e.getValue().dailyCapacity;
            double pressure = Math.min(100, (load / Math.max(1, cap)) * 100);
            double feas = Math.max(0, Math.min(100, 100 - pressure - (pressure > 75 ? 8 : 0)));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("nodeId", id);
            row.put("name", e.getValue().name);
            row.put("type", e.getValue().type);
            row.put("pressurePct", Math.round(pressure * 10) / 10.0);
            row.put("feasibilityScore", Math.round(feas * 10) / 10.0);
            rows.add(row);
        }
        return Map.of("success", true, "stations", rows);
    }

    private int pathCost(List<String> path) {
        int sum = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String a = path.get(i);
            String b = path.get(i + 1);
            sum += edgeCost(a, b);
        }
        return sum;
    }

    private int edgeCost(String a, String b) {
        for (Edge e : EDGES) {
            if (e.from.equals(a) && e.to.equals(b)) return e.costKm;
        }
        return Integer.MAX_VALUE / 4;
    }

    private List<String> dijkstra(String start, String goal) {
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        for (String n : NODES.keySet()) dist.put(n, Integer.MAX_VALUE / 2);
        dist.put(start, 0);
        Set<String> visited = new HashSet<>();
        int n = NODES.size();
        for (int iter = 0; iter < n; iter++) {
            String u = null;
            int best = Integer.MAX_VALUE / 2;
            for (String node : NODES.keySet()) {
                if (visited.contains(node)) continue;
                if (dist.get(node) < best) {
                    best = dist.get(node);
                    u = node;
                }
            }
            if (u == null || best >= Integer.MAX_VALUE / 4) break;
            visited.add(u);
            if (u.equals(goal)) break;
            for (Edge e : EDGES) {
                if (!e.from.equals(u)) continue;
                int nd = dist.get(u) + e.costKm;
                if (nd < dist.get(e.to)) {
                    dist.put(e.to, nd);
                    prev.put(e.to, u);
                }
            }
        }
        if (dist.get(goal) >= Integer.MAX_VALUE / 4) return null;
        LinkedList<String> path = new LinkedList<>();
        String cur = goal;
        while (cur != null) {
            path.addFirst(cur);
            cur = prev.get(cur);
        }
        return new ArrayList<>(path);
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static double doubleVal(Object o, double def) {
        if (o == null) return def;
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return def;
        }
    }

    private static class NodeMeta {
        String name;
        String type;
        int dailyCapacity;

        NodeMeta(String name, String type, int dailyCapacity) {
            this.name = name;
            this.type = type;
            this.dailyCapacity = dailyCapacity;
        }
    }

    private static class Edge {
        String from;
        String to;
        int costKm;
        int edgeCap;

        Edge(String from, String to, int costKm, int edgeCap) {
            this.from = from;
            this.to = to;
            this.costKm = costKm;
            this.edgeCap = edgeCap;
        }
    }

    public static class OutboundRecord {
        public String id;
        public String warehouseId;
        public String goodsCode;
        public String destRegionId;
        public double weightKg;
        public String outboundTime;
    }
}
