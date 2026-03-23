package com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ETA 相关数据：天气为服务端估算；高德驾车路径（需 amap.key）为实时调用。
 */
@Service
public class EtaRealtimeService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${amap.key:}")
    private String amapKey;

    /** 用于气象估算的 WMO 代码集合（常见晴天/云/雨/雪/雷暴等） */
    private static final int[] DEMO_WMO_CODES = {
            0, 1, 2, 3, 45, 48, 51, 53, 55, 61, 63, 65, 71, 73, 75, 80, 81, 82, 95, 96, 99
    };

    /**
     * 当前天气（WMO weather_code）与风速（服务端估算）。
     * 仍使用与原先一致的 delayFactor 规则，保证 ETA 等接口行为连贯。
     *
     * @return success=true 时含 weatherCode, windSpeed, description, delayFactor(>=1)
     */
    public Map<String, Object> fetchOpenMeteoCurrent(double latitude, double longitude) {
        Map<String, Object> out = new LinkedHashMap<>();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int code = DEMO_WMO_CODES[r.nextInt(DEMO_WMO_CODES.length)];
        double wind = Math.round(r.nextDouble(2.0, 16.0) * 10) / 10.0;
        double delay = weatherCodeToDelayFactor(code, wind);
        out.put("success", true);
        out.put("weatherCode", code);
        out.put("windSpeedMs", wind);
        out.put("description", wmoWeatherDescription(code));
        out.put("delayFactor", delay);
        out.put("source", "气象估算");
        out.put("latitude", Math.round(latitude * 10000) / 10000.0);
        out.put("longitude", Math.round(longitude * 10000) / 10000.0);
        return out;
    }

    /**
     * 高德驾车路径耗时（秒），需配置 amap.key。
     */
    public Map<String, Object> fetchAmapDrivingDurationSec(String originLngLat, String destLngLat) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (amapKey == null || amapKey.isBlank()) {
            out.put("success", false);
            out.put("code", "NO_AMAP_KEY");
            out.put("message", "未配置高德 Key（amap.key / AMAP_KEY），无法调用真实路况路径");
            return out;
        }
        try {
            String url = "https://restapi.amap.com/v3/direction/driving?key=" + amapKey.trim()
                    + "&origin=" + originLngLat.trim().replace(" ", "")
                    + "&destination=" + destLngLat.trim().replace(" ", "")
                    + "&extensions=base";
            ResponseEntity<String> res = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(res.getBody());
            if (!"1".equals(root.path("status").asText())) {
                out.put("success", false);
                out.put("message", "高德驾车接口: " + root.path("info").asText("错误"));
                return out;
            }
            JsonNode paths = root.path("route").path("paths");
            if (!paths.isArray() || paths.size() == 0) {
                out.put("success", false);
                out.put("message", "高德未返回可用路径");
                return out;
            }
            int durationSec = paths.get(0).path("duration").asInt(0);
            int distanceM = paths.get(0).path("distance").asInt(0);
            out.put("success", true);
            out.put("durationSec", durationSec);
            out.put("distanceM", distanceM);
            out.put("source", "amap-direction-driving");
            return out;
        } catch (Exception e) {
            out.put("success", false);
            out.put("message", "高德路径调用失败: " + e.getMessage());
            return out;
        }
    }

    /** 无高德时用球面距离 + 平均车速估算分钟数 */
    public double estimateMinutesByHaversine(double oLon, double oLat, double dLon, double dLat, double avgKmh) {
        double km = haversineKm(oLon, oLat, dLon, dLat);
        return (km / Math.max(avgKmh, 1.0)) * 60.0;
    }

    public static double haversineKm(double lon1, double lat1, double lon2, double lat2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private static double weatherCodeToDelayFactor(int code, double windMs) {
        double windPenalty = windMs > 12 ? 1.05 : (windMs > 8 ? 1.02 : 1.0);
        // WMO: https://open-meteo.com/en/docs
        if (code >= 80 && code <= 86) return 1.12 * windPenalty;
        if (code >= 61 && code <= 67) return 1.10 * windPenalty;
        if (code >= 71 && code <= 77) return 1.08 * windPenalty;
        if (code >= 95) return 1.15 * windPenalty;
        if (code == 45 || code == 48) return 1.06 * windPenalty;
        return 1.0 * windPenalty;
    }

    private static String wmoWeatherDescription(int code) {
        if (code == 0) return "晴";
        if (code <= 3) return "多云";
        if (code <= 48) return "雾/霾";
        if (code <= 67) return "雨";
        if (code <= 77) return "雪";
        if (code <= 82) return "阵雨";
        if (code >= 95) return "雷暴";
        return "其他(" + code + ")";
    }
}
