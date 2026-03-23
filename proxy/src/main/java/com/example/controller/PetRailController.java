package com.example.controller;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 爱宠专送 · 铁路出行参考
 * <p>
 * 说明：中国铁路 12306 未对公众开放稳定、完整的第三方票务/托运开放 API。
 * 本模块基于里程、车型均速度与常见计价量级做<strong>参考估算</strong>，并标注车次办理宠物托运的可行性提示，
 * 非 12306 官方实时余票与票价，办理前请以车站与 12306 公告为准。
 */
@RestController
@RequestMapping("/api/pet-rail")
public class PetRailController {

    private static final double EARTH_R_KM = 6371.0;
    private static final Map<String, double[]> STATIONS = new LinkedHashMap<>();

    static {
        STATIONS.put("北京", new double[]{39.9042, 116.4074});
        STATIONS.put("上海", new double[]{31.2304, 121.4737});
        STATIONS.put("广州", new double[]{23.1291, 113.2644});
        STATIONS.put("深圳", new double[]{22.5431, 114.0579});
        STATIONS.put("杭州", new double[]{30.2741, 120.1551});
        STATIONS.put("南京", new double[]{32.0603, 118.7969});
        STATIONS.put("成都", new double[]{30.5728, 104.0668});
        STATIONS.put("重庆", new double[]{29.563, 106.5516});
        STATIONS.put("武汉", new double[]{30.5928, 114.3055});
        STATIONS.put("西安", new double[]{34.3416, 108.9398});
        STATIONS.put("苏州", new double[]{31.2989, 120.5853});
        STATIONS.put("天津", new double[]{39.3434, 117.3616});
        STATIONS.put("郑州", new double[]{34.7466, 113.6254});
        STATIONS.put("长沙", new double[]{28.228, 112.9388});
        STATIONS.put("济南", new double[]{36.6512, 117.12});
        STATIONS.put("青岛", new double[]{36.0671, 120.3826});
        STATIONS.put("沈阳", new double[]{41.8057, 123.4328});
        STATIONS.put("福州", new double[]{26.0745, 119.2965});
        STATIONS.put("厦门", new double[]{24.4798, 118.0819});
        STATIONS.put("昆明", new double[]{25.0406, 102.7123});
        STATIONS.put("哈尔滨", new double[]{45.8038, 126.535});
        STATIONS.put("大连", new double[]{38.914, 121.6147});
    }

    /** 支持的车站列表（与前端下拉一致） */
    @GetMapping("/stations")
    public Map<String, Object> stations() {
        return Map.of("success", true, "stations", new ArrayList<>(STATIONS.keySet()));
    }

    /**
     * 查询参考车次、历时、票价与宠物托运参考费用（估算模型）
     */
    @GetMapping("/query")
    public Map<String, Object> query(
            @RequestParam String fromStation,
            @RequestParam String toStation,
            @RequestParam String date,
            @RequestParam(defaultValue = "5") double petWeightKg
    ) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (fromStation == null || fromStation.isBlank() || toStation == null || toStation.isBlank()) {
            out.put("success", false);
            out.put("message", "请填写出发站与到达站");
            return out;
        }
        fromStation = fromStation.trim();
        toStation = toStation.trim();
        if (fromStation.equals(toStation)) {
            out.put("success", false);
            out.put("message", "出发站与到达站不能相同");
            return out;
        }
        double[] a = STATIONS.get(fromStation);
        double[] b = STATIONS.get(toStation);
        if (a == null || b == null) {
            out.put("success", false);
            out.put("message", "暂不支持的站点，请从列表中选择主要城市车站");
            return out;
        }
        LocalDate d;
        try {
            d = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            out.put("success", false);
            out.put("message", "日期格式应为 YYYY-MM-DD");
            return out;
        }

        double distKm = haversineKm(a[0], a[1], b[0], b[1]) * 1.12;

        int seed = (fromStation + toStation + date).hashCode();
        Random rnd = new Random(seed);

        List<Map<String, Object>> trains = new ArrayList<>();
        trains.add(buildTrain("G", 285, 0.52, distKm, d, rnd, petWeightKg, fromStation, toStation,
                "高铁：一般不办理宠物托运，请以 12306 与车站公告为准；下列票价与历时为出行参考"));
        trains.add(buildTrain("D", 220, 0.48, distKm, d, rnd, petWeightKg, fromStation, toStation,
                "动车：多数不办理宠物托运；票价与历时为参考"));
        trains.add(buildTrain("K", 95, 0.35, distKm, d, rnd, petWeightKg, fromStation, toStation,
                "普速：部分车次可办理宠物托运，需提前咨询出发站"));
        trains.add(buildTrain("Z", 110, 0.38, distKm, d, rnd, petWeightKg, fromStation, toStation,
                "直达特快：普速体系，托运政策以车站为准"));

        out.put("success", true);
        out.put("fromStation", fromStation);
        out.put("toStation", toStation);
        out.put("date", date);
        out.put("distanceKm", Math.round(distKm * 10) / 10.0);
        out.put("petWeightKg", petWeightKg);
        out.put("trains", trains);
        out.put("disclaimer", "本结果为参考估算，非 12306 官方实时数据。票价、余票与宠物托运以中国铁路客户服务中心与车站规定为准。");
        out.put("reference", "12306.cn · 中国铁路客户服务中心");
        return out;
    }

    private Map<String, Object> buildTrain(
            String prefix, double speedKmh, double yuanPerKm, double distKm,
            LocalDate date, Random rnd, double petKg, String from, String to, String policyNote
    ) {
        double durationH = distKm / speedKmh;
        int durationMin = (int) Math.round(durationH * 60);
        if (durationMin < 35) durationMin = 35;

        int hStart = 6 + rnd.nextInt(12);
        int mStart = rnd.nextInt(4) * 15;
        LocalTime dep = LocalTime.of(hStart, mStart);
        LocalDateTime depDt = date.atTime(dep);
        LocalDateTime arrDt = depDt.plusMinutes(durationMin);
        String arriveStr = arrDt.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (!arrDt.toLocalDate().equals(date)) {
            arriveStr = "次日 " + arriveStr;
        }

        int trainNum = 100 + rnd.nextInt(800);
        if ("G".equals(prefix)) trainNum = 1 + rnd.nextInt(399);
        if ("D".equals(prefix)) trainNum = 600 + rnd.nextInt(200);
        String trainNo = prefix + trainNum;

        double ticketYuan = Math.max(12, distKm * yuanPerKm * (0.92 + rnd.nextDouble() * 0.12));
        ticketYuan = Math.round(ticketYuan * 10) / 10.0;

        double petFee = 25 + petKg * 15 + distKm * 0.06;
        petFee = Math.round(petFee * 10) / 10.0;

        double totalRef = ticketYuan + petFee;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("trainNo", trainNo);
        m.put("trainType", prefix.equals("G") ? "高速动车" : prefix.equals("D") ? "动车" : prefix.equals("K") ? "快速" : "直达特快");
        m.put("departTime", dep.format(DateTimeFormatter.ofPattern("HH:mm")));
        m.put("arriveTime", arriveStr);
        m.put("durationMinutes", durationMin);
        m.put("durationText", formatDuration(durationMin));
        m.put("secondClassYuan", ticketYuan);
        m.put("petConsignmentYuan", petFee);
        m.put("totalEstimateYuan", Math.round(totalRef * 10) / 10.0);
        m.put("petPolicyNote", policyNote);
        return m;
    }

    private static String formatDuration(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h <= 0) return m + " 分钟";
        return h + " 小时 " + m + " 分钟";
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = Math.PI / 180;
        double dLat = (lat2 - lat1) * r;
        double dLon = (lon2 - lon1) * r;
        double x = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1 * r) * Math.cos(lat2 * r) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_R_KM * Math.asin(Math.min(1, Math.sqrt(x)));
    }
}
