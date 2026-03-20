package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CORS 跨域配置属性，支持多前端地址、多环境部署。
 */
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /** 允许的源，支持逗号分隔多地址 */
    private String allowedOrigins = "http://127.0.0.1:5173,http://localhost:5173";
    private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
    private String allowedHeaders = "*";
    private boolean allowCredentials = true;
    private long maxAge = 3600;

    public String[] getAllowedOriginsArray() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return new String[] { "http://127.0.0.1:5173" };
        }
        return allowedOrigins.split(",\\s*");
    }

    public String getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    public String getAllowedMethods() { return allowedMethods; }
    public void setAllowedMethods(String allowedMethods) { this.allowedMethods = allowedMethods; }
    public String getAllowedHeaders() { return allowedHeaders; }
    public void setAllowedHeaders(String allowedHeaders) { this.allowedHeaders = allowedHeaders; }
    public boolean isAllowCredentials() { return allowCredentials; }
    public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }
    public long getMaxAge() { return maxAge; }
    public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
}
