package com.example.controller;

import com.example.config.EntityMappingService;
import com.example.config.XdmProxyProperties;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 导出 iDME/xDM-F 模型数据到 D:\EGS：Excel 文件 + 模型图谱（Mermaid）
 */
@RestController
@RequestMapping("/api")
public class ExportController extends BaseXdmProxyController {

    private static final String EXPORT_DIR = "D:\\EGS";
    private final EntityMappingService entityMapping;

    public ExportController(XdmProxyProperties xdmConfig, EntityMappingService entityMapping) {
        super(xdmConfig);
        this.entityMapping = entityMapping;
    }

    @PostMapping("/export/models-to-disk")
    public Map<String, Object> exportModelsToDisk(HttpServletRequest req) {
        try {
            Path dir = Paths.get(EXPORT_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            HttpHeaders headers = buildForwardHeaders(req);
            Map<String, Object> listBody = Map.of("params", Map.of(
                "conditions", Collections.emptyList(),
                "pageVO", Map.of("curPage", 1, "pageSize", 500, "totalRows", 0, "totalPages", 0, "limit", 500, "offset", 0)
            ));

            List<String> equipEntities = entityMapping.getPreferredEntities("EquipmentManagement", List.of("Equipment", "EquipmentManagement"));
            List<String> matEntities = entityMapping.getPreferredEntities("MaterialManagement", List.of("Material01", "Part", "MaterialManagement"));
            List<String> procEntities = entityMapping.getPreferredEntities("ProcedureManagement", List.of("WorkingProcedure", "ProcedureManagement"));
            List<String> routeEntities = entityMapping.getPreferredEntities("ProcessRoute", List.of("WorkingPlan", "ProcessRoute", "ProcessRouteManagement"));

            List<Map<String, Object>> equipment = fetchList(listBody, headers, equipEntities);
            List<Map<String, Object>> material = fetchList(listBody, headers, matEntities);
            List<Map<String, Object>> procedure = fetchList(listBody, headers, procEntities);
            List<Map<String, Object>> route = fetchList(listBody, headers, routeEntities);
            List<Map<String, Object>> routeProc = fetchList(listBody, headers, List.of("ProcessRouteProcedure"));

            String excelPath = dir.resolve("xDM-F模型导出_" + ts + ".xlsx").toString();
            writeExcel(excelPath, equipment, material, procedure, route, routeProc);

            String mermaidPath = dir.resolve("xDM-F模型图谱_" + ts + ".md").toString();
            writeMermaidGraph(mermaidPath, equipment, material, procedure, route, routeProc);

            return Map.of(
                "success", true,
                "message", "导出成功",
                "excelPath", excelPath,
                "mermaidPath", mermaidPath
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", "导出失败：" + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchList(Map<String, Object> body, HttpHeaders headers, List<String> entities) {
        for (String entity : entities) {
            try {
                String url = getBaseUrl() + "/dynamic/api/" + entity + "/list";
                Object res = postWith405Fallback(url, new HttpEntity<>(body, headers));
                return parseList(res);
            } catch (Exception ignored) { }
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseList(Object res) {
        if (res == null) return Collections.emptyList();
        Map<String, Object> m = res instanceof Map ? (Map<String, Object>) res : Map.of();
        Object data = m.get("data");
        if (data == null) data = m;
        if (data instanceof List) return (List<Map<String, Object>>) data;
        if (data instanceof Map) {
            Map<String, Object> dm = (Map<String, Object>) data;
            Object arr = dm.get("rows");
            if (arr == null) arr = dm.get("list");
            if (arr == null) arr = dm.get("data");
            if (arr instanceof List) return (List<Map<String, Object>>) arr;
        }
        return Collections.emptyList();
    }

    private void writeExcel(String path, List<Map<String, Object>> equipment, List<Map<String, Object>> material,
                           List<Map<String, Object>> procedure, List<Map<String, Object>> route,
                           List<Map<String, Object>> routeProc) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            writeSheet(wb, "设备", equipment, headerStyle, "equipment_code", "equipment_name", "manufacturer", "brand", "model");
            writeSheet(wb, "物料", material, headerStyle, "material_code", "material_name", "model", "version", "categoryId");
            writeSheet(wb, "工序", procedure, headerStyle, "procedure_code", "procedure_name", "production_steps", "operator");
            writeSheet(wb, "工艺路线", route, headerStyle, "route_code", "route_name", "product", "version", "description");
            writeSheet(wb, "工艺路线-工序关联", routeProc, headerStyle, "route_id", "procedure_id", "sequence_order");

            try (FileOutputStream fos = new FileOutputStream(path)) {
                wb.write(fos);
            }
        }
    }

    private void writeSheet(Workbook wb, String name, List<Map<String, Object>> data, CellStyle headerStyle, String... keys) {
        Sheet sheet = wb.createSheet(name.length() > 31 ? name.substring(0, 31) : name);
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        for (int i = 0; i < keys.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(keys[i]);
            c.setCellStyle(headerStyle);
        }
        for (Map<String, Object> row : data) {
            Row r = sheet.createRow(rowNum++);
            for (int i = 0; i < keys.length; i++) {
                Object v = getMapValue(row, keys[i]);
                r.createCell(i).setCellValue(v != null ? v.toString() : "");
            }
        }
    }

    private Object getMapValue(Map<String, Object> m, String key) {
        if (m.containsKey(key)) return m.get(key);
        String camel = toCamelCase(key);
        if (m.containsKey(camel)) return m.get(camel);
        String cap = camel.isEmpty() ? "" : Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
        return m.getOrDefault(cap, null);
    }

    private String toCamelCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '_' && i + 1 < s.length()) {
                sb.append(Character.toUpperCase(s.charAt(++i)));
            } else {
                sb.append(Character.toLowerCase(s.charAt(i)));
            }
        }
        return sb.toString();
    }

    private void writeMermaidGraph(String path, List<Map<String, Object>> equipment, List<Map<String, Object>> material,
                                  List<Map<String, Object>> procedure, List<Map<String, Object>> route,
                                  List<Map<String, Object>> routeProc) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# xDM-F 模型图谱\n\n");
        sb.append("```mermaid\nflowchart TB\n");
        sb.append("  subgraph 设备\n");
        int ei = 0;
        for (Map<String, Object> e : equipment) {
            String name = escapeMermaid((String) getMapValue(e, "equipment_name"));
            sb.append("    E").append(ei).append("[").append(name).append("]\n");
            ei++;
        }
        sb.append("  end\n\n  subgraph 物料\n");
        int mi = 0;
        for (Map<String, Object> m : material) {
            String name = escapeMermaid((String) getMapValue(m, "material_name"));
            sb.append("    M").append(mi).append("[").append(name).append("]\n");
            mi++;
        }
        sb.append("  end\n\n  subgraph 工序\n");
        Map<Object, Integer> procIdx = new HashMap<>();
        int pi = 0;
        for (Map<String, Object> p : procedure) {
            Object id = extractId(p.get("id"));
            procIdx.put(id, pi);
            String name = escapeMermaid((String) getMapValue(p, "procedure_name"));
            sb.append("    P").append(pi).append("[").append(name).append("]\n");
            pi++;
        }
        sb.append("  end\n\n  subgraph 工艺路线\n");
        int ri = 0;
        for (Map<String, Object> r : route) {
            String name = escapeMermaid((String) getMapValue(r, "route_name"));
            sb.append("    R").append(ri).append("[").append(name).append("]\n");
            ri++;
        }
        sb.append("  end\n\n");
        for (int i = 0; i < routeProc.size() - 1; i++) {
            Object pId = extractId(getMapValue(routeProc.get(i), "procedure_id"));
            Object nextPId = extractId(getMapValue(routeProc.get(i + 1), "procedure_id"));
            Integer a = procIdx.get(pId);
            Integer b = procIdx.get(nextPId);
            if (a != null && b != null) sb.append("  P").append(a).append(" --> P").append(b).append("\n");
        }
        sb.append("```\n");
        Files.write(Paths.get(path), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String escapeMermaid(String s) {
        if (s == null) return "";
        return s.replace("[", "(").replace("]", ")").replace("\"", "'");
    }

    @SuppressWarnings("unchecked")
    private Object extractId(Object o) {
        if (o == null) return null;
        if (o instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) o;
            return m.get("id");
        }
        return o;
    }
}
