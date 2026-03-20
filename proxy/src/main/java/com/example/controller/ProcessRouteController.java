package com.example.controller;

import com.example.config.EntityMappingService;
import com.example.config.XdmProxyProperties;
import com.example.service.XdmMockService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 工艺路线管理接口，转发到 xDM-F ProcessRoute 等。xdm.mock-mode=true 时返回 Mock 数据。
 */
@RestController
@RequestMapping("/api")
public class ProcessRouteController extends BaseXdmProxyController {

    private final EntityMappingService entityMapping;
    private final XdmMockService mockService;

    public ProcessRouteController(XdmProxyProperties xdmConfig, EntityMappingService entityMapping, XdmMockService mockService) {
        super(xdmConfig);
        this.entityMapping = entityMapping;
        this.mockService = mockService;
    }

    @PostMapping("/dynamic/api/ProcessRoute/list")
    public Object getProcessRouteList(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.listProcessRoute();
        HttpHeaders headers = buildForwardHeaders(req);
        Map<String, Object> body = buildListBodyWithPageVO(requestBody, req);
        List<String> entities = entityMapping.getPreferredEntities("ProcessRoute", List.of("ProcessRoute", "ProcessRouteManagement"));
        for (String e : entities) {
            try {
                return postWith405Fallback(getBaseUrl() + "/dynamic/api/" + e + "/list", new HttpEntity<>(body, headers));
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound ignored) { }
        }
        return Map.of("result", "SUCCESS", "data", Collections.emptyList(), "message", "该模型在 xDM-F 中未部署");
    }

    @PostMapping("/dynamic/api/ProcessRoute/create")
    public Object createProcessRoute(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.createProcessRoute(ensureMasterInBody(requestBody));
        Map<String, Object> body = ensureMasterInBody(requestBody);
        HttpEntity<?> entity = new HttpEntity<>(body, buildForwardHeaders(req));
        List<String> entities = entityMapping.getPreferredEntities("ProcessRoute", List.of("ProcessRoute", "ProcessRouteManagement"));
        return postCreateWithEntityFallback(entities, "create", entity, "工艺路线模型在 xDM-F 中未部署，请在设计态创建并发布 ProcessRoute 或 ProcessRouteManagement 实体");
    }

    @DeleteMapping("/dynamic/api/ProcessRoute/delete/{id}")
    public Object deleteProcessRoute(@PathVariable String id, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) {
            mockService.deleteProcessRoute(id);
            return Map.of("success", true, "message", "删除成功");
        }
        List<String> entities = entityMapping.getPreferredEntities("ProcessRoute", List.of("ProcessRoute", "ProcessRouteManagement"));
        for (String e : entities) {
            try {
                deleteWith405Fallback(getBaseUrl() + "/dynamic/api/" + e + "/delete/" + id, new HttpEntity<>(buildForwardHeaders(req)));
                return Map.of("success", true, "message", "删除成功");
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound ignored) { }
        }
        return Map.of("success", false, "message", "工艺路线模型在 xDM-F 中未部署");
    }

    @PostMapping("/dynamic/api/ProcessRouteProcedure/list")
    public Object getProcessRouteProcedureList(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.listProcessRouteProcedure();
        HttpHeaders headers = buildForwardHeaders(req);
        Map<String, Object> body = buildListBodyWithPageVO(requestBody, req);
        return postWith404Fallback(getBaseUrl() + "/dynamic/api/ProcessRouteProcedure/list",
            new HttpEntity<>(body, headers));
    }

    @PostMapping("/dynamic/api/ProcessRouteProcedure/create")
    public Object createProcessRouteProcedure(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.createProcessRouteProcedure(ensureMasterInBody(requestBody));
        Map<String, Object> body = ensureMasterInBody(requestBody);
        HttpEntity<?> entity = new HttpEntity<>(body, buildForwardHeaders(req));
        List<String> entities = entityMapping.getPreferredEntities("ProcessRouteProcedure", List.of("ProcessRouteProcedure"));
        return postCreateWithEntityFallback(entities, "create", entity, "工艺路线工序关联模型在 xDM-F 中未部署，请在设计态创建并发布 ProcessRouteProcedure 实体");
    }

    @DeleteMapping("/dynamic/api/ProcessRouteProcedure/delete/{id}")
    public Object deleteProcessRouteProcedure(@PathVariable String id, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) {
            mockService.deleteProcessRouteProcedure(id);
            return Map.of("success", true, "message", "删除成功");
        }
        return deleteWith404Fallback(getBaseUrl() + "/dynamic/api/ProcessRouteProcedure/delete/" + id, req,
            "工艺路线工序关联模型在 xDM-F 中未部署");
    }
}
