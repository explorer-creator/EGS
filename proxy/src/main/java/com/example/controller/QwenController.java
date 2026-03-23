package com.example.controller;

import com.example.config.QwenProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 千问（DashScope）配置自检：不泄露完整 Key，便于排查「已配置但仍失败」。
 */
@RestController
@RequestMapping("/api/qwen")
public class QwenController {

    private final QwenProperties qwenConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public QwenController(QwenProperties qwenConfig) {
        this.qwenConfig = qwenConfig;
    }

    /** 仅检查 Key 是否非空、端点与模型名；可选轻量探测 DashScope（一次最小 tokens 调用） */
    @GetMapping("/status")
    public Map<String, Object> status() {
        String apiKey = resolveKey();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("apiKeyConfigured", apiKey != null && !apiKey.isEmpty());
        out.put("apiKeyHint", maskKey(apiKey));
        out.put("baseUrl", qwenConfig.getBaseUrl());
        out.put("textModel", qwenConfig.getModel());
        out.put("vlModel", qwenConfig.getVlModel());

        if (apiKey == null || apiKey.isEmpty()) {
            out.put("dashscopeReachable", false);
            out.put("hint", "未读取到 Key。请设置环境变量 DASHSCOPE_API_KEY，或在 application-local.yml 中配置 qwen.api-key，并重启 Java 代理。");
            return out;
        }

        // 轻量探测：最小请求，失败时把 HTTP 状态与响应片段返回便于排错
        String url = qwenConfig.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", qwenConfig.getModel());
        body.put("messages", List.of(
            Map.of("role", "user", "content", "ping")
        ));
        body.put("max_tokens", 2);
        body.put("temperature", 0.0);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(url, entity, Map.class);
            out.put("dashscopeReachable", true);
            out.put("probeHttpStatus", 200);
            out.put("probeNote", "compatible-mode /chat/completions 调用成功");
            if (resp != null && resp.containsKey("error")) {
                out.put("probeWarning", resp.get("error"));
            }
        } catch (HttpStatusCodeException e) {
            out.put("dashscopeReachable", false);
            out.put("probeHttpStatus", e.getStatusCode().value());
            String snippet = e.getResponseBodyAsString();
            if (snippet != null && snippet.length() > 400) {
                snippet = snippet.substring(0, 400) + "…";
            }
            out.put("probeErrorBody", snippet);
            out.put("hint", hintForProbeFailure(e.getStatusCode().value(), snippet));
        } catch (Exception e) {
            out.put("dashscopeReachable", false);
            out.put("probeHttpStatus", null);
            out.put("probeException", e.getClass().getSimpleName() + ": " + e.getMessage());
            out.put("hint", "无法连接 DashScope：若在国内，请确认 base-url 为 https://dashscope.aliyuncs.com/compatible-mode/v1；检查本机能否访问外网 HTTPS、代理/防火墙是否拦截。");
        }
        return out;
    }

    private String resolveKey() {
        String k = qwenConfig.getApiKey();
        if (k == null || k.isEmpty()) {
            k = System.getenv("DASHSCOPE_API_KEY");
        }
        return k != null ? k.trim() : "";
    }

    private static String maskKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) return "(empty)";
        if (apiKey.length() <= 8) return "****";
        return apiKey.substring(0, 4) + "…" + apiKey.substring(apiKey.length() - 4);
    }

    private static String hintForProbeFailure(int status, String body) {
        String b = body == null ? "" : body.toLowerCase();
        if (status == 401 || b.contains("invalid") && b.contains("api")) {
            return "401/鉴权失败：Key 错误或已失效。请在阿里云 DashScope 控制台核对 API-Key，重新生成后配置并重启代理。";
        }
        if (status == 429 || b.contains("quota") || b.contains("limit")) {
            return "429/额度：免费额度用尽或 QPS 限制。请稍后再试或到控制台查看用量。";
        }
        if (status == 400 && (b.contains("model") || b.contains("not exist"))) {
            return "400 模型名错误：检查 QWEN_MODEL / QWEN_VL_MODEL（如 qwen-plus、qwen-vl-plus）是否在控制台已开通。";
        }
        if (status == 403 || status == 404) {
            return "403/404：请确认 base-url 使用官方 compatible-mode 地址；国际站与地域模型可能不同。";
        }
        return "请根据 probeErrorBody 对照 DashScope 文档；常见原因：Key 无效、模型未开通、网络无法访问 dashscope.aliyuncs.com。";
    }
}
