package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 视觉质检配置。demo-mode=true 时禁止将模拟缺陷同步至 xDM-F。
 * green-business-url：智能质检×环保指标，检测到次品时调用浪费折算接口。
 */
@Component
@ConfigurationProperties(prefix = "cv")
public class CvProperties {

    /** true=演示模式（模拟检测，禁止同步），false=真实 CV 已接入，允许同步 */
    private boolean demoMode = true;

    /** 商业分析服务地址，用于次品浪费折算。为空则不调用 */
    private String greenBusinessUrl = "";

    /** true=生产方案不转发 8001，直接返回演示方案（绕过 405） */
    private boolean productionPlanBypass8001 = false;

    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    public String getGreenBusinessUrl() {
        return greenBusinessUrl;
    }

    public void setGreenBusinessUrl(String greenBusinessUrl) {
        this.greenBusinessUrl = greenBusinessUrl;
    }

    public boolean isProductionPlanBypass8001() {
        return productionPlanBypass8001;
    }

    public void setProductionPlanBypass8001(boolean productionPlanBypass8001) {
        this.productionPlanBypass8001 = productionPlanBypass8001;
    }
}
