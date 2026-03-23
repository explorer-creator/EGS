package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 千问大模型配置读取类
 * 对应 application.yml 中的 qwen: 前缀配置
 */
@Configuration
@ConfigurationProperties(prefix = "qwen")
public class QwenProperties {

    /**
     * 对应 qwen.api-key
     * 注意：实际运行时建议由环境变量 DASHSCOPE_API_KEY 注入
     */
    private String apiKey;

    /**
     * 对应 qwen.model，默认为通义千问
     */
    private String model = "qwen-turbo";

    /**
     * 对应 qwen.base-url，DashScope 的 OpenAI 兼容接口地址
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    // --- Getters & Setters ---

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}