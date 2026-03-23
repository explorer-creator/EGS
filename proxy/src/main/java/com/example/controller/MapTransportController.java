package com.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 地图与运输路线：代理高德驾车路径规划（Web 服务 key 放在服务端，避免前端泄露）。
 * 未配置 amap.key 时返回提示，前端可降级为直线示意。
 */
@RestController
@RequestMapping("/api/map")
public class MapTransportController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${amap.key:}")
    private String amapKey;

    /**
     * 驾车路径规划（高德 v3）
     * @param origin 起点 "经度,纬度"
     * @param destination 终点 "经度,纬度"
     * @param waypoints 可选途经点，多个用 | 分隔，每点为 "经度,纬度"
     */
    @GetMapping("/driving")
    public Map<String, Object> driving(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(required = false) String waypoints
    ) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (amapKey == null || amapKey.isBlank()) {
            out.put("success", false);
            out.put("code", "NO_KEY");
            out.put("message", "未配置高德 Web 服务 Key。请在 proxy application.yml 设置 amap.key 或环境变量 AMAP_KEY");
            return out;
        }
        try {
            StringBuilder sb = new StringBuilder("https://restapi.amap.com/v3/direction/driving?key=");
            sb.append(amapKey.trim());
            sb.append("&origin=").append(origin.trim().replace(" ", ""));
            sb.append("&destination=").append(destination.trim().replace(" ", ""));
            sb.append("&extensions=all");
            if (waypoints != null && !waypoints.isBlank()) {
                sb.append("&waypoints=").append(waypoints.trim().replace(" ", ""));
            }
            ResponseEntity<String> res = restTemplate.getForEntity(sb.toString(), String.class);
            JsonNode root = objectMapper.readTree(res.getBody());
            if (!"1".equals(root.path("status").asText())) {
                out.put("success", false);
                out.put("message", root.path("info").asText("高德接口错误"));
                return out;
            }
            JsonNode paths = root.path("route").path("paths");
            if (!paths.isArray() || paths.size() == 0) {
                out.put("success", false);
                out.put("message", "无可用路径");
                return out;
            }
            JsonNode path0 = paths.get(0);
            List<List<Double>> coords = new ArrayList<>();
            for (JsonNode step : path0.path("steps")) {
                String poly = step.path("polyline").asText("");
                for (String seg : poly.split(";")) {
                    if (seg.isEmpty()) continue;
                    String[] p = seg.split(",");
                    if (p.length >= 2) {
                        coords.add(Arrays.asList(Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim())));
                    }
                }
            }
            out.put("success", true);
            out.put("distance", path0.path("distance").asInt(0));
            out.put("duration", path0.path("duration").asInt(0));
            out.put("path", coords);
            return out;
        } catch (Exception e) {
            out.put("success", false);
            out.put("message", e.getMessage());
            return out;
        }
    }

    /** 健康检查：是否已配置 Key（不返回 key 本身） */
    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("amapConfigured", amapKey != null && !amapKey.isBlank());
        return m;
    }
}
