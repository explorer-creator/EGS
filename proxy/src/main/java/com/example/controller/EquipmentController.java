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
 * 设备管理接口，转发到 xDM-F EquipmentManagement 实体。
 * xdm.mock-mode=true 时返回 Mock 数据，无需 8003。
 */
@RestController
@RequestMapping("/api")
public class EquipmentController extends BaseXdmProxyController {

    private final EntityMappingService entityMapping;
    private final XdmMockService mockService;

    public EquipmentController(XdmProxyProperties xdmConfig, EntityMappingService entityMapping, XdmMockService mockService) {
        super(xdmConfig);
        this.entityMapping = entityMapping;
        this.mockService = mockService;
    }

    @PostMapping("/dynamic/api/EquipmentManagement/list")
    public Object getEquipmentList(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.listEquipment();
        HttpHeaders headers = buildForwardHeaders(req);
        Map<String, Object> body = buildListBodyWithPageVO(requestBody, req);
        List<String> entities = entityMapping.getPreferredEntities("EquipmentManagement", List.of("EquipmentManagement"));
        for (String e : entities) {
            try {
                return postWith405Fallback(getBaseUrl() + "/dynamic/api/" + e + "/list", new HttpEntity<>(body, headers));
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound ignored) { }
        }
        return Map.of("result", "SUCCESS", "data", Collections.emptyList(), "message", "该模型在 xDM-F 中未部署");
    }

    @PostMapping("/dynamic/api/EquipmentManagement/create")
    public Object addEquipment(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.createEquipment(ensureMasterInBody(requestBody));
        Map<String, Object> body = ensureMasterInBody(requestBody);
        HttpEntity<?> entity = new HttpEntity<>(body, buildForwardHeaders(req));
        List<String> entities = entityMapping.getPreferredEntities("EquipmentManagement", List.of("EquipmentManagement"));
        return postCreateWithEntityFallback(entities, "create", entity, "设备模型在 xDM-F 中未部署");
    }

    @PostMapping("/dynamic/api/EquipmentManagement/update")
    public Object updateEquipment(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.updateEquipment(requestBody);
        List<String> entities = entityMapping.getPreferredEntities("EquipmentManagement", List.of("EquipmentManagement"));
        for (String e : entities) {
            try {
                return postWith405Fallback(getBaseUrl() + "/dynamic/api/" + e + "/update", new HttpEntity<>(requestBody, buildForwardHeaders(req)));
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound ignored) { }
        }
        return Map.of("result", "FAIL", "message", "设备模型在 xDM-F 中未部署");
    }

    @DeleteMapping("/dynamic/api/EquipmentManagement/delete/{id}")
    public Object deleteEquipment(@PathVariable String id, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) {
            mockService.deleteEquipment(id);
            return Map.of("success", true, "message", "删除成功");
        }
        List<String> entities = entityMapping.getPreferredEntities("EquipmentManagement", List.of("EquipmentManagement"));
        for (String e : entities) {
            try {
                deleteWith405Fallback(getBaseUrl() + "/dynamic/api/" + e + "/delete/" + id, new HttpEntity<>(buildForwardHeaders(req)));
                return Map.of("success", true, "message", "删除成功");
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound ignored) { }
        }
        return Map.of("success", false, "message", "设备模型在 xDM-F 中未部署");
    }
}
