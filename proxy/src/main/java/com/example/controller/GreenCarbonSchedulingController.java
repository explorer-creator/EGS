package com.example.controller;

import com.example.config.QwenProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 绿色排产大屏数据（队友 greengeneratesysteam）+ 产品碳足迹 AI 报告（通义千问）。
 */
@RestController
@RequestMapping("/api/green")
public class GreenCarbonSchedulingController {

    private static final String CARBON_SYSTEM = """
            你是工业碳管理与绿色制造顾问。请结合公开可查的碳足迹方法论（如 ISO 14067、GHG Protocol、中国电网平均排放因子常识）与机械制造行业经验，给出专业分析。
            输出必须使用 Markdown，结构如下（用中文）：
            ## 1. 产品碳足迹概览
            - 估算单位产品 CO₂ 当量（kg 或 t），并说明数量级依据（材料、能耗、运输等假设需简要列出）。
            ## 2. 批量产量与总排放
            - 按用户给定产量估算总 CO₂ 当量区间（给出合理区间与关键假设）。
            ## 3. 减排路径
            - 至少 4 条可落地的节能/减排措施（设备、工艺、能源结构、供应链）。
            ## 4. 推荐绿色生产方案
            - 综合排序的「最佳绿色生产方案」摘要（可含排产时段、绿电/谷电、工艺替代等）。
            ## 5. 数据局限性
            - 说明需现场实测或 LCA 数据库才能精算。
            勿编造具体第三方未公开的精确数值；涉及因子时写「约」「典型区间」。""";

    private final QwenProperties qwenConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GreenCarbonSchedulingController(QwenProperties qwenConfig) {
        this.qwenConfig = qwenConfig;
    }

    /**
     * 绿能排产大屏：峰谷电价、趋势、待优化订单（与 greengeneratesysteam mock 同源）。
     */
    @GetMapping("/scheduling-dashboard")
    public Map<String, Object> schedulingDashboard() {
        try {
            ClassPathResource res = new ClassPathResource("green-scheduling-dashboard.json");
            String json = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            Map<String, Object> out = new LinkedHashMap<>(data);
            out.put("success", true);
            return out;
        } catch (Exception e) {
            return Map.of("success", false, "message", "加载大屏数据失败: " + e.getMessage());
        }
    }

    /**
     * 产品碳足迹与绿色方案（通义千问；需 DASHSCOPE_API_KEY）。
     * Body: { "productName": "行星减速器", "quantity": 1000, "context": "可选补充" }
     */
    @PostMapping("/product-carbon-report")
    public Map<String, Object> productCarbonReport(@RequestBody(required = false) Map<String, Object> body) {
        String productName = body != null && body.get("productName") != null
                ? String.valueOf(body.get("productName")).trim() : "";
        if (productName.isEmpty()) {
            return Map.of("success", false, "message", "请填写产品名称");
        }
        int quantity = 1;
        if (body != null && body.get("quantity") != null) {
            try {
                quantity = Math.max(1, Integer.parseInt(String.valueOf(body.get("quantity"))));
            } catch (NumberFormatException ignored) {
            }
        }
        String context = body != null && body.get("context") != null ? String.valueOf(body.get("context")).trim() : "";

        String apiKey = qwenConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            return Map.of(
                    "success", false,
                    "message", "请配置千问 API Key（qwen.api-key 或环境变量 DASHSCOPE_API_KEY）以生成碳足迹报告",
                    "fallbackMarkdown", buildFallbackReport(productName, quantity, context)
            );
        }

        String userMsg = "产品名称：" + productName + "\n计划产量（件）：" + quantity;
        if (!context.isEmpty()) userMsg += "\n补充说明：" + context;

        String url = qwenConfig.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", CARBON_SYSTEM));
        messages.add(Map.of("role", "user", "content", userMsg));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", qwenConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.45);
        requestBody.put("max_tokens", 2500);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response == null) return Map.of("success", false, "message", "LLM 返回为空");
            Object choices = response.get("choices");
            if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
                return Map.of("success", false, "message", "LLM 未返回有效内容");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> first = (Map<String, Object>) ((List<?>) choices).get(0);
            Object messageObj = first.get("message");
            if (!(messageObj instanceof Map)) return Map.of("success", false, "message", "响应格式异常");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) messageObj;
            String content = message.get("content") != null ? String.valueOf(message.get("content")) : "";

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("markdown", content);
            out.put("productName", productName);
            out.put("quantity", quantity);
            out.put("model", qwenConfig.getModel());
            return out;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (e.getCause() != null) msg += "; " + e.getCause().getMessage();
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", "调用 LLM 失败：" + msg);
            err.put("fallbackMarkdown", buildFallbackReport(productName, quantity, context));
            return err;
        }
    }

    private static String buildFallbackReport(String product, int qty, String ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 1. 产品碳足迹概览\n\n");
        sb.append("在未配置大模型 API 时，以下为**规则化占位分析**，仅供界面联调；配置 **DASHSCOPE_API_KEY** 后可生成完整报告。\n\n");
        sb.append("- 产品：**").append(product).append("**\n");
        sb.append("- 产量：**").append(qty).append("** 件\n");
        if (!ctx.isEmpty()) sb.append("- 补充：").append(ctx).append("\n");
        sb.append("\n## 2. 批量产量与总排放\n\n");
        sb.append("典型机械加工件单位碳足迹常在 **数 kg ~ 数十 kg CO₂e/件**（随材料、热处理、机加工工时变化）。总排放 ≈ 单位值 × 产量，需现场电表与 BOM 精算。\n\n");
        sb.append("## 3. 减排路径\n\n");
        sb.append("1. 热处理、压铸等高耗能工序尽量安排在**谷电**或绿电占比高的时段。\n");
        sb.append("2. 优化切削参数与刀具寿命，降低单位能耗与废品率。\n");
        sb.append("3. 轻质材料替代（在强度允许下）降低运输与加工碳强度。\n");
        sb.append("4. 空压机、空调等公用工程做变频与余热回收。\n\n");
        sb.append("## 4. 推荐绿色生产方案\n\n");
        sb.append("结合左侧「绿能排产」模块，对高耗电订单做**峰谷移荷**，并对接能源管理系统持续监测 kWh/件。\n\n");
        sb.append("## 5. 数据局限性\n\n");
        sb.append("本段为离线占位，正式披露请使用 LCA 数据库与第三方核查。\n");
        return sb.toString();
    }
}
