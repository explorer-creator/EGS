package com.example.controller;

import com.example.config.CvProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 最优生产方案代理：转发至商业分析服务（Python 8001），
 * 支持一键导出 Excel/PDF，含设备、物料、工序、工艺、商业分析、绿色环保、智能检测、流程图。
 * 同时转发产品绿色/商业分析演示（product-context-demo），纯 LLM 调用，绕过 8003。
 */
@RestController
public class ProductionPlanController {

    private final CvProperties cvConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public ProductionPlanController(CvProperties cvConfig) {
        this.cvConfig = cvConfig;
    }

    private String getBusinessBaseUrl() {
        String base = cvConfig.getGreenBusinessUrl();
        return (base != null && !base.isEmpty()) ? base.replaceAll("/+$", "") : "http://127.0.0.1:8001";
    }

    /**
     * 产品绿色/商业分析演示：转发至 8001，纯 LLM，绕过 8003。
     */
    @PostMapping("/api/product-context-demo")
    public Map<String, Object> productContextDemo(@RequestBody Map<String, Object> body) {
        String url = getBusinessBaseUrl() + "/api/product-context-demo";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
    }

    /**
     * 产品综合报告：聚合敏感度、绿色仪表盘、LLM 文本，返回可视化数据。
     */
    @PostMapping("/api/product-report")
    public Map<String, Object> productReport(@RequestBody Map<String, Object> body) {
        String url = getBusinessBaseUrl() + "/api/product-report";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
    }

    /**
     * 生成最优生产方案（JSON）。production-plan-bypass-8001=true 或 8001 返回 405 时，直接返回演示方案。
     */
    @PostMapping("/api/production-plan/optimal")
    public Map<String, Object> optimal(@RequestBody Map<String, Object> body) {
        if (cvConfig.isProductionPlanBypass8001()) {
            return buildFallbackOptimalPlan(body);
        }
        String url = getBusinessBaseUrl() + "/api/production-plan/optimal";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            return restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
        } catch (HttpClientErrorException.MethodNotAllowed e) {
            return buildFallbackOptimalPlan(body);
        } catch (Exception e) {
            return buildFallbackOptimalPlan(body);
        }
    }

    /** 8001 不可用时的演示方案（绕过 405） */
    private Map<String, Object> buildFallbackOptimalPlan(Map<String, Object> body) {
        String product = body != null && body.get("product_name") != null
                ? String.valueOf(body.get("product_name")).trim() : "产品";
        int quantity = body != null && body.get("quantity") != null
                ? ((Number) body.get("quantity")).intValue() : 1000;
        if (product.isEmpty()) product = "产品";

        List<Map<String, Object>> equipment = List.of(
                map("equipment_code", "EQ-001", "equipment_name", "SMT贴片机", "manufacturer", "示例厂商", "brand", "", "model", "SMT-100", "supplier", "", "location", "产线A"),
                map("equipment_code", "EQ-002", "equipment_name", "AOI检测机", "manufacturer", "示例厂商", "brand", "", "model", "AOI-200", "supplier", "", "location", "产线A")
        );
        List<Map<String, Object>> materials = List.of(
                map("material_code", "MAT-001", "material_name", "PCB基板", "model", "", "quantity", 1, "unit", "件", "supplier", ""),
                map("material_code", "MAT-002", "material_name", "主控芯片", "model", "", "quantity", 1, "unit", "颗", "supplier", "")
        );
        List<Map<String, Object>> procedures = List.of(
                map("procedure_code", "PROC-001", "procedure_name", "贴片", "production_steps", "SMT贴片", "production_inspection_equipment", "AOI", "operator", ""),
                map("procedure_code", "PROC-002", "procedure_name", "检测", "production_steps", "AOI检测", "production_inspection_equipment", "", "operator", "")
        );
        List<Map<String, Object>> routes = List.of(
                map("route_code", "ROUTE-001", "route_name", product + "工艺路线", "product", product, "version", "1.0", "description", "", "procedure_sequence", List.of("PROC-001", "PROC-002"))
        );

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("success", true);
        plan.put("product_name", product);
        plan.put("quantity", quantity);
        plan.put("summary", "演示方案（接口 405 时兜底）：涵盖 " + product + " 的完整生产流程，设备 " + equipment.size() + " 台、物料 " + materials.size() + " 种、工序 " + procedures.size() + " 道、工艺路线 " + routes.size() + " 条。");
        plan.put("equipment", equipment);
        plan.put("materials", materials);
        plan.put("procedures", procedures);
        plan.put("process_routes", routes);
        plan.put("_fallback", true);
        return plan;
    }

    private Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }

    /**
     * 导出 Excel（返回文件流）。production-plan-bypass-8001=true 或 8001 不可用时返回简易兜底 Excel。
     */
    @PostMapping("/api/production-plan/export-excel")
    public ResponseEntity<byte[]> exportExcel(@RequestBody Map<String, Object> body) {
        if (cvConfig.isProductionPlanBypass8001()) {
            byte[] fallback = buildFallbackExcel(body);
            String productName = body != null && body.get("product_name") != null ? String.valueOf(body.get("product_name")) : "产品";
            String filename = "产品生产方案_" + productName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) + ".xlsx";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(fallback);
        }
        String url = getBusinessBaseUrl() + "/api/production-plan/export-excel";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = restTemplate.postForObject(url, new HttpEntity<>(body, headers), byte[].class);
            String productName = body.get("product_name") != null ? String.valueOf(body.get("product_name")) : "产品";
            String filename = "产品生产方案_" + productName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) + ".xlsx";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes != null ? bytes : new byte[0]);
        } catch (Exception e) {
            byte[] fallback = buildFallbackExcel(body);
            String productName = body.get("product_name") != null ? String.valueOf(body.get("product_name")) : "产品";
            String filename = "产品生产方案_" + productName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) + ".xlsx";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(fallback);
        }
    }

    /**
     * 导出 PDF（返回文件流）。production-plan-bypass-8001=true 或 8001 不可用时返回说明文件。
     */
    @PostMapping("/api/production-plan/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody Map<String, Object> body) {
        if (cvConfig.isProductionPlanBypass8001()) {
            String msg = "PDF 导出暂不可用（演示模式）。完整 PDF 请设置 cv.production-plan-bypass-8001=false 并启动 8001 服务。";
            return ResponseEntity.status(503)
                    .header("X-Error-Message", msg)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(msg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        String url = getBusinessBaseUrl() + "/api/production-plan/export-pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = restTemplate.postForObject(url, new HttpEntity<>(body, headers), byte[].class);
            String productName = body.get("product_name") != null ? String.valueOf(body.get("product_name")) : "产品";
            String filename = "产品生产方案_" + productName + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) + ".pdf";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes != null ? bytes : new byte[0]);
        } catch (Exception e) {
            byte[] fallback = "导出 PDF 服务暂不可用（8001 返回 405 或未启动），请先启动 business-analysis 服务。".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header("Content-Disposition", "attachment; filename=\"export-pdf-failed.txt\"")
                    .body(fallback);
        }
    }

    @SuppressWarnings("unchecked")
    private byte[] buildFallbackExcel(Map<String, Object> body) {
        Map<String, Object> plan = buildFallbackOptimalPlan(body);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("生产方案");
            Row r0 = sheet.createRow(0);
            r0.createCell(0).setCellValue("产品");
            r0.createCell(1).setCellValue(String.valueOf(plan.get("product_name")));
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("计划产量");
            r1.createCell(1).setCellValue(((Number) plan.get("quantity")).intValue());
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("说明");
            r2.createCell(1).setCellValue("演示方案（接口 405 时兜底），完整导出请启动 8001 服务。");
            int row = 4;
            for (Object o : (List<?>) (plan.get("equipment") != null ? plan.get("equipment") : List.of())) {
                Map<String, Object> e = (Map<String, Object>) o;
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue("设备");
                r.createCell(1).setCellValue(String.valueOf(e.get("equipment_name")));
            }
            for (Object o : (List<?>) (plan.get("materials") != null ? plan.get("materials") : List.of())) {
                Map<String, Object> m = (Map<String, Object>) o;
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue("物料");
                r.createCell(1).setCellValue(String.valueOf(m.get("material_name")));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            return new byte[0];
        }
    }
}
