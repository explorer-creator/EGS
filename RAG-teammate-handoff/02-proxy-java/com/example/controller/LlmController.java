package com.example.controller;

import com.example.config.QwenProperties;
import com.example.service.CompanyRecommendationService;
import com.example.service.ProductComponentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 千问（Qwen）LLM 接口：产品制造知识问答。
 * 用户输入产品名称（如耳机、充电宝等），模型输出原材料、设备、工序、工艺流程、物理原理等。
 * 同时提供 B 站等相关视频网站的工艺讲解视频推荐。
 */
@RestController
@RequestMapping("/api/llm")
public class LlmController {

    /** 常见产品对应的 B 站工艺/制造讲解视频（bvid），API 失败时使用 */
    private static final Map<String, String> FALLBACK_VIDEOS = Map.of(
        "耳机", "BV1Cz4y1a7HA", "充电宝", "BV1Lx411c7Vw", "锂电池", "BV1GJ411x7h7",
        "行星减速器", "BV1B4411h7xN", "减速器", "BV1B4411h7xN", "电路板", "BV1px411c7mE", "PCB", "BV1px411c7mE"
    );

    private static final String BILIBILI_SEARCH_URL = "https://api.bilibili.com/x/web-interface/search/type";

    private static final String SYSTEM_PROMPT = "你是一位专业的制造业专家。当用户询问某种产品的制造信息时，请按以下结构清晰回答（使用 Markdown 格式）：\n" +
        "1. **原材料**：列出生产该产品所需的主要原材料\n" +
        "2. **所用设备**：列出生产过程中使用的主要设备\n" +
        "3. **工序**：简要列出主要工序步骤\n" +
        "4. **工艺流程**：描述完整的工艺流程\n" +
        "5. **相关物理原理**：说明涉及的主要物理原理（如力学、热学、电磁学等）\n\n" +
        "若用户问题不限于产品制造，可灵活回答。保持专业、准确、简洁。";

    private final QwenProperties qwenConfig;
    private final CompanyRecommendationService companyService;
    private final ProductComponentService componentService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmController(QwenProperties qwenConfig, CompanyRecommendationService companyService, ProductComponentService componentService) {
        this.qwenConfig = qwenConfig;
        this.companyService = companyService;
        this.componentService = componentService;
    }

    @PostMapping("/product-query")
    public Map<String, Object> productQuery(@RequestBody Map<String, Object> body) {
        String product = body != null && body.get("product") != null
            ? String.valueOf(body.get("product")).trim()
            : "";
        if (product.isEmpty()) {
            return Map.of("success", false, "message", "请输入产品名称");
        }

        String apiKey = qwenConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            return Map.of("success", false, "message",
                "请配置千问 API Key：在 application.yml 中设置 qwen.api-key，或设置环境变量 DASHSCOPE_API_KEY");
        }

        String url = qwenConfig.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", "请详细介绍「" + product + "」的制造相关信息：原材料、所用设备、工序、工艺流程、相关主要物理原理。"));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", qwenConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2048);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response == null) {
                return Map.of("success", false, "message", "千问 API 返回为空");
            }

            Object choices = response.get("choices");
            if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
                return Map.of("success", false, "message", "千问 API 未返回有效内容");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> firstChoice = (Map<String, Object>) ((List<?>) choices).get(0);
            Object messageObj = firstChoice.get("message");
            if (!(messageObj instanceof Map)) {
                return Map.of("success", false, "message", "千问 API 响应格式异常");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) messageObj;
            String content = message.get("content") != null ? String.valueOf(message.get("content")) : "";

            return Map.of(
                "success", true,
                "content", content,
                "product", product
            );
        } catch (Exception e) {
            String msg = e.getMessage();
            if (e.getCause() != null) msg += "; " + e.getCause().getMessage();
            return Map.of("success", false, "message", "调用千问 API 失败：" + msg);
        }
    }

    /**
     * 根据产品名称获取相关工艺/制造讲解视频（B 站等）。
     * 优先调用 B 站搜索 API，失败时使用预设视频映射。
     */
    @GetMapping("/product-video")
    public Map<String, Object> productVideo(@RequestParam String product) {
        String productTrim = product != null ? product.trim() : "";
        if (productTrim.isEmpty()) {
            return Map.of("success", false, "message", "请输入产品名称");
        }

        String bvid = null;
        String title = null;

        // 1. 尝试 B 站搜索 API
        try {
            String keyword = URLEncoder.encode(productTrim + " 工艺 制造", StandardCharsets.UTF_8);
            String url = BILIBILI_SEARCH_URL + "?search_type=video&keyword=" + keyword + "&page=1&page_size=10";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Referer", "https://search.bilibili.com");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class).getBody();

            if (resp != null && Integer.valueOf(0).equals(resp.get("code"))) {
                Object dataObj = resp.get("data");
                if (dataObj instanceof Map) {
                    Object resultObj = ((Map<?, ?>) dataObj).get("result");
                    if (resultObj instanceof List && !((List<?>) resultObj).isEmpty()) {
                        Object first = ((List<?>) resultObj).get(0);
                        if (first instanceof Map) {
                            Map<?, ?> item = (Map<?, ?>) first;
                            bvid = item.get("bvid") != null ? String.valueOf(item.get("bvid")) : null;
                            title = item.get("title") != null ? String.valueOf(item.get("title")).replace("<em class=\"keyword\">", "").replace("</em>", "") : null;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // 忽略，使用 fallback
        }

        // 2. Fallback：预设映射或模糊匹配
        if (bvid == null || bvid.isEmpty()) {
            bvid = FALLBACK_VIDEOS.get(productTrim);
            if (bvid == null) {
                for (Map.Entry<String, String> e : FALLBACK_VIDEOS.entrySet()) {
                    if (productTrim.contains(e.getKey())) {
                        bvid = e.getValue();
                        break;
                    }
                }
            }
        }

        if (bvid == null || bvid.isEmpty()) {
            return Map.of("success", false, "message", "暂未找到相关视频");
        }

        String embedUrl = "https://player.bilibili.com/player.html?bvid=" + bvid;
        String pageUrl = "https://www.bilibili.com/video/" + bvid;

        return Map.of(
            "success", true,
            "bvid", bvid,
            "title", title != null ? title : "相关工艺讲解视频",
            "embedUrl", embedUrl,
            "pageUrl", pageUrl
        );
    }

    private static final String ROBOT_SYSTEM = "你是一个可爱的小机器人助手，性格活泼、亲切。用简洁易懂的语言回答，适当使用表情符号。";

    /**
     * 小机器人智能体聊天接口。
     * 支持普通对话和「名词解释」模式（explainTerm 为 true 时，用通俗语言解释专业术语）。
     */
    @PostMapping("/chat")
    public Map<String, Object> robotChat(@RequestBody Map<String, Object> body) {
        String userMsg = body != null && body.get("message") != null ? String.valueOf(body.get("message")).trim() : "";
        boolean explainTerm = body != null && Boolean.TRUE.equals(body.get("explainTerm"));

        if (userMsg.isEmpty()) {
            return Map.of("success", false, "message", "请输入内容");
        }

        String apiKey = qwenConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            return Map.of("success", false, "message", "请配置千问 API Key");
        }

        String systemContent = explainTerm
            ? ROBOT_SYSTEM + " 用户双击了专业术语，请用通俗易懂的语言解释「" + userMsg + "」，不要超过 100 字，适合非专业人士理解。"
            : ROBOT_SYSTEM;

        String url = qwenConfig.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemContent));
        messages.add(Map.of("role", "user", "content", userMsg));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", qwenConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.8);
        requestBody.put("max_tokens", 512);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response == null) return Map.of("success", false, "message", "API 返回为空");
            Object choices = response.get("choices");
            if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
                return Map.of("success", false, "message", "未返回有效内容");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> firstChoice = (Map<String, Object>) ((List<?>) choices).get(0);
            Object messageObj = firstChoice.get("message");
            if (!(messageObj instanceof Map)) return Map.of("success", false, "message", "响应格式异常");

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) messageObj;
            String content = message.get("content") != null ? String.valueOf(message.get("content")) : "";

            return Map.of("success", true, "content", content);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (e.getCause() != null) msg += "; " + e.getCause().getMessage();
            return Map.of("success", false, "message", "调用失败：" + msg);
        }
    }

    /**
     * 根据产品名称推荐相关优秀企业（核心配件、工艺供应商）。
     */
    @GetMapping("/product-companies")
    public Map<String, Object> productCompanies(@RequestParam String product) {
        String productTrim = product != null ? product.trim() : "";
        if (productTrim.isEmpty()) {
            return Map.of("success", false, "message", "请输入产品名称");
        }
        List<Map<String, Object>> list = companyService.getCompaniesForProduct(productTrim);
        return Map.of("success", true, "companies", list, "product", productTrim);
    }

    /**
     * 根据产品名称获取核心配件展示（图片与介绍）。
     */
    @GetMapping("/product-components")
    public Map<String, Object> productComponents(@RequestParam String product) {
        String productTrim = product != null ? product.trim() : "";
        if (productTrim.isEmpty()) {
            return Map.of("success", false, "message", "请输入产品名称");
        }
        List<Map<String, Object>> list = componentService.getComponentsForProduct(productTrim);
        return Map.of("success", true, "components", list, "product", productTrim);
    }
}
