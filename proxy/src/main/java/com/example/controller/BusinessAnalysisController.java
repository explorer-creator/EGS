package com.example.controller;

import com.example.config.QwenProperties;
import com.example.service.BusinessAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 商业分析模块：成本计算、LLM 市场建议。
 */
@RestController
@RequestMapping("/api/business")
public class BusinessAnalysisController {

    private static final String MARKET_SYSTEM = "你是一位资深商业分析师。根据用户提供的产品、成本、市场等信息，给出简洁专业的市场建议，包括：目标客户、定价策略、渠道建议、竞争分析、风险提示。使用 Markdown 格式，分点列出。";

    private final BusinessAnalysisService businessService;
    private final QwenProperties qwenConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BusinessAnalysisController(BusinessAnalysisService businessService, QwenProperties qwenConfig) {
        this.businessService = businessService;
        this.qwenConfig = qwenConfig;
    }

    /**
     * 成本计算。
     * POST /api/business/cost
     * Body: { "materialCost": 100, "laborCost": 50, "quantity": 1000, "overheadRate": 0.15 }
     */
    @PostMapping("/cost")
    public Map<String, Object> calculateCost(@RequestBody Map<String, Object> body) {
        double materialCost = toDouble(body.get("materialCost"), 0);
        double laborCost = toDouble(body.get("laborCost"), 0);
        int quantity = toInt(body.get("quantity"), 1);
        double overheadRate = toDouble(body.get("overheadRate"), 0.15);

        return businessService.calculateCost(materialCost, laborCost, quantity, overheadRate);
    }

    /**
     * 利润分析。
     * POST /api/business/profit
     * Body: { "revenue": 20000, "totalCost": 15000, "quantity": 1000 }
     */
    @PostMapping("/profit")
    public Map<String, Object> calculateProfit(@RequestBody Map<String, Object> body) {
        double revenue = toDouble(body.get("revenue"), 0);
        double totalCost = toDouble(body.get("totalCost"), 0);
        int quantity = toInt(body.get("quantity"), 1);

        return businessService.calculateProfit(revenue, totalCost, quantity);
    }

    /**
     * 调用 LLM 生成市场建议。
     * POST /api/business/market-suggestion
     * Body: { "product": "耳机", "unitCost": 50, "quantity": 5000, "context": "面向年轻消费者" }
     */
    @PostMapping("/market-suggestion")
    public Map<String, Object> marketSuggestion(@RequestBody Map<String, Object> body) {
        String product = body != null && body.get("product") != null ? String.valueOf(body.get("product")).trim() : "";
        if (product.isEmpty()) {
            return Map.of("success", false, "message", "请输入产品名称");
        }

        String apiKey = qwenConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            return Map.of("success", false, "message", "请配置千问 API Key");
        }

        double unitCost = toDouble(body != null ? body.get("unitCost") : null, 0);
        int quantity = toInt(body != null ? body.get("quantity") : null, 0);
        String context = body != null && body.get("context") != null ? String.valueOf(body.get("context")).trim() : "";

        StringBuilder userContent = new StringBuilder();
        userContent.append("请针对「").append(product).append("」给出市场建议。");
        if (unitCost > 0) userContent.append(" 单位成本：").append(unitCost).append(" 元。");
        if (quantity > 0) userContent.append(" 计划产量：").append(quantity).append(" 件。");
        if (!context.isEmpty()) userContent.append(" 补充信息：").append(context);

        String url = qwenConfig.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", MARKET_SYSTEM));
        messages.add(Map.of("role", "user", "content", userContent.toString()));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", qwenConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1024);

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
            Map<String, Object> firstChoice = (Map<String, Object>) ((List<?>) choices).get(0);
            Object messageObj = firstChoice.get("message");
            if (!(messageObj instanceof Map)) return Map.of("success", false, "message", "响应格式异常");

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) messageObj;
            String content = message.get("content") != null ? String.valueOf(message.get("content")) : "";

            return Map.of("success", true, "content", content, "product", product);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (e.getCause() != null) msg += "; " + e.getCause().getMessage();
            return Map.of("success", false, "message", "调用 LLM 失败：" + msg);
        }
    }

    private static double toDouble(Object o, double def) {
        if (o == null) return def;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int toInt(Object o, int def) {
        if (o == null) return def;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
