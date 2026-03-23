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
 * 工序管理接口，转发到 xDM-F WorkingProcedure 等。xdm.mock-mode=true 时返回 Mock 数据。
 */
@RestController
@RequestMapping("/api")
public class ProcedureController extends BaseXdmProxyController {

    private final EntityMappingService entityMapping;
    private final XdmMockService mockService;

    public ProcedureController(XdmProxyProperties xdmConfig, EntityMappingService entityMapping, XdmMockService mockService) {
        super(xdmConfig);
        this.entityMapping = entityMapping;
        this.mockService = mockService;
    }

    // ========== 工序（WorkingProcedure） ==========

    @PostMapping("/dynamic/api/ProcedureManagement/list")
    public Object getProcedureList(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("ProcedureManagement")) return mockService.listProcedure();
        HttpHeaders headers = buildForwardHeaders(req);
        Map<String, Object> body = buildListBodyWithPageVO(requestBody, req);
        List<String> entities = entityMapping.getPreferredEntities("ProcedureManagement", List.of("ProcedureManagement", "WorkingProcedure"));
        for (String e : entities) {
            try {
                return postWith405Fallback(getBaseUrl() + "/dynamic/api/" + e + "/list", new HttpEntity<>(body, headers));
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound ignored) { }
        }
        return Map.of("result", "SUCCESS", "data", Collections.emptyList(), "message", "该模型在 xDM-F 中未部署");
    }

    @PostMapping("/dynamic/api/ProcedureManagement/create")
    public Object addProcedure(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("ProcedureManagement")) return mockService.createProcedure(ensureMasterInBody(requestBody));
        Map<String, Object> body = ensureMasterInBody(requestBody);
        HttpEntity<?> entity = new HttpEntity<>(body, buildForwardHeaders(req));
        List<String> entities = entityMapping.getPreferredEntities("ProcedureManagement", List.of("ProcedureManagement", "WorkingProcedure"));
        return postCreateWithEntityFallback(entities, "create", entity, "工序模型在 xDM-F 中未部署，请在设计态创建并发布 ProcedureManagement 或 WorkingProcedure 实体");
    }

    @DeleteMapping("/dynamic/api/ProcedureManagement/delete/{id}")
    public Object deleteProcedure(@PathVariable String id, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("ProcedureManagement")) {
            mockService.deleteProcedure(id);
            return Map.of("success", true, "message", "删除成功");
        }
        List<String> entities = entityMapping.getPreferredEntities("ProcedureManagement", List.of("ProcedureManagement", "WorkingProcedure"));
        for (String e : entities) {
            try {
                deleteWith405Fallback(getBaseUrl() + "/dynamic/api/" + e + "/delete/" + id, new HttpEntity<>(buildForwardHeaders(req)));
                return Map.of("success", true, "message", "删除成功");
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound ignored) { }
        }
        return Map.of("success", false, "message", "工序模型在 xDM-F 中未部署");
    }

    // ========== 工序-设备关联（ProcedureEquipment 关系实体） ==========

    @PostMapping("/dynamic/api/ProcedureEquipment/list")
    public Object getProcedureEquipmentList(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("ProcedureEquipment")) return mockService.listProcedureEquipment();
        HttpHeaders headers = buildForwardHeaders(req);
        Map<String, Object> body = buildListBodyWithPageVO(requestBody, req);
        return postWith404Fallback(getBaseUrl() + "/dynamic/api/ProcedureEquipment/list",
            new HttpEntity<>(body, headers));
    }

    @PostMapping("/dynamic/api/ProcedureEquipment/create")
    public Object createProcedureEquipment(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("ProcedureEquipment")) return mockService.createProcedureEquipment(ensureMasterInBody(requestBody));
        return postCreateWith404Fallback(getBaseUrl() + "/dynamic/api/ProcedureEquipment/create",
            new HttpEntity<>(ensureMasterInBody(requestBody), buildForwardHeaders(req)));
    }

    @DeleteMapping("/dynamic/api/ProcedureEquipment/delete/{id}")
    public Object deleteProcedureEquipment(@PathVariable String id, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("ProcedureEquipment")) {
            mockService.deleteProcedureEquipment(id);
            return Map.of("success", true, "message", "删除成功");
        }
        return deleteWith404Fallback(getBaseUrl() + "/dynamic/api/ProcedureEquipment/delete/" + id, req,
            "工序-设备关联模型在 xDM-F 中未部署");
    }

    // ========== 工序-物料关联（ProcedureMaterial 关系实体） ==========

    @PostMapping("/dynamic/api/ProcedureMaterial/list")
    public Object getProcedureMaterialList(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("ProcedureMaterial")) return mockService.listProcedureMaterial();
        HttpHeaders headers = buildForwardHeaders(req);
        Map<String, Object> body = buildListBodyWithPageVO(requestBody, req);
        return postWith404Fallback(getBaseUrl() + "/dynamic/api/ProcedureMaterial/list",
            new HttpEntity<>(body, headers));
    }

    @PostMapping("/dynamic/api/ProcedureMaterial/create")
    public Object createProcedureMaterial(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("ProcedureMaterial")) return mockService.createProcedureMaterial(ensureMasterInBody(requestBody));
        return postCreateWith404Fallback(getBaseUrl() + "/dynamic/api/ProcedureMaterial/create",
            new HttpEntity<>(ensureMasterInBody(requestBody), buildForwardHeaders(req)));
    }

    @DeleteMapping("/dynamic/api/ProcedureMaterial/delete/{id}")
    public Object deleteProcedureMaterial(@PathVariable String id, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("ProcedureMaterial")) {
            mockService.deleteProcedureMaterial(id);
            return Map.of("success", true, "message", "删除成功");
        }
        return deleteWith404Fallback(getBaseUrl() + "/dynamic/api/ProcedureMaterial/delete/" + id, req,
            "工序-物料关联模型在 xDM-F 中未部署");
    }
}
