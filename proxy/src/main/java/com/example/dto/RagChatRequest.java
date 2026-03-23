package com.example.dto;

/**
 * POST /api/rag/chat 请求体：用户自然语言问题。
 */
public class RagChatRequest {
    private String query;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
