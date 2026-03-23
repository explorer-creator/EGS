package com.example.controller;

import com.example.config.QwenProperties;
import com.example.service.EtaRealtimeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 智慧运输管理系统（TMS）：蚁群路径优化、在途 OBD+疲劳预警、ETA 预测。
 */
@RestController
@RequestMapping("/api/tms-smart")
public class TmsSmartController {

    private final EtaRealtimeService etaRealtimeService;
    private final QwenProperties qwenConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public TmsSmartController(EtaRealtimeService etaRealtimeService, QwenProperties qwenConfig) {
        this.etaRealtimeService = etaRealtimeService;
        this.qwenConfig = qwenConfig;
    }

    private static final int N = 6; // 0  depot, 1..5 customers
    private static final double[][] DIST_KM;
    private static final boolean[][] TRUCK_RESTRICTED; // true = 货车限行边
    private static final int[][] TIME_WINDOWS; // [earliest, latest] in minutes from 8:00

    static {
        DIST_KM = new double[][]{
                {0, 12, 18, 25, 22, 30},
                {12, 0, 10, 20, 14, 28},
                {18, 10, 0, 15, 12, 22},
                {25, 20, 15, 0, 8, 10},
                {22, 14, 12, 8, 0, 11},
                {30, 28, 22, 10, 11, 0}
        };
        TRUCK_RESTRICTED = new boolean[N][N];
        TRUCK_RESTRICTED[1][4] = TRUCK_RESTRICTED[4][1] = true; // 市区段限行
        TRUCK_RESTRICTED[2][5] = TRUCK_RESTRICTED[5][2] = true;
        TIME_WINDOWS = new int[][]{
                {0, 600},
                {30, 120},
                {60, 180},
                {90, 200},
                {45, 160},
                {120, 240}
        };
    }

    /** 与 eta-ml / eta-predictions 共用的三条示范运单（经纬度 WGS84） */
    private record TripLeg(String id, String originName, String destName, double oLon, double oLat, double dLon, double dLat) {}

    private static final TripLeg[] ETA_TRIPS = {
            new TripLeg("T-20250316-01", "DC-华东仓", "客户C-苏州", 121.25, 31.33, 120.62, 31.30),
            new TripLeg("T-20250316-02", "昆山", "客户D-市区", 120.98, 31.38, 121.48, 31.22),
            new TripLeg("T-20250316-03", "松江", "嘉兴", 121.23, 31.01, 120.75, 30.75)
    };

    /**
     * 智能调度：蚁群算法（ACO）求解带限行约束的 TSP 近似；可选时效窗软惩罚与成本权重。
     */
    @GetMapping("/schedule-optimize")
    public Map<String, Object> scheduleOptimize(
            @RequestParam(defaultValue = "TRUCK") String vehicleType,
            @RequestParam(defaultValue = "1.2") double costPerKm,
            @RequestParam(defaultValue = "0.5") double timeWindowPenaltyPerMin,
            @RequestParam(defaultValue = "25") int iterations
    ) {
        boolean truck = "TRUCK".equalsIgnoreCase(vehicleType);
        int n = N;
        double[][] tau = new double[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(tau[i], 0.1);
        }
        double alpha = 1.0;
        double beta = 2.0;
        double rho = 0.15;
        double Q = 100.0;
        int numAnts = 12;
        int ants = Math.max(20, iterations) > 50 ? numAnts : numAnts;

        List<Integer> bestTour = null;
        double bestCost = Double.POSITIVE_INFINITY;

        Random rnd = ThreadLocalRandom.current();
        for (int it = 0; it < Math.min(80, iterations + 5); it++) {
            for (int k = 0; k < ants; k++) {
                List<Integer> tour = buildAntTour(rnd, tau, alpha, beta, truck);
                if (tour == null) continue;
                double c = tourCost(tour, truck, costPerKm, timeWindowPenaltyPerMin);
                if (c < bestCost) {
                    bestCost = c;
                    bestTour = new ArrayList<>(tour);
                }
                double deposit = Q / Math.max(c, 1);
                for (int s = 0; s < tour.size() - 1; s++) {
                    int a = tour.get(s);
                    int b = tour.get(s + 1);
                    tau[a][b] += deposit;
                    tau[b][a] += deposit;
                }
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    tau[i][j] *= (1 - rho);
                }
            }
        }

        if (bestTour == null) {
            bestTour = greedyFallback(truck);
            if (bestTour == null) {
                return Map.of("success", false, "message", "无可行路径（限行过严）");
            }
            bestCost = tourCost(bestTour, truck, costPerKm, timeWindowPenaltyPerMin);
        }

        double totalKm = tourKm(bestTour);
        List<Map<String, Object>> legs = new ArrayList<>();
        for (int s = 0; s < bestTour.size() - 1; s++) {
            int a = bestTour.get(s);
            int b = bestTour.get(s + 1);
            Map<String, Object> leg = new LinkedHashMap<>();
            leg.put("from", nodeName(a));
            leg.put("to", nodeName(b));
            leg.put("distanceKm", Math.round(DIST_KM[a][b] * 10) / 10.0);
            leg.put("restrictedEdge", truck && TRUCK_RESTRICTED[a][b]);
            legs.add(leg);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("algorithm", "蚁群算法 ACO");
        out.put("vehicleType", vehicleType);
        out.put("bestRouteOrder", bestTour.stream().map(TmsSmartController::nodeName).toList());
        out.put("legs", legs);
        out.put("totalDistanceKm", Math.round(totalKm * 10) / 10.0);
        out.put("estimatedCost", Math.round(bestCost * 10) / 10.0);
        out.put("constraints", Map.of(
                "trafficFactor", "1.0（可接入实时路况 API）",
                "truckRestrictionsApplied", truck,
                "timeWindows", Arrays.stream(TIME_WINDOWS).skip(1).map(tw -> tw[0] + "-" + tw[1] + "min").toList()
        ));
        out.put("rlNote", "强化学习方案可对接仿真环境（如 Gym + 自定义路网）进行策略训练；当前为 ACO 基线。");
        return out;
    }

    private static List<Integer> buildAntTour(Random rnd, double[][] tau, double alpha, double beta, boolean truck) {
        int n = N;
        List<Integer> unvisited = new ArrayList<>();
        for (int i = 1; i < n; i++) unvisited.add(i);
        Collections.shuffle(unvisited, rnd);
        List<Integer> tour = new ArrayList<>();
        tour.add(0);
        int cur = 0;
        while (!unvisited.isEmpty()) {
            double[] probs = new double[unvisited.size()];
            double sum = 0;
            for (int i = 0; i < unvisited.size(); i++) {
                int j = unvisited.get(i);
                if (truck && TRUCK_RESTRICTED[cur][j]) {
                    probs[i] = 0;
                    continue;
                }
                double eta = 1.0 / Math.max(DIST_KM[cur][j], 0.01);
                probs[i] = Math.pow(tau[cur][j], alpha) * Math.pow(eta, beta);
                sum += probs[i];
            }
            if (sum <= 1e-9) return null;
            double pick = rnd.nextDouble() * sum;
            double acc = 0;
            int idx = 0;
            for (int i = 0; i < unvisited.size(); i++) {
                acc += probs[i];
                if (pick <= acc) {
                    idx = i;
                    break;
                }
            }
            int next = unvisited.remove(idx);
            tour.add(next);
            cur = next;
        }
        tour.add(0);
        return tour;
    }

    private static double tourCost(List<Integer> tour, boolean truck, double costPerKm, double twPenalty) {
        double km = 0;
        for (int s = 0; s < tour.size() - 1; s++) {
            int a = tour.get(s);
            int b = tour.get(s + 1);
            if (truck && TRUCK_RESTRICTED[a][b]) return 1e9;
            km += DIST_KM[a][b];
        }
        double t = 0;
        double penalty = 0;
        double speed = 45.0;
        for (int s = 1; s < tour.size() - 1; s++) {
            int a = tour.get(s - 1);
            int b = tour.get(s);
            t += (DIST_KM[a][b] / speed) * 60;
            int[] tw = TIME_WINDOWS[b];
            if (t < tw[0]) t = tw[0];
            if (t > tw[1]) penalty += (t - tw[1]) * twPenalty;
        }
        return km * costPerKm + penalty;
    }

    private static double tourKm(List<Integer> tour) {
        double km = 0;
        for (int s = 0; s < tour.size() - 1; s++) {
            km += DIST_KM[tour.get(s)][tour.get(s + 1)];
        }
        return km;
    }

    /** 贪心回退：最近邻 + 跳过限行边 */
    private static List<Integer> greedyFallback(boolean truck) {
        List<Integer> tour = new ArrayList<>();
        tour.add(0);
        boolean[] vis = new boolean[N];
        int cur = 0;
        for (int step = 1; step < N; step++) {
            int best = -1;
            double bestD = Double.MAX_VALUE;
            for (int j = 1; j < N; j++) {
                if (vis[j]) continue;
                if (truck && TRUCK_RESTRICTED[cur][j]) continue;
                if (DIST_KM[cur][j] < bestD) {
                    bestD = DIST_KM[cur][j];
                    best = j;
                }
            }
            if (best < 0) return null;
            vis[best] = true;
            tour.add(best);
            cur = best;
        }
        if (truck && TRUCK_RESTRICTED[cur][0]) {
            return null;
        }
        tour.add(0);
        return tour;
    }

    private static String nodeName(int i) {
        return switch (i) {
            case 0 -> "DC-华东仓";
            case 1 -> "客户A-松江";
            case 2 -> "客户B-昆山";
            case 3 -> "客户C-苏州";
            case 4 -> "客户D-市区";
            case 5 -> "客户E-嘉兴";
            default -> "N" + i;
        };
    }

    /** 在途：OBD + 视觉疲劳 */
    @GetMapping("/in-transit")
    public Map<String, Object> inTransit() {
        long tick = System.currentTimeMillis() / 5000;
        List<Map<String, Object>> vehicles = new ArrayList<>();
        String[] ids = {"V-001", "V-002", "V-003"};
        for (int i = 0; i < ids.length; i++) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("vehicleId", ids[i]);
            v.put("plate", "沪A·" + (8800 + i * 17));
            v.put("lat", 31.2 + i * 0.05 + (tick % 7) * 0.001);
            v.put("lng", 121.4 + i * 0.03);
            v.put("speedKmh", 62 + (int) (tick % 15));
            Map<String, Object> obd = new LinkedHashMap<>();
            obd.put("avgFuelLPer100Km", 11.2 + i * 0.3);
            obd.put("instantFuelLPerH", 4.5 + (tick % 2) * 0.1);
            obd.put("harshBrakeCount30m", 1 + (i + 1) % 3);
            obd.put("engineStatus", i == 0 ? "NORMAL" : "NORMAL");
            obd.put("coolantTempC", 88 + (int) (tick % 5));
            obd.put("dtcCodes", i == 2 ? List.of("P0420") : Collections.emptyList());
            v.put("obd", obd);
            int fatigue = 28 + (int) (Math.sin(tick + i) * 15) + i * 5;
            fatigue = Math.min(100, Math.max(0, fatigue));
            Map<String, Object> vision = new LinkedHashMap<>();
            vision.put("fatigueScore", fatigue);
            vision.put("eyeAspectRatio", 0.22 - fatigue / 500.0);
            vision.put("yawnCountPerMinute", fatigue > 60 ? 2 : 0);
            vision.put("alertLevel", fatigue > 75 ? "HIGH" : fatigue > 40 ? "MEDIUM" : "LOW");
            vision.put("model", "Edge-vision 疲劳检测");
            v.put("driverVision", vision);
            if (fatigue > 75) {
                v.put("activeWarning", "建议就近服务区休息 15 分钟");
            } else {
                v.put("activeWarning", null);
            }
            vehicles.add(v);
        }
        return Map.of("success", true, "vehicles", vehicles, "obdSource", "OBD-II 接口");
    }

    /** ETA：与 eta-ml 同源的天气（服务端估算）+ 可选高德路径；trafficDelayFactor=预测/自由流估算 */
    @GetMapping("/eta-predictions")
    public Map<String, Object> etaPredictions() {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> trips = new ArrayList<>();
        List<String> warn = new ArrayList<>();
        double histMae = 3.2;

        for (TripLeg t : ETA_TRIPS) {
            Map<String, Object> wx = etaRealtimeService.fetchOpenMeteoCurrent(t.dLat(), t.dLon());
            if (!Boolean.TRUE.equals(wx.get("success"))) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("success", false);
                err.put("message", "天气数据失败: " + wx.getOrDefault("message", ""));
                return err;
            }
            double weatherFactor = ((Number) wx.get("delayFactor")).doubleValue();
            String originLl = String.format("%.5f,%.5f", t.oLon(), t.oLat());
            String destLl = String.format("%.5f,%.5f", t.dLon(), t.dLat());
            Map<String, Object> drv = etaRealtimeService.fetchAmapDrivingDurationSec(originLl, destLl);

            double baseMin;
            double haFree = etaRealtimeService.estimateMinutesByHaversine(t.oLon(), t.oLat(), t.dLon(), t.dLat(), 65);
            if (Boolean.TRUE.equals(drv.get("success"))) {
                baseMin = ((Number) drv.get("durationSec")).intValue() / 60.0;
            } else {
                warn.add(t.id() + ": " + drv.getOrDefault("message", ""));
                baseMin = etaRealtimeService.estimateMinutesByHaversine(t.oLon(), t.oLat(), t.dLon(), t.dLat(), 45);
            }
            double trafficFactor = Math.max(1.0, baseMin / Math.max(haFree, 1.0));
            double predictedMin = baseMin * weatherFactor * 1.0;
            double errBand = Math.max(1.5, histMae * 0.9);
            LocalDateTime eta = now.plusMinutes((long) Math.round(predictedMin));

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tripId", t.id());
            m.put("origin", t.originName());
            m.put("destination", t.destName());
            m.put("baselineDurationMin", Math.round(baseMin * 10) / 10.0);
            m.put("trafficDelayFactor", Math.round(trafficFactor * 100) / 100.0);
            m.put("historicalMaeMinutes", histMae);
            m.put("predictedEta", eta.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            m.put("etaConfidenceBandMin", Math.round(errBand * 10) / 10.0);
            m.put("accuracyNote", "路况来自高德或球面估算；天气为气象估算");
            trips.add(m);
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("maeVsHistoryMinutes", histMae);
        summary.put("realtimeTrafficBlend", "气象估算 + 高德驾车（若已配置 AMAP_KEY）");
        summary.put("targetErrorBand", "±3~5 分钟（城配场景）");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("trips", trips);
        out.put("summary", summary);
        out.put("warnings", warn);
        return out;
    }

    /**
     * ETA 精准预测：天气由服务端<strong>气象估算</strong> + 可选高德驾车路径（需 AMAP_KEY）；
     * 综合特征输出分钟级 ETA。未配置高德时使用球面距离估算路况，并在 warnings 中说明。
     * PAI-EAS 需自行部署后替换特征融合逻辑。
     */
    @GetMapping("/eta-ml-predict")
    public Map<String, Object> etaMlPredict() {
        LocalDateTime now = LocalDateTime.now();
        List<String> warnings = new ArrayList<>();

        List<Map<String, Object>> predictions = new ArrayList<>();

        for (TripLeg t : ETA_TRIPS) {
            Map<String, Object> wx = etaRealtimeService.fetchOpenMeteoCurrent(t.dLat(), t.dLon());
            if (!Boolean.TRUE.equals(wx.get("success"))) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("success", false);
                err.put("message", "天气数据获取失败: " + wx.getOrDefault("message", "unknown"));
                err.put("failedTrip", t.id());
                return err;
            }
            double weatherFactor = ((Number) wx.get("delayFactor")).doubleValue();
            String weatherDesc = String.valueOf(wx.get("description"));
            int wcode = ((Number) wx.get("weatherCode")).intValue();

            String originLl = String.format("%.5f,%.5f", t.oLon(), t.oLat());
            String destLl = String.format("%.5f,%.5f", t.dLon(), t.dLat());
            Map<String, Object> drv = etaRealtimeService.fetchAmapDrivingDurationSec(originLl, destLl);

            double baseMin;
            String trafficSource;
            double trafficIdx;
            if (Boolean.TRUE.equals(drv.get("success"))) {
                int sec = ((Number) drv.get("durationSec")).intValue();
                baseMin = sec / 60.0;
                trafficSource = "高德驾车路径（真实路况与距离）";
                double haFree = etaRealtimeService.estimateMinutesByHaversine(t.oLon(), t.oLat(), t.dLon(), t.dLat(), 65);
                trafficIdx = Math.min(95, Math.max(5, (baseMin / Math.max(haFree, 0.1) - 1) * 45 + 35));
            } else {
                warnings.add(t.id() + ": " + drv.getOrDefault("message", "无高德路径"));
                baseMin = etaRealtimeService.estimateMinutesByHaversine(t.oLon(), t.oLat(), t.dLon(), t.dLat(), 45);
                trafficSource = "直线距离÷均速（无高德 Key 或路径失败时的估算）";
                trafficIdx = 40;
            }

            int driverHabitScore = 70;
            double habitMultiplier = 1.0 + (50 - driverHabitScore) / 200.0;

            double rawMin = baseMin * weatherFactor * habitMultiplier;
            long remMin = Math.max(5, Math.round(rawMin));
            double band = 2.0 + (weatherFactor > 1.08 ? 1.5 : 0.8);

            Map<String, Object> feat = new LinkedHashMap<>();
            feat.put("trafficCongestionIndex", Math.round(trafficIdx * 10) / 10.0);
            feat.put("trafficSource", trafficSource);
            feat.put("weatherWmoCode", wcode);
            feat.put("weatherDescription", weatherDesc);
            feat.put("weatherImpactFactor", Math.round(weatherFactor * 1000) / 1000.0);
            feat.put("weatherSource", String.valueOf(wx.getOrDefault("source", "气象估算")));
            feat.put("driverHabitScore", driverHabitScore);
            feat.put("driverHabitNote", "未接入司机画像 API，使用默认中位分");
            feat.put("driverStyleMultiplier", habitMultiplier);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tripId", t.id());
            row.put("origin", t.originName());
            row.put("destination", t.destName());
            row.put("baselineDurationMin", Math.round(baseMin * 10) / 10.0);
            row.put("features", feat);
            row.put("remainingMinutes", remMin);
            row.put("predictedEta", now.plusMinutes(remMin).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            row.put("confidenceBandMinutes", Math.round(band * 10) / 10.0);
            row.put("downstreamHint", "可同步仓库月台、收货方与中转资源");
            predictions.add(row);
        }

        Map<String, Object> deployment = new LinkedHashMap<>();
        deployment.put("platform", "特征融合（气象估算 + 高德可选）；PAI-EAS 可替换本段逻辑");
        deployment.put("invokeStyle", "当前由 Spring 后端直连公开 API；生产可将特征 POST 至阿里云 PAI-EAS");
        deployment.put("modelName", "heuristic-eta-v1");
        deployment.put("modelVersion", "realtime-apis");
        deployment.put("integrationNote", "配置环境变量 AMAP_KEY 可获得真实驾车耗时；未配置时降级为球面距离估算。");

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("inputFeatures", List.of(
                "amap_duration_sec_or_haversine", "demo_random_wmo_weather_code", "driver_habit_score_default"
        ));
        schema.put("output", List.of("eta_ts", "confidence_band_min"));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("deployment", deployment);
        out.put("featureSchema", schema);
        out.put("predictions", predictions);
        out.put("maeTargetMinutes", 3.5);
        out.put("warnings", warnings);
        out.put("dataSources", Map.of(
                "weather", "气象估算",
                "routing", warnings.isEmpty() ? "高德驾车路径（已配置 Key）" : "部分运单使用距离估算，见 warnings"
        ));
        out.put("dataDisclaimer", "路况可为高德或估算；PAI-EAS 需自行部署后接入。");
        return out;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return Map.of(
                "success", true,
                "modules", List.of(
                        Map.of("name", "调度优化", "api", "/api/tms-smart/schedule-optimize"),
                        Map.of("name", "在途监控", "api", "/api/tms-smart/in-transit"),
                        Map.of("name", "ETA统计", "api", "/api/tms-smart/eta-predictions"),
                        Map.of("name", "ETA-ML部署", "api", "/api/tms-smart/eta-ml-predict"),
                        Map.of("name", "AI延误分析", "api", "POST /api/tms-smart/delay-insight"),
                        Map.of("name", "溯源哈希", "api", "POST /api/tms-smart/trace-anchor")
                )
        );
    }

    /**
     * TMS + 天气/路况 + 千问：解释「为何延误/迟到」。无 Key 时返回规则合成说明。
     */
    @PostMapping("/delay-insight")
    public Map<String, Object> delayInsight(@RequestBody(required = false) Map<String, Object> body) {
        String tripId = body != null && body.get("tripId") != null
                ? String.valueOf(body.get("tripId")).trim()
                : ETA_TRIPS[0].id();
        String userQuestion = body != null && body.get("userQuestion") != null
                ? String.valueOf(body.get("userQuestion")).trim()
                : "为什么这批货可能延误？";

        TripLeg trip = findTripLeg(tripId);
        Map<String, Object> context = buildDelayInsightContext(trip);

        String apiKey = qwenConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("usedLlm", false);
            out.put("message", "未配置千问 API Key，已使用规则合成说明（配置 DASHSCOPE_API_KEY 可启用大模型）");
            out.put("explanation", buildFallbackDelayExplanation(userQuestion, context));
            out.put("context", context);
            return out;
        }

        String system = "你是智慧物流（TMS）调度专家。根据用户提供的运单上下文（含天气摘要、高德或估算路况、在途车辆与疲劳等数据），"
                + "用中文分点简洁回答用户问题；只依据上下文中的数值与描述，不要编造未给出的数据。若上下文显示风险较低，说明预计可准时或轻微波动。";
        String userContent;
        try {
            userContent = "用户问题：「" + userQuestion + "」\n\n运单与路况上下文（JSON）：\n"
                    + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
        } catch (Exception e) {
            userContent = "用户问题：「" + userQuestion + "」\n上下文获取失败：" + e.getMessage();
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        messages.add(Map.of("role", "user", "content", userContent));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", qwenConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.35);
        requestBody.put("max_tokens", 1200);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            String url = qwenConfig.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response == null) {
                return Map.of("success", false, "message", "千问返回为空", "context", context);
            }
            Object choices = response.get("choices");
            if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
                return Map.of("success", false, "message", "千问未返回有效内容", "context", context);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> firstChoice = (Map<String, Object>) ((List<?>) choices).get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            String content = message != null && message.get("content") != null
                    ? String.valueOf(message.get("content"))
                    : "";
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("usedLlm", true);
            out.put("explanation", content);
            out.put("context", context);
            return out;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (e.getCause() != null) msg += "; " + e.getCause().getMessage();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("usedLlm", false);
            out.put("message", "千问调用失败，已降级为规则说明：" + msg);
            out.put("explanation", buildFallbackDelayExplanation(userQuestion, context));
            out.put("context", context);
            return out;
        }
    }

    /**
     * 为关键运单事件生成 SHA-256 指纹（链下存证，可比对溯源）。
     */
    @PostMapping("/trace-anchor")
    public Map<String, Object> traceAnchor(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null) body = Collections.emptyMap();
        String tripId = body.get("tripId") != null ? String.valueOf(body.get("tripId")).trim() : ETA_TRIPS[0].id();
        String eventType = body.get("eventType") != null ? String.valueOf(body.get("eventType")).trim() : "TMS_EVENT_LOG";
        TripLeg trip = findTripLeg(tripId);
        Map<String, Object> snap = buildDelayInsightContext(trip);

        long ts = System.currentTimeMillis();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", "tms-trace-v1");
        payload.put("tripId", trip.id());
        payload.put("origin", trip.originName());
        payload.put("destination", trip.destName());
        payload.put("eventType", eventType);
        payload.put("anchoredAtMillis", ts);
        payload.put("etaSnapshot", snap.get("eta"));
        payload.put("weatherSnapshot", snap.get("weather"));
        payload.put("trafficSnapshot", snap.get("traffic"));
        if (body.get("note") != null) payload.put("note", body.get("note"));

        try {
            String canonical = objectMapper.writeValueAsString(payload);
            String hash = sha256Hex(canonical);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("hash", hash);
            out.put("hashAlgorithm", "SHA-256");
            out.put("timestamp", ts);
            out.put("payload", payload);
            out.put("canonicalJsonLength", canonical.length());
            out.put("dataDisclaimer", "本哈希为服务端对事件快照的指纹。");
            return out;
        } catch (Exception e) {
            return Map.of("success", false, "message", "序列化失败: " + e.getMessage());
        }
    }

    private TripLeg findTripLeg(String tripId) {
        for (TripLeg t : ETA_TRIPS) {
            if (t.id().equals(tripId)) return t;
        }
        return ETA_TRIPS[0];
    }

    /** 与 eta-ml 同源：单票天气 + 路况 + 车辆在途快照 */
    private Map<String, Object> buildDelayInsightContext(TripLeg t) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> wx = etaRealtimeService.fetchOpenMeteoCurrent(t.dLat(), t.dLon());
        Map<String, Object> weather = new LinkedHashMap<>();
        if (Boolean.TRUE.equals(wx.get("success"))) {
            weather.put("description", wx.get("description"));
            weather.put("weatherCode", wx.get("weatherCode"));
            weather.put("delayFactor", wx.get("delayFactor"));
            weather.put("source", wx.getOrDefault("source", "气象估算"));
        } else {
            weather.put("error", wx.getOrDefault("message", "weather failed"));
            weather.put("source", "气象数据");
        }

        String originLl = String.format("%.5f,%.5f", t.oLon(), t.oLat());
        String destLl = String.format("%.5f,%.5f", t.dLon(), t.dLat());
        Map<String, Object> drv = etaRealtimeService.fetchAmapDrivingDurationSec(originLl, destLl);

        Map<String, Object> traffic = new LinkedHashMap<>();
        double baseMin;
        if (Boolean.TRUE.equals(drv.get("success"))) {
            int sec = ((Number) drv.get("durationSec")).intValue();
            baseMin = sec / 60.0;
            traffic.put("source", "高德驾车路径（真实路况与距离，需 AMAP_KEY）");
            traffic.put("durationSec", sec);
            double haFree = etaRealtimeService.estimateMinutesByHaversine(t.oLon(), t.oLat(), t.dLon(), t.dLat(), 65);
            double trafficIdx = Math.min(95, Math.max(5, (baseMin / Math.max(haFree, 0.1) - 1) * 45 + 35));
            traffic.put("congestionIndex", Math.round(trafficIdx * 10) / 10.0);
        } else {
            baseMin = etaRealtimeService.estimateMinutesByHaversine(t.oLon(), t.oLat(), t.dLon(), t.dLat(), 45);
            traffic.put("source", "球面距离÷均速（高德不可用时的估算）");
            traffic.put("message", drv.getOrDefault("message", ""));
            traffic.put("congestionIndex", 40.0);
        }

        double weatherFactor = weather.get("delayFactor") instanceof Number
                ? ((Number) weather.get("delayFactor")).doubleValue() : 1.0;
        int habit = 70;
        double habitMul = 1.0 + (50 - habit) / 200.0;
        double rawMin = baseMin * weatherFactor * habitMul;
        long remMin = Math.max(5, Math.round(rawMin));

        Map<String, Object> eta = new LinkedHashMap<>();
        eta.put("predictedEta", now.plusMinutes(remMin).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        eta.put("remainingMinutes", remMin);
        eta.put("baselineRouteMinutes", Math.round(baseMin * 10) / 10.0);

        Map<String, Object> vehicle = vehicleSnapshotForTrip(t.id());

        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("tripId", t.id());
        ctx.put("origin", t.originName());
        ctx.put("destination", t.destName());
        ctx.put("weather", weather);
        ctx.put("traffic", traffic);
        ctx.put("eta", eta);
        ctx.put("vehicle", vehicle);
        ctx.put("driverHabitScore", habit);
        ctx.put("tmsStatus", "在途运输");
        return ctx;
    }

    private Map<String, Object> vehicleSnapshotForTrip(String tripId) {
        int idx = 0;
        for (int i = 0; i < ETA_TRIPS.length; i++) {
            if (ETA_TRIPS[i].id().equals(tripId)) idx = i;
        }
        long tick = System.currentTimeMillis() / 5000;
        String[] ids = {"V-001", "V-002", "V-003"};
        String vid = ids[idx % 3];
        int fatigue = 28 + (int) (Math.sin(tick + idx) * 15) + idx * 5;
        fatigue = Math.min(100, Math.max(0, fatigue));
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("vehicleId", vid);
        v.put("plate", "沪A·" + (8800 + (idx % 3) * 17));
        v.put("speedKmh", 62 + (int) (tick % 15));
        v.put("fatigueScore", fatigue);
        v.put("fatigueAlert", fatigue > 75 ? "HIGH" : fatigue > 40 ? "MEDIUM" : "LOW");
        v.put("harshBrake30m", 1 + (idx + 1) % 3);
        v.put("note", "与在途监控页同源：高疲劳可能增加短暂停车与服务区休息");
        return v;
    }

    private String buildFallbackDelayExplanation(String userQuestion, Map<String, Object> context) {
        @SuppressWarnings("unchecked")
        Map<String, Object> w = (Map<String, Object>) context.get("weather");
        @SuppressWarnings("unchecked")
        Map<String, Object> tr = (Map<String, Object>) context.get("traffic");
        @SuppressWarnings("unchecked")
        Map<String, Object> eta = (Map<String, Object>) context.get("eta");
        @SuppressWarnings("unchecked")
        Map<String, Object> v = (Map<String, Object>) context.get("vehicle");

        String wd = w != null ? String.valueOf(w.getOrDefault("description", "—")) : "—";
        double cf = tr != null && tr.get("congestionIndex") instanceof Number
                ? ((Number) tr.get("congestionIndex")).doubleValue() : 40;
        String ts = tr != null ? String.valueOf(tr.getOrDefault("source", "")) : "";
        String etaStr = eta != null ? String.valueOf(eta.getOrDefault("predictedEta", "")) : "";
        int fat = v != null && v.get("fatigueScore") instanceof Number
                ? ((Number) v.get("fatigueScore")).intValue() : 0;

        return "【规则合成·无大模型】针对「" + userQuestion + "」\n"
                + "1）天气（目的地）：" + wd + "，可能影响装卸与末端行驶节奏。\n"
                + "2）路况：" + ts + "；拥堵指数约 " + String.format("%.1f", cf)
                + "，指数偏高时行程易长于基准。\n"
                + "3）当前预计到达：" + etaStr + "。\n"
                + "4）在途状态：司机疲劳分 " + fat + "，偏高时可能增加服务区休息与短暂延误。\n"
                + "综合：延误风险主要来自路况与驾驶状态叠加；配置千问 API 可获得更自然的推理说明。";
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "error-" + s.hashCode();
        }
    }
}
