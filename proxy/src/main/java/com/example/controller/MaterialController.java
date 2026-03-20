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
 * 物料管理接口，转发到 xDM-F Material01 等。xdm.mock-mode=true 时返回 Mock 数据。
 */
@RestController
@RequestMapping("/api")
public class MaterialController extends BaseXdmProxyController {

    private final EntityMappingService entityMapping;
    private final XdmMockService mockService;

    public MaterialController(XdmProxyProperties xdmConfig, EntityMappingService entityMapping, XdmMockService mockService) {
        super(xdmConfig);
        this.entityMapping = entityMapping;
        this.mockService = mockService;
    }

    @PostMapping("/dynamic/api/MaterialManagement/list")
    public Object getMaterialList(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.listMaterial();
        HttpHeaders headers = buildForwardHeaders(req);
        Map<String, Object> body = buildListBodyWithPageVO(requestBody, req);
        List<String> entities = entityMapping.getPreferredEntities("MaterialManagement", List.of("Material01", "Part", "MaterialManagement"));
        for (String e : entities) {
            try {
                return postWith405Fallback(getBaseUrl() + "/dynamic/api/" + e + "/list", new HttpEntity<>(body, headers));
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound ignored) { }
        }
        return Map.of("result", "SUCCESS", "data", Collections.emptyList(), "message", "该模型在 xDM-F 中未部署，请在设计态创建并发布");
    }

    @PostMapping("/dynamic/api/MaterialManagement/create")
    public Object addMaterial(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.createMaterial(ensureMasterInBody(requestBody));
        Map<String, Object> body = ensureMasterInBody(requestBody);
        HttpEntity<?> entity = new HttpEntity<>(body, buildForwardHeaders(req));
        List<String> entities = entityMapping.getPreferredEntities("MaterialManagement", List.of("Material01", "Part", "MaterialManagement"));
        return postCreateWithEntityFallback(entities, "create", entity, "物料模型在 xDM-F 中未部署，请在设计态创建并发布 Material01 或 MaterialManagement 实体");
    }

    @PostMapping("/dynamic/api/MaterialManagement/update")
    public Object updateMaterial(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.updateMaterial(requestBody);
        List<String> entities = entityMapping.getPreferredEntities("MaterialManagement", List.of("Material01", "Part", "MaterialManagement"));
        for (String e : entities) {
            try {
                return restTemplate.postForObject(getBaseUrl() + "/dynamic/api/" + e + "/update", new HttpEntity<>(requestBody, buildForwardHeaders(req)), Object.class);
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound ignored) { }
        }
        return Map.of("result", "FAIL", "message", "物料模型在 xDM-F 中未部署");
    }

    @DeleteMapping("/dynamic/api/MaterialManagement/delete/{id}")
    public Object deleteMaterial(@PathVariable String id, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) {
            mockService.deleteMaterial(id);
            return Map.of("success", true, "message", "删除成功");
        }
        List<String> entities = entityMapping.getPreferredEntities("MaterialManagement", List.of("Material01", "Part", "MaterialManagement"));
        for (String e : entities) {
            try {
                deleteWith405Fallback(getBaseUrl() + "/dynamic/api/" + e + "/delete/" + id, new HttpEntity<>(buildForwardHeaders(req)));
                return Map.of("success", true, "message", "删除成功");
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound ignored) { }
        }
        return Map.of("success", false, "message", "物料模型在 xDM-F 中未部署");
    }

    @PostMapping("/dynamic/api/MaterialVersionManagement/list")
    public Object getMaterialVersionList(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.listMaterialVersion();
        HttpHeaders headers = buildForwardHeaders(req);
        return postWith404Fallback(getBaseUrl() + "/dynamic/api/MaterialVersionManagement/list",
            new HttpEntity<>(requestBody, headers));
    }

    @PostMapping("/dynamic/api/MaterialVersionManagement/create")
    public Object addMaterialVersion(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.createMaterialVersion(requestBody);
        return postCreateWith404Fallback(getBaseUrl() + "/dynamic/api/MaterialVersionManagement/create",
            new HttpEntity<>(ensureMasterInBody(requestBody), buildForwardHeaders(req)));
    }

    @PostMapping("/dynamic/api/MaterialCategoryManagement/list")
    public Object getMaterialCategoryList(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.listMaterialCategory();
        HttpHeaders headers = buildForwardHeaders(req);
        return postWith404Fallback(getBaseUrl() + "/dynamic/api/MaterialCategoryManagement/list",
            new HttpEntity<>(requestBody, headers));
    }

    @PostMapping("/dynamic/api/MaterialCategoryManagement/create")
    public Object addMaterialCategory(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.createMaterialCategory(requestBody);
        return postCreateWith404Fallback(getBaseUrl() + "/dynamic/api/MaterialCategoryManagement/create",
            new HttpEntity<>(ensureMasterInBody(requestBody), buildForwardHeaders(req)));
    }

    @DeleteMapping("/dynamic/api/MaterialCategoryManagement/delete/{id}")
    public Object deleteMaterialCategory(@PathVariable String id, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) {
            mockService.deleteMaterialCategory(id);
            return Map.of("success", true, "message", "删除成功");
        }
        return deleteWith404Fallback(getBaseUrl() + "/dynamic/api/MaterialCategoryManagement/delete/" + id, req,
            "物料分类模型在 xDM-F 中未部署");
    }

    @PostMapping("/dynamic/api/MaterialBOMManagement/list")
    public Object getBOMList(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.listMaterialBOM();
        HttpHeaders headers = buildForwardHeaders(req);
        return postWith404Fallback(getBaseUrl() + "/dynamic/api/MaterialBOMManagement/list",
            new HttpEntity<>(requestBody, headers));
    }

    @PostMapping("/dynamic/api/MaterialBOMManagement/create")
    public Object createBOM(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.createMaterialBOM(requestBody);
        return postCreateWith404Fallback(getBaseUrl() + "/dynamic/api/MaterialBOMManagement/create",
            new HttpEntity<>(ensureMasterInBody(requestBody), buildForwardHeaders(req)));
    }

    @DeleteMapping("/dynamic/api/MaterialBOMManagement/delete/{id}")
    public Object deleteBOM(@PathVariable String id, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) {
            mockService.deleteMaterialBOM(id);
            return Map.of("success", true, "message", "删除成功");
        }
        return deleteWith404Fallback(getBaseUrl() + "/dynamic/api/MaterialBOMManagement/delete/" + id, req,
            "BOM 模型在 xDM-F 中未部署");
    }
}
