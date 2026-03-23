package com.example.controller;

import com.example.config.QwenProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 核心 RAG 对话控制器
 * 提供独立接口，整合本地向量检索与千问 DashScope 兼容接口调用
 */
@RestController
@RequestMapping("/api/rag")
public class LlmController {

    @Autowired
    private QwenProperties qwenProperties;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 接收前端聊天请求
     * @param request 包含前端传来的 "query" 字段
     * @return 返回给前端的 JSON 结果
     */
    @PostMapping("/chat")
    public Map<String, Object> chatWithRag(@RequestBody RagRequest request) {
        String userQuery = request.getQuery();
        Map<String, Object> responseMap = new HashMap<>();

        try {
            // ==========================================
            // 第一步：【本地向量数据库检索占位】
            // ==========================================
            System.out.println("=> [1] 收到提问，开始检索本地知识库: " + userQuery);
            
            // 模拟检索到的上下文 (后续此处替换为 Qdrant Client 检索代码)
            String retrievedContext = "1. [知识库片段]: 智慧物流智能客服可叠加 RAG 知识库。\n" +
                                      "2. [知识库片段]: 口岸单证 OCR 特征向量入池需进行预处理校验。";

            // ==========================================
            // 第二步：【拼凑 System / User Prompt】
            // ==========================================
            System.out.println("=> [2] 检索完毕，正在组装增强 Prompt...");
            String systemContent = "你是一个专业的智慧物流 AI 助手。请严格基于以下【已知信息】回答用户的问题。\n"
                                 + "如果已知信息无法回答该问题，请直接回答'根据本地知识库，我暂时无法回答该问题'。\n\n"
                                 + "【已知信息】:\n" + retrievedContext;

            // ==========================================
            // 第三步：【构造调用千问 DashScope 的请求】
            // ==========================================
            Map<String, Object> llmRequestBody = new HashMap<>();
            llmRequestBody.put("model", qwenProperties.getModel());

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemContent);
            
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userQuery);

            messages.add(systemMsg);
            messages.add(userMsg);
            llmRequestBody.put("messages", messages);

            // ==========================================
            // 第四步：【发送 HTTP 请求并处理响应】
            // ==========================================
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + qwenProperties.getApiKey());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(llmRequestBody, headers);
            String apiUrl = qwenProperties.getBaseUrl() + "/chat/completions";

            System.out.println("=> [3] 发起调用千问 API: " + apiUrl);
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);

            // 【修复的截断部分】：解析返回的 JSON 并组装给前端的返回值
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String answer = (String) message.get("content");
                    
                    System.out.println("=> [4] 成功获取大模型 RAG 回答。");
                    responseMap.put("success", true);
                    responseMap.put("answer", answer);
                    return responseMap;
                }
            }
            
            responseMap.put("success", false);
            responseMap.put("message", "模型未返回有效内容");

        } catch (Exception e) {
            e.printStackTrace();
            responseMap.put("success", false);
            responseMap.put("message", "调用大模型或 RAG 检索时发生异常: " + e.getMessage());
        }

        return responseMap;
    }
}

/**
 * 前端请求接收的数据结构 DTO (必须存在，否则无法解析前端 JSON)
 */
class RagRequest {
    private String query;
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
}