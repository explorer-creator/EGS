package com.example.controller;

import com.example.config.XdmProxyProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * xDM-F 代理基类，提供鉴权头构建、请求转发等公共逻辑。
 * 配置从 XdmProxyProperties 注入，支持 application.yml 与环境变量，提升可扩展性。
 */
public abstract class BaseXdmProxyController {

    protected final XdmProxyProperties xdmConfig;
    protected final RestTemplate restTemplate = new RestTemplate();

    protected BaseXdmProxyController(XdmProxyProperties xdmConfig) {
        this.xdmConfig = xdmConfig;
    }

    protected String getBaseUrl() {
        return xdmConfig.getBaseUrl();
    }

    /** 构建目标 URL，使用可配置的 api-path-prefix */
    protected String buildTargetUrl(String entity, String action) {
        String prefix = xdmConfig.getApiPathPrefix();
        if (prefix == null || prefix.isEmpty()) prefix = "dynamic/api";
        return getBaseUrl() + "/" + prefix.replaceAll("/+$", "") + "/" + entity + "/" + action;
    }

    /** 405 时的备用路径（部分 xDM-F 部署使用 rdm/basic/api/dynamic） */
    protected String buildFallbackUrl(String entity, String action) {
        return getBaseUrl() + "/rdm/basic/api/dynamic/" + entity + "/" + action;
    }

    /** POST 请求，405 时自动尝试备用路径 */
    protected Object postWith405Fallback(String targetUrl, HttpEntity<?> request) {
        try {
            return restTemplate.postForObject(targetUrl, request, Object.class);
        } catch (HttpClientErrorException.MethodNotAllowed e) {
            String fallback = targetUrl.replace("/dynamic/api/", "/rdm/basic/api/dynamic/");
            if (!fallback.equals(targetUrl)) {
                try {
                    return restTemplate.postForObject(fallback, request, Object.class);
                } catch (Exception ignored) { }
            }
            throw e;
        }
    }

    /** DELETE 请求，405 时自动尝试备用路径 */
    protected void deleteWith405Fallback(String targetUrl, HttpEntity<?> request) {
        try {
            restTemplate.exchange(targetUrl, HttpMethod.DELETE, request, Object.class);
        } catch (HttpClientErrorException.MethodNotAllowed e) {
            String fallback = targetUrl.replace("/dynamic/api/", "/rdm/basic/api/dynamic/");
            if (!fallback.equals(targetUrl)) {
                try {
                    restTemplate.exchange(fallback, HttpMethod.DELETE, request, Object.class);
                    return;
                } catch (Exception ignored) { }
            }
            throw e;
        }
    }

    /** 将当前请求中的 Cookie、Authorization 或前端粘贴的 X-XDM-Cookie 转发到 xDM-F */
    protected HttpHeaders buildForwardHeaders(HttpServletRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String xdmCookie = req.getHeader("X-XDM-Cookie");
        if (xdmCookie != null && !xdmCookie.isEmpty()) {
            headers.add("Cookie", xdmCookie);
        } else {
            String cookie = req.getHeader("Cookie");
            if (cookie != null && !cookie.isEmpty()) {
                headers.add("Cookie", cookie);
            }
        }
        String auth = req.getHeader("Authorization");
        if (auth != null && !auth.isEmpty()) {
            headers.add("Authorization", auth);
        }
        String xAuthToken = req.getHeader("X-Auth-Token");
        if (xAuthToken == null || xAuthToken.isEmpty()) xAuthToken = req.getHeader("x-auth-token");
        String authVal = (xAuthToken != null && !xAuthToken.isEmpty()) ? xAuthToken : xdmConfig.getDefaultAuthToken();
        headers.add("X-Auth-Token", authVal);
        headers.add("x-auth-token", authVal);
        String csrf = req.getHeader("X-CSRF-TOKEN");
        if (csrf == null || csrf.isEmpty()) csrf = req.getHeader("X-XSRF-TOKEN");
        if (csrf == null || csrf.isEmpty()) csrf = req.getHeader("x-csrf-token");
        if (csrf != null && !csrf.isEmpty()) {
            headers.add("X-CSRF-TOKEN", csrf);
            headers.add("X-XSRF-TOKEN", csrf);
            headers.add("x-csrf-token", csrf);
        }
        headers.add("Referer", xdmConfig.getReferer());
        headers.add("Origin", xdmConfig.getOrigin());
        String ua = req.getHeader("User-Agent");
        headers.add("User-Agent", (ua != null && !ua.isEmpty()) ? ua : "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36");
        String appId = req.getHeader("X-DME-ApplicationId");
        headers.add("applicationId", (appId != null && !appId.isEmpty()) ? appId : xdmConfig.getApplicationId());
        String tid = req.getHeader("X-DME-TenantId");
        headers.add("tenantid", (tid != null && !tid.isEmpty()) ? tid : xdmConfig.getTenantId());
        String uid = req.getHeader("X-DME-UserId");
        headers.add("x-user-id", (uid != null && !uid.isEmpty()) ? uid : xdmConfig.getDefaultUserId());
        String mod = req.getHeader("X-DME-Modifier");
        if (mod != null && !mod.isEmpty()) headers.add("modifier", mod);
        return headers;
    }

    /** 调用 xDM-F list 接口，404 返回空列表，405 时尝试备用路径 rdm/basic/api/dynamic */
    protected Object postWith404Fallback(String targetUrl, HttpEntity<?> request) {
        try {
            return restTemplate.postForObject(targetUrl, request, Object.class);
        } catch (HttpClientErrorException.NotFound e) {
            return Map.of("result", "SUCCESS", "data", Collections.emptyList(), "message", "该模型在 xDM-F 中未部署，请在设计态创建并发布");
        } catch (HttpClientErrorException.MethodNotAllowed e) {
            String fallback = targetUrl.replace("/dynamic/api/", "/rdm/basic/api/dynamic/");
            if (!fallback.equals(targetUrl)) {
                try {
                    return restTemplate.postForObject(fallback, request, Object.class);
                } catch (Exception ignored) { }
            }
            throw e;
        }
    }

    /** 简化 list 参数，仅保留 conditions（部分实体对 orderBy 等敏感） */
    protected Map<String, Object> simplifyListParams(Map<String, Object> requestBody) {
        Object params = requestBody != null ? requestBody.get("params") : null;
        Object conditions = params instanceof Map ? ((Map<?, ?>) params).get("conditions") : null;
        return Map.of("params", Map.of("conditions", conditions != null ? conditions : Collections.emptyList()));
    }

    /** 构建 list 请求体，合并前端传来的 pageVO（从 query 或 body），确保 xDM-F 返回完整数据 */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> buildListBodyWithPageVO(Map<String, Object> requestBody, HttpServletRequest req) {
        Map<String, Object> params = new HashMap<>();
        if (requestBody != null && requestBody.get("params") instanceof Map) {
            params.putAll((Map<String, Object>) requestBody.get("params"));
        }
        String pageVOStr = req.getParameter("pageVO");
        if (pageVOStr != null && !pageVOStr.isEmpty()) {
            try {
                Map<String, Object> pageVO = new ObjectMapper().readValue(pageVOStr, Map.class);
                params.put("pageVO", pageVO);
                // 部分 xDM-F 接口要求 pageVO 在顶层，同时保留在 params 中
                Map<String, Object> body = new HashMap<>();
                body.put("params", params);
                body.put("pageVO", pageVO);
                return body;
            } catch (Exception ignored) { }
        }
        return Map.of("params", params);
    }

    /** 确保 create 请求体包含 master（xDM-F 必填），始终注入有效 master */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> ensureMasterInBody(Map<String, Object> body) {
        Map<String, Object> masterObj = Map.of("id", xdmConfig.getTenantId(), "clazz", "Tenant");
        if (body == null) return new HashMap<>(Map.of("master", masterObj, "params", Collections.emptyMap()));
        Object paramsObj = body.get("params");
        Map<String, Object> params = paramsObj instanceof Map ? new HashMap<>((Map<String, Object>) paramsObj) : new HashMap<>(body);
        params.put("master", masterObj);
        Map<String, Object> result = new HashMap<>();
        result.put("master", masterObj);
        result.put("params", params);
        return result;
    }

    /** 调用 xDM-F create/update 接口，404 返回友好提示，405 时尝试备用路径 */
    protected Object postCreateWith404Fallback(String targetUrl, HttpEntity<?> request) {
        try {
            return restTemplate.postForObject(targetUrl, request, Object.class);
        } catch (HttpClientErrorException.NotFound e) {
            return Map.of("result", "FAIL", "message", "该模型在 xDM-F 中未部署，请在设计态创建并发布");
        } catch (HttpClientErrorException.MethodNotAllowed e) {
            String fallback = targetUrl.replace("/dynamic/api/", "/rdm/basic/api/dynamic/");
            if (!fallback.equals(targetUrl)) {
                try {
                    return restTemplate.postForObject(fallback, request, Object.class);
                } catch (Exception ignored) { }
            }
            throw e;
        }
    }

    /**
     * D:\EGS\API 实例名称优先：按实体列表顺序尝试 POST create，404/405/400 时尝试下一个，403 返回鉴权错误。
     * 每次尝试时注入 params.rdmExtensionType = 当前实体名；Part 实体时映射 material_* -> part_*。
     */
    @SuppressWarnings("unchecked")
    protected Object postCreateWithEntityFallback(List<String> entities, String action, HttpEntity<?> request, String notDeployedMsg) {
        Object body = request.getBody();
        HttpHeaders headers = request.getHeaders() != null ? request.getHeaders() : new HttpHeaders();
        for (String entity : entities) {
            try {
                Object reqBody = body;
                if (body instanceof Map) {
                    Map<String, Object> bodyMap = new HashMap<>((Map<String, Object>) body);
                    Object paramsObj = bodyMap.get("params");
                    if (paramsObj instanceof Map) {
                        Map<String, Object> params = new HashMap<>((Map<String, Object>) paramsObj);
                        params.put("rdmExtensionType", entity);
                        if ("Part".equals(entity)) {
                            if (params.containsKey("material_code") && !params.containsKey("part_code")) params.put("part_code", params.get("material_code"));
                            if (params.containsKey("material_name") && !params.containsKey("part_name")) params.put("part_name", params.get("material_name"));
                        }
                        bodyMap.put("params", params);
                        reqBody = bodyMap;
                    }
                }
                return postWith405Fallback(getBaseUrl() + "/dynamic/api/" + entity + "/" + action, new HttpEntity<>(reqBody, headers));
            } catch (HttpClientErrorException.Forbidden e) {
                String msg = e.getResponseBodyAsString();
                return Map.of("result", "FAIL", "message", "403 鉴权失败" + (msg != null && !msg.isEmpty() ? "：" + msg : "，请检查 Cookie 和 x-auth-token"));
            } catch (HttpClientErrorException.NotFound e) {
                // 尝试下一个实体
            } catch (HttpClientErrorException.MethodNotAllowed e) {
                // 尝试下一个实体
            } catch (HttpClientErrorException.BadRequest e) {
                // 参数校验失败（如 IIT.01411000），尝试下一个实体
            }
        }
        return Map.of("result", "FAIL", "message", notDeployedMsg);
    }

    /** 调用 xDM-F delete 接口，404 返回提示，405 时尝试备用路径 */
    protected Object deleteWith404Fallback(String targetUrl, HttpServletRequest req, String notDeployedMsg) {
        try {
            restTemplate.exchange(targetUrl, HttpMethod.DELETE, new HttpEntity<>(buildForwardHeaders(req)), Object.class);
            return Map.of("success", true, "message", "删除成功");
        } catch (HttpClientErrorException.NotFound e) {
            return Map.of("success", false, "message", notDeployedMsg);
        } catch (HttpClientErrorException.MethodNotAllowed e) {
            String fallback = targetUrl.replace("/dynamic/api/", "/rdm/basic/api/dynamic/");
            if (!fallback.equals(targetUrl)) {
                try {
                    restTemplate.exchange(fallback, HttpMethod.DELETE, new HttpEntity<>(buildForwardHeaders(req)), Object.class);
                    return Map.of("success", true, "message", "删除成功");
                } catch (Exception ignored) { }
            }
            throw e;
        }
    }
}
