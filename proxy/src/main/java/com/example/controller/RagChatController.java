package com.example.controller;

import com.example.config.QwenProperties;
import com.example.dto.RagChatRequest;
import com.example.service.RagKnowledgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 对话：本地知识检索（术语表 / 后续可换 Qdrant）+ 通义千问 DashScope 兼容接口。
 * 与 {@link LlmController} 并存：本类路径为 /api/rag，原小助手直连仍为 /api/llm/chat。
 */
@RestController
@RequestMapping("/api/rag")
public class RagChatController {

    private static final String SYSTEM_PREFIX = "你是智慧制造与物流领域的专业助手。请严格基于下方【检索到的知识片段】回答用户；若片段不足以回答，请说明并给出合理建议。\n\n【检索到的知识片段】\n";

    private final QwenProperties qwenConfig;
    private final RagKnowledgeService ragKnowledgeService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagChatController(QwenProperties qwenConfig, RagKnowledgeService ragKnowledgeService) {
        this.qwenConfig = qwenConfig;
        this.ragKnowledgeService = ragKnowledgeService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> out = new HashMap<>();
        out.put("ok", true);
        out.put("knowledgeItems", ragKnowledgeService.getItemCount());
        out.put("mode", "keyword-rag");
        out.put("hint", "向量库 Qdrant 可选：见 EGSfb/rag_project/docker-compose.yml");
        return out;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody RagChatRequest request) {
        String userQuery = request != null && request.getQuery() != null ? request.getQuery().trim() : "";
        Map<String, Object> responseMap = new HashMap<>();

        if (userQuery.isEmpty()) {
            responseMap.put("success", false);
            responseMap.put("message", "请输入 query");
            return responseMap;
        }

        String apiKey = qwenConfig.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            responseMap.put("success", false);
            responseMap.put("message", "请配置 DASHSCOPE_API_KEY 或 qwen.api-key");
            return responseMap;
        }

        try {
            String retrievedContext = ragKnowledgeService.retrieveContext(userQuery);
            String systemContent = SYSTEM_PREFIX + retrievedContext;

            String url = qwenConfig.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemContent));
            messages.add(Map.of("role", "user", "content", userQuery));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", qwenConfig.getModel());
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.6);
            requestBody.put("max_tokens", 2048);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response == null) {
                responseMap.put("success", false);
                responseMap.put("message", "千问 API 返回为空");
                return responseMap;
            }

            Object choices = response.get("choices");
            if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
                responseMap.put("success", false);
                responseMap.put("message", "千问未返回有效内容");
                return responseMap;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> firstChoice = (Map<String, Object>) ((List<?>) choices).get(0);
            Object messageObj = firstChoice.get("message");
            if (!(messageObj instanceof Map)) {
                responseMap.put("success", false);
                responseMap.put("message", "响应格式异常");
                return responseMap;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) messageObj;
            String answer = message.get("content") != null ? String.valueOf(message.get("content")) : "";

            responseMap.put("success", true);
            responseMap.put("answer", answer);
            responseMap.put("retrievedPreview", retrievedContext.length() > 800
                ? retrievedContext.substring(0, 800) + "…" : retrievedContext);
            return responseMap;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (e.getCause() != null) {
                msg += "; " + e.getCause().getMessage();
            }
            responseMap.put("success", false);
            responseMap.put("message", "RAG 调用失败: " + msg);
            return responseMap;
        }
    }
}
