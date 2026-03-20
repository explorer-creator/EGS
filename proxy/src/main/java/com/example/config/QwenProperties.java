package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 千问（Qwen）API 配置，支持环境变量覆盖。
 */
@Component
@ConfigurationProperties(prefix = "qwen")
public class QwenProperties {

    /** API Key，可通过 application.yml 或环境变量 DASHSCOPE_API_KEY 配置 */
    private String apiKey;

    /** API 基础地址（OpenAI 兼容模式） */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /** 模型名称 */
    private String model = "qwen-plus";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
