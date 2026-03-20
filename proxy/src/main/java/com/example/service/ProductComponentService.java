package com.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 产品核心配件展示服务。
 * 根据产品名称匹配并返回核心配件/零部件的图片与介绍。
 */
@Service
public class ProductComponentService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, List<String>> productKeywords = new HashMap<>();
    private List<Map<String, Object>> components = new ArrayList<>();

    @PostConstruct
    public void loadData() {
        try (InputStream is = new ClassPathResource("product-components.json").getInputStream()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = objectMapper.readValue(is, Map.class);

            Object pk = root.get("productKeywords");
            if (pk instanceof Map) {
                Map<?, ?> pkMap = (Map<?, ?>) pk;
                for (Map.Entry<?, ?> e : pkMap.entrySet()) {
                    if (e.getValue() instanceof List) {
                        List<String> vals = new ArrayList<>();
                        for (Object o : (List<?>) e.getValue()) {
                            if (o != null) vals.add(o.toString());
                        }
                        productKeywords.put(e.getKey().toString(), vals);
                    }
                }
            }

            Object comp = root.get("components");
            if (comp instanceof List) {
                for (Object o : (List<?>) comp) {
                    if (o instanceof Map) components.add((Map<String, Object>) o);
                }
            }
        } catch (Exception e) {
            // 加载失败时使用空数据
        }
    }

    /**
     * 根据产品名称获取核心配件展示列表。
     */
    public List<Map<String, Object>> getComponentsForProduct(String product) {
        if (product == null || product.trim().isEmpty()) return Collections.emptyList();

        String productTrim = product.trim();
        Set<String> matchedTags = new HashSet<>();

        for (Map.Entry<String, List<String>> e : productKeywords.entrySet()) {
            if (productTrim.contains(e.getKey()) || e.getKey().contains(productTrim)) {
                matchedTags.addAll(e.getValue());
            }
        }

        if (matchedTags.isEmpty()) return Collections.emptyList();

        return components.stream()
            .filter(c -> {
                Object tagsObj = c.get("tags");
                if (!(tagsObj instanceof List)) return false;
                for (Object t : (List<?>) tagsObj) {
                    if (t != null && matchedTags.contains(t.toString())) return true;
                }
                return false;
            })
            .limit(8)
            .collect(Collectors.toList());
    }
}
