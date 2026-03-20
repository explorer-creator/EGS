package com.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 产品相关优秀企业推荐服务。
 * 根据产品名称匹配核心配件/工艺，推荐相关企业及联系方式。
 */
@Service
public class CompanyRecommendationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, List<String>> productKeywords = new HashMap<>();
    private List<Map<String, Object>> companies = new ArrayList<>();

    @PostConstruct
    public void loadData() {
        try (InputStream is = new ClassPathResource("product-companies.json").getInputStream()) {
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

            Object comp = root.get("companies");
            if (comp instanceof List) {
                for (Object o : (List<?>) comp) {
                    if (o instanceof Map) companies.add((Map<String, Object>) o);
                }
            }
        } catch (Exception e) {
            // 加载失败时使用空数据
        }
    }

    /**
     * 根据产品名称获取推荐企业列表。
     */
    public List<Map<String, Object>> getCompaniesForProduct(String product) {
        if (product == null || product.trim().isEmpty()) return Collections.emptyList();

        String productTrim = product.trim();
        Set<String> matchedTags = new HashSet<>();

        for (Map.Entry<String, List<String>> e : productKeywords.entrySet()) {
            if (productTrim.contains(e.getKey()) || e.getKey().contains(productTrim)) {
                matchedTags.addAll(e.getValue());
            }
        }

        if (matchedTags.isEmpty()) return Collections.emptyList();

        return companies.stream()
            .filter(c -> {
                Object tagsObj = c.get("tags");
                if (!(tagsObj instanceof List)) return false;
                for (Object t : (List<?>) tagsObj) {
                    if (t != null && matchedTags.contains(t.toString())) return true;
                }
                return false;
            })
            .limit(6)
            .collect(Collectors.toList());
    }
}
