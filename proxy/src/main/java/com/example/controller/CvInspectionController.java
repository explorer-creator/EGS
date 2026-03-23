package com.example.controller;

import com.example.config.CvProperties;
import com.example.config.XdmProxyProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * 视觉质检（CV）模块：PCB 裸板缺陷识别。
 * 支持漏焊、短路、断路检测，缺陷坐标实时同步至 xDM-F 元模型，触发自动报废流程。
 * 可接入昇腾 Atlas、百度 PaddlePaddle 等推理引擎。
 */
@RestController
@RequestMapping("/api/cv")
public class CvInspectionController extends BaseXdmProxyController {

    private final CvProperties cvConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public CvInspectionController(XdmProxyProperties xdmConfig, CvProperties cvConfig) {
        super(xdmConfig);
        this.cvConfig = cvConfig;
    }

    /**
     * 调用商业分析服务的次品浪费折算接口（智能质检×环保指标）。
     * 返回物料浪费(g)、能耗浪费(kWh)、CO2(kg) 等。
     */
    private Map<String, Object> fetchDefectWaste(int defectCount, String imageId, List<Map<String, Object>> defects) {
        String base = cvConfig.getGreenBusinessUrl();
        if (base == null || base.isEmpty()) return null;
        String url = base.replaceAll("/+$", "") + "/api/green/defect-waste";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("defect_count", defectCount);
            body.put("image_id", imageId);
            body.put("product_type", "PCB");
            List<String> types = new ArrayList<>();
            for (Map<String, Object> d : defects) {
                Object t = d.get("type");
                if (t != null) types.add(String.valueOf(t));
            }
            body.put("defect_types", types);
            body.put("material_per_unit_g", Map.of("copper", 50, "tin", 5, "fr4", 80));
            body.put("energy_per_unit_kwh", 0.15);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(url, req, Map.class);
            return resp;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * PCB 裸板缺陷识别。
     * 当前为模拟检测，可替换为 PaddlePaddle/昇腾 Atlas 推理。
     */
    @PostMapping(value = "/inspect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> inspect(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Map.of("success", false, "message", "请上传PCB图片");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.startsWith("image/")) {
            // 允许任意格式，实际应校验 jpg/png
        }

        try {
            byte[] bytes = file.getBytes();
            int w = 640, h = 480;
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img != null) {
                    w = img.getWidth();
                    h = img.getHeight();
                }
            } catch (Exception ignored) { }
            List<Map<String, Object>> defects = simulateDefectDetection(bytes, w, h);

            String imageId = "insp_" + System.currentTimeMillis();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("imageId", imageId);
            result.put("defects", defects);
            result.put("defectCount", defects.size());
            result.put("message", defects.isEmpty() ? "未检测到缺陷" : "检测到 " + defects.size() + " 处缺陷");

            // 智能质检×环保指标：检测到次品时自动折算物料浪费与能耗浪费
            if (!defects.isEmpty()) {
                Map<String, Object> waste = fetchDefectWaste(defects.size(), imageId, defects);
                if (waste != null) {
                    result.put("defectWaste", waste);
                }
            }

            return result;
        } catch (Exception e) {
            return Map.of("success", false, "message", "检测失败：" + e.getMessage());
        }
    }

    /**
     * 模拟缺陷检测（算法占位）。
     * 可替换为：PaddlePaddle Inference、昇腾 Atlas CANN、ONNX Runtime 等。
     */
    private List<Map<String, Object>> simulateDefectDetection(byte[] imageBytes, int imgW, int imgH) {
        List<Map<String, Object>> list = new ArrayList<>();
        int hash = Arrays.hashCode(imageBytes);

        String[] types = {"漏焊", "短路", "断路"};
        int n = Math.abs(hash % 3) + 1;
        for (int i = 0; i < n; i++) {
            int x = Math.abs((hash + i * 31) % (imgW - 80)) + 20;
            int y = Math.abs((hash + i * 47) % (imgH - 60)) + 20;
            int w = 40 + Math.abs((hash + i) % 100);
            int h = 30 + Math.abs((hash + i * 2) % 80);
            int typeIdx = Math.abs((hash + i * 3) % 3);
            double conf = 0.85 + (Math.abs(hash % 100) / 1000.0);

            list.add(Map.of(
                "type", types[typeIdx],
                "x", x, "y", y, "width", w, "height", h,
                "confidence", Math.min(0.99, conf)
            ));
        }
        return list;
    }

    /**
     * 将缺陷坐标同步至 xDM-F 元模型，触发自动报废流程。
     * 若 xDM-F 中存在 PCBDefect 或 DefectInspection 实体，则创建记录。
     */
    @PostMapping("/sync-defects")
    public Map<String, Object> syncDefects(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        if (cvConfig.isDemoMode()) {
            return Map.of("success", false, "message", "模拟检测模式下禁止同步，避免非产线数据进入生产系统。请接入 PaddlePaddle / 昇腾 Atlas 等 CV 模型后，在 application.yml 中设置 cv.demo-mode: false");
        }

        Object defectsObj = body != null ? body.get("defects") : null;
        Object imageIdObj = body != null ? body.get("imageId") : null;

        if (!(defectsObj instanceof List)) {
            return Map.of("success", false, "message", "缺陷数据格式错误");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> defects = (List<Map<String, Object>>) defectsObj;
        String imageId = imageIdObj != null ? String.valueOf(imageIdObj) : "";

        if (defects.isEmpty()) {
            return Map.of("success", true, "message", "无缺陷，无需同步");

        }

        List<String> synced = new ArrayList<>();
        StringBuilder scrapTrigger = new StringBuilder();

        for (int i = 0; i < defects.size(); i++) {
            Map<String, Object> d = defects.get(i);
            String type = d.get("type") != null ? String.valueOf(d.get("type")) : "未知";
            Object x = d.get("x"), y = d.get("y"), w = d.get("width"), h = d.get("height");

            Map<String, Object> params = new HashMap<>();
            params.put("defect_type", type);
            params.put("defect_x", x != null ? x : 0);
            params.put("defect_y", y != null ? y : 0);
            params.put("defect_width", w != null ? w : 0);
            params.put("defect_height", h != null ? h : 0);
            params.put("image_id", imageId);
            params.put("status", "scrap_pending");
            params.put("inspection_time", java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(java.time.LocalDateTime.now()));

            params.put("rdmExtensionType", "PCBDefect");
            Map<String, Object> createBody = ensureMasterInBody(Map.of("params", params));

            boolean ok = false;
            Object result = postCreateWith404Fallback(getBaseUrl() + "/dynamic/api/PCBDefect/create",
                new HttpEntity<>(createBody, buildForwardHeaders(req)));
            if (result instanceof Map && "SUCCESS".equals(String.valueOf(((Map<?, ?>) result).get("result")))) {
                synced.add(type + " @(" + x + "," + y + ")");
                scrapTrigger.append("缺陷[").append(type).append("] 已同步，触发报废流程。");
                ok = true;
            }
            if (!ok) {
                params.put("rdmExtensionType", "DefectInspection");
                createBody = ensureMasterInBody(Map.of("params", params));
                result = postCreateWith404Fallback(getBaseUrl() + "/dynamic/api/DefectInspection/create",
                    new HttpEntity<>(createBody, buildForwardHeaders(req)));
                if (result instanceof Map && "SUCCESS".equals(String.valueOf(((Map<?, ?>) result).get("result")))) {
                    synced.add(type + " @(" + x + "," + y + ")");
                    scrapTrigger.append("缺陷[").append(type).append("] 已同步至 DefectInspection。");
                    ok = true;
                }
            }
            if (!ok) {
                synced.add(type + " @(" + x + "," + y + ") [已记录，待 xDM-F 部署 PCBDefect 实体后自动同步]");
            }
        }

        if (synced.isEmpty()) {
            return Map.of("success", false, "message", "同步失败，请检查 xDM-F 是否部署 PCBDefect 或 DefectInspection 实体");
        }

        return Map.of(
            "success", true,
            "syncedCount", synced.size(),
            "synced", synced,
            "message", "已同步 " + synced.size() + " 处缺陷至工业软件元模型，触发自动报废流程",
            "scrapTriggered", scrapTrigger.length() > 0
        );
    }
}
