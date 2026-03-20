package com.example.config;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * D:\EGS\API 实例名称优先：从 entity-mapping.yaml 加载实体映射，
 * 转发时优先尝试 API 中定义的实例名称。
 */
@Service
public class EntityMappingService {

    private final XdmProxyProperties xdmConfig;
    private Map<String, List<String>> entityMapping = new HashMap<>();

    public EntityMappingService(XdmProxyProperties xdmConfig) {
        this.xdmConfig = xdmConfig;
    }

    @PostConstruct
    public void loadMapping() {
        String path = xdmConfig.getApiEntityMappingPath();
        if (path == null || path.isEmpty()) return;
        try {
            Path p = Path.of(path.trim());
            if (!Files.exists(p)) return;
            Yaml yaml = new Yaml();
            try (var reader = Files.newBufferedReader(p)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> root = yaml.load(reader);
                if (root == null) return;
                for (Map.Entry<String, Object> e : root.entrySet()) {
                    if (e.getValue() instanceof List) {
                        List<String> list = new ArrayList<>();
                        for (Object o : (List<?>) e.getValue()) {
                            if (o != null) list.add(o.toString());
                        }
                        if (!list.isEmpty()) entityMapping.put(e.getKey(), list);
                    }
                }
            }
        } catch (IOException e) {
            // 静默忽略，使用默认逻辑
        }
    }

    /**
     * 获取优先尝试的实体列表（API 实例名称优先）
     * @param frontendEntity 前端调用的实体名，如 MaterialManagement
     * @param fallbacks 默认备选列表
     * @return 按优先级排序的实体列表
     */
    public List<String> getPreferredEntities(String frontendEntity, List<String> fallbacks) {
        List<String> fromMapping = entityMapping.get(frontendEntity);
        if (fromMapping != null && !fromMapping.isEmpty()) {
            return new ArrayList<>(fromMapping);
        }
        return fallbacks != null ? new ArrayList<>(fallbacks) : List.of(frontendEntity);
    }
}
