package com.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 轻量级「检索」：从 classpath 术语表做关键词匹配，作为 RAG 的检索层。
 * 后续可替换为 Qdrant 向量 Top-K（队友 docker-compose 已提供 Qdrant 容器模板）。
 */
@Service
public class RagKnowledgeService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<Map<String, Object>> knowledgeItems = new ArrayList<>();

    @PostConstruct
    public void loadKnowledge() {
        try {
            ClassPathResource res = new ClassPathResource("rag/init_knowledge.json");
            if (!res.exists()) {
                return;
            }
            try (InputStream in = res.getInputStream()) {
                knowledgeItems = objectMapper.readValue(in, new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            knowledgeItems = new ArrayList<>();
            System.err.println("[RAG] 加载 init_knowledge.json 失败: " + e.getMessage());
        }
    }

    public int getItemCount() {
        return knowledgeItems.size();
    }

    /**
     * 根据用户问题检索若干条术语定义，拼成上下文文本。
     */
    public String retrieveContext(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return "";
        }
        if (knowledgeItems.isEmpty()) {
            return "（知识库未加载）";
        }

        String q = userQuery.trim().toLowerCase(Locale.ROOT);
        String[] tokens = q.split("[\\s,，。；;、]+");
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> item : knowledgeItems) {
            int score = 0;
            String term = String.valueOf(item.getOrDefault("term", ""));
            String cat = String.valueOf(item.getOrDefault("category", ""));
            String def = String.valueOf(item.getOrDefault("definition", ""));
            String blob = (term + " " + cat + " " + def).toLowerCase(Locale.ROOT);
            for (String t : tokens) {
                if (t.length() < 2) continue;
                if (blob.contains(t)) score += 2;
            }
            if (q.length() >= 2 && blob.contains(q)) score += 5;
            if (score > 0) {
                Map<String, Object> m = new HashMap<>(item);
                m.put("_score", score);
                scored.add(m);
            }
        }

        scored.sort(Comparator.comparingInt((Map<String, Object> m) -> (Integer) m.getOrDefault("_score", 0)).reversed());

        List<Map<String, Object>> top = scored.stream().limit(5).collect(Collectors.toList());
        if (top.isEmpty()) {
            // 无命中时给模型少量通用条目，避免完全空上下文
            top = knowledgeItems.stream().limit(3).map(m -> new HashMap<>(m)).collect(Collectors.toList());
        }

        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Map<String, Object> item : top) {
            sb.append(i++).append(". 【").append(item.get("term")).append("】（")
                .append(item.get("category")).append("）\n")
                .append(item.get("definition")).append("\n\n");
        }
        return sb.toString().trim();
    }
}
