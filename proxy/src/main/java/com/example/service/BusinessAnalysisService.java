package com.example.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 商业分析服务：成本计算、利润分析等。
 */
@Service
public class BusinessAnalysisService {

    /**
     * 计算产品成本。
     * @param materialCost 原材料成本（元）
     * @param laborCost 人工成本（元）
     * @param quantity 产量
     * @param overheadRate 制造费用率（0~1，如 0.15 表示 15%）
     */
    public Map<String, Object> calculateCost(double materialCost, double laborCost, int quantity, double overheadRate) {
        if (quantity <= 0) {
            return Map.of("success", false, "message", "产量必须大于 0");
        }

        double directCost = materialCost + laborCost;
        double overhead = directCost * (overheadRate >= 0 && overheadRate <= 1 ? overheadRate : 0.15);
        double totalCost = directCost + overhead;
        double unitCost = totalCost / quantity;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("materialCost", materialCost);
        result.put("laborCost", laborCost);
        result.put("directCost", directCost);
        result.put("overhead", overhead);
        result.put("overheadRate", overheadRate >= 0 && overheadRate <= 1 ? overheadRate : 0.15);
        result.put("totalCost", totalCost);
        result.put("quantity", quantity);
        result.put("unitCost", unitCost);
        return result;
    }

    /**
     * 计算利润与毛利率。
     */
    public Map<String, Object> calculateProfit(double revenue, double totalCost, int quantity) {
        if (quantity <= 0) {
            return Map.of("success", false, "message", "产量必须大于 0");
        }

        double profit = revenue - totalCost;
        double profitMargin = revenue > 0 ? (profit / revenue) * 100 : 0;
        double unitPrice = revenue / quantity;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("revenue", revenue);
        result.put("totalCost", totalCost);
        result.put("profit", profit);
        result.put("profitMargin", Math.round(profitMargin * 100) / 100.0);
        result.put("quantity", quantity);
        result.put("unitPrice", unitPrice);
        return result;
    }
}
