package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * xDM-F 代理配置属性，支持外部配置与环境变量覆盖。
 * 提升系统灵活性与可扩展性：不同部署环境可通过 application.yml 或环境变量调整。
 */
@Component
@ConfigurationProperties(prefix = "xdm")
public class XdmProxyProperties {

    private String baseUrl = "http://127.0.0.1:8003/rdm_3f5e31761ec249ea9dace6b96dd92e6a_app/services";
    /**
     * 全局：true=全部实体走 Mock（当 remote-entities 为空时生效）。
     * 若配置了 remote-entities，则仅列表中的实体名会转发 8003，其余实体强制 Mock。
     */
    private boolean mockMode = false;
    /**
     * 逗号分隔的 xDM 实体名（与 API 路径一致，如 EquipmentManagement、User_Infor）。
     * 非空时：仅这些实体请求转发到 8003；未列入的实体始终 Mock，不受 mock-mode 影响。
     * 为空时：全部实体仍按 mock-mode 决定 Mock 或远程。
     */
    private String remoteEntities = "";
    /** 主路径：dynamic/api。405 时尝试 rdm/basic/api/dynamic */
    private String apiPathPrefix = "dynamic/api";
    /** list 不返回 password 时是否允许登录（宽松模式） */
    private boolean loginAllowNoPassword = true;
    private String applicationId = "5aa040bab9484f2d8cb08125e85ab15b";
    private String tenantId = "-1";
    private String defaultUserId = "1";
    private String defaultAuthToken = "1";
    private String referer = "http://127.0.0.1:8003/";
    private String origin = "http://127.0.0.1:8003";
    /** D:\EGS\API 实例名称优先：实体映射文件路径 */
    private String apiEntityMappingPath = "D:/EGS/API/entity-mapping.yaml";

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public boolean isMockMode() { return mockMode; }
    public void setMockMode(boolean mockMode) { this.mockMode = mockMode; }

    public String getRemoteEntities() { return remoteEntities; }
    public void setRemoteEntities(String remoteEntities) { this.remoteEntities = remoteEntities != null ? remoteEntities : ""; }

    /** 解析后的远程实体白名单 */
    public List<String> getRemoteEntityList() {
        if (remoteEntities == null || remoteEntities.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(remoteEntities.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 是否对该实体使用 Mock（不连 8003）。
     * remote-entities 非空：仅白名单内走远程，其余一律 Mock。
     * remote-entities 为空：由 mock-mode 决定全局。
     */
    public boolean useMockForEntity(String entityName) {
        List<String> remote = getRemoteEntityList();
        if (!remote.isEmpty()) {
            return !remote.contains(entityName);
        }
        return mockMode;
    }
    public String getApiPathPrefix() { return apiPathPrefix; }
    public void setApiPathPrefix(String apiPathPrefix) { this.apiPathPrefix = apiPathPrefix; }
    public boolean isLoginAllowNoPassword() { return loginAllowNoPassword; }
    public void setLoginAllowNoPassword(boolean loginAllowNoPassword) { this.loginAllowNoPassword = loginAllowNoPassword; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getDefaultUserId() { return defaultUserId; }
    public void setDefaultUserId(String defaultUserId) { this.defaultUserId = defaultUserId; }
    public String getDefaultAuthToken() { return defaultAuthToken; }
    public void setDefaultAuthToken(String defaultAuthToken) { this.defaultAuthToken = defaultAuthToken; }
    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getApiEntityMappingPath() { return apiEntityMappingPath; }
    public void setApiEntityMappingPath(String apiEntityMappingPath) { this.apiEntityMappingPath = apiEntityMappingPath; }
}
