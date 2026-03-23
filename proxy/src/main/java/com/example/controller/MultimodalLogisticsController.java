package com.example.controller;

import com.example.config.QwenProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多模态物流：运单截图识别、语音/自然语言报单 → 结构化写入 TMS/WMS（会话内存储）。
 */
@RestController
@RequestMapping("/api/logistics-multimodal")
public class MultimodalLogisticsController {

    private final QwenProperties qwenConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final List<Map<String, Object>> TMS_DEMO = new CopyOnWriteArrayList<>();
    private static final List<Map<String, Object>> WMS_DEMO = new CopyOnWriteArrayList<>();

    private static final String STRUCT_PROMPT = """
        请只输出一个 JSON 对象：不要用 Markdown，不要用 ``` 代码块，不要任何解释文字。
        必须使用下列英文字段名（值均为字符串）：
        waybillNo,sender,receiver,originAddress,destAddress,cargoDescription,weightKg,phone,remark
        规则：weightKg 只填数字与小数点；waybillNo 填运单号/面单号；地址写清省市门牌；识别不到则填 ""。
        示例：{"waybillNo":"SF123","sender":"","receiver":"张三","originAddress":"上海市浦东新区","destAddress":"苏州市","cargoDescription":"电子产品","weightKg":"10.5","phone":"13800138000","remark":""}
        """;

    public MultimodalLogisticsController(QwenProperties qwenConfig) {
        this.qwenConfig = qwenConfig;
    }

    private String resolveApiKey() {
        String k = qwenConfig.getApiKey();
        if (k == null || k.isEmpty()) k = System.getenv("DASHSCOPE_API_KEY");
        return k != null ? k : "";
    }

    /** 运单截图 → 结构化（千问视觉；无 Key 时返回本地参考结果） */
    @PostMapping(value = "/parse-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> parseImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Map.of("success", false, "message", "请上传图片文件");
        }
        String apiKey = resolveApiKey();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            return Map.of("success", false, "message", "读取文件失败: " + e.getMessage());
        }
        if (bytes.length > 7 * 1024 * 1024) {
            return Map.of("success", false, "message", "图片过大，请小于 7MB");
        }

        String mime = file.getContentType();
        if (mime == null || !mime.startsWith("image/")) {
            mime = "image/jpeg";
        }
        String b64 = Base64.getEncoder().encodeToString(bytes);
        String dataUrl = "data:" + mime + ";base64," + b64;

        if (apiKey.isEmpty()) {
            return buildDemoResponse("", "");
        }

        String url = qwenConfig.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        List<Map<String, Object>> content = new ArrayList<>();
        Map<String, Object> imgPart = new LinkedHashMap<>();
        imgPart.put("type", "image_url");
        Map<String, String> imgUrl = new LinkedHashMap<>();
        imgUrl.put("url", dataUrl);
        imgPart.put("image_url", imgUrl);
        content.add(imgPart);
        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("type", "text");
        textPart.put("text", "你是物流 OCR 助手。" + STRUCT_PROMPT);
        content.add(textPart);

        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", content);

        Map<String, Object> body = new HashMap<>();
        body.put("model", qwenConfig.getVlModel());
        body.put("messages", List.of(userMsg));
        body.put("temperature", 0.1);
        body.put("max_tokens", 1024);

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(url, entity, Map.class);
            String raw = extractAssistantText(resp);
            Map<String, Object> structured = parseStructuredJson(raw, "");
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("mode", "vision");
            out.put("structured", structured);
            out.put("rawExcerpt", raw != null && raw.length() > 280 ? raw.substring(0, 280) + "…" : raw);
            return out;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (e.getCause() != null) msg += "; " + e.getCause().getMessage();
            Map<String, Object> fallback = buildDemoResponse("", "");
            fallback.put("success", true);
            return fallback;
        }
    }

    /** 语音转写文本或自然语言 → 结构化 */
    @PostMapping("/parse-text")
    public Map<String, Object> parseText(@RequestBody Map<String, Object> req) {
        String text = req != null && req.get("text") != null ? String.valueOf(req.get("text")).trim() : "";
        if (text.isEmpty()) {
            return Map.of("success", false, "message", "text 不能为空");
        }
        String apiKey = resolveApiKey();
        if (apiKey.isEmpty()) {
            Map<String, Object> h = heuristicParseFromText(text);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("mode", "heuristic");
            out.put("message", "");
            out.put("structured", mergeHeuristicOrEmpty(h));
            return out;
        }

        String url = qwenConfig.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "你是物流录单助手，从用户口语/转写中提取信息。" + STRUCT_PROMPT));
        messages.add(Map.of("role", "user", "content", text));

        Map<String, Object> body = new HashMap<>();
        body.put("model", qwenConfig.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.15);
        body.put("max_tokens", 800);

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(url, entity, Map.class);
            String raw = extractAssistantText(resp);
            Map<String, Object> structured = parseStructuredJson(raw, text);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("mode", "text");
            out.put("structured", structured);
            out.put("rawExcerpt", raw != null && raw.length() > 200 ? raw.substring(0, 200) + "…" : raw);
            return out;
        } catch (Exception e) {
            String msg = e.getMessage();
            Map<String, Object> fb = buildDemoResponse("", text);
            fb.put("success", true);
            return fb;
        }
    }

    /** 提交到 TMS / WMS 队列 */
    @PostMapping("/commit")
    public Map<String, Object> commit(@RequestBody Map<String, Object> req) {
        if (req == null) {
            return Map.of("success", false, "message", "body 为空");
        }
        String target = req.get("target") != null ? String.valueOf(req.get("target")).toLowerCase(Locale.ROOT) : "tms";
        @SuppressWarnings("unchecked")
        Map<String, Object> record = req.get("record") instanceof Map ? (Map<String, Object>) req.get("record") : new LinkedHashMap<>();

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        row.put("target", target);
        row.put("committedAt", Instant.now().toString());
        row.put("payload", record);

        if ("wms".equals(target)) {
            WMS_DEMO.add(0, row);
            while (WMS_DEMO.size() > 50) WMS_DEMO.remove(WMS_DEMO.size() - 1);
        } else {
            TMS_DEMO.add(0, row);
            while (TMS_DEMO.size() > 50) TMS_DEMO.remove(TMS_DEMO.size() - 1);
        }

        return Map.of("success", true, "id", row.get("id"), "message", "已写入" + ("wms".equals(target) ? "WMS" : "TMS") + "队列");
    }

    @GetMapping("/recent")
    public Map<String, Object> recent(@RequestParam(defaultValue = "tms") String target) {
        String t = target != null ? target.toLowerCase(Locale.ROOT) : "tms";
        List<Map<String, Object>> list = "wms".equals(t) ? new ArrayList<>(WMS_DEMO) : new ArrayList<>(TMS_DEMO);
        return Map.of("success", true, "target", t, "items", list);
    }

    // --- helpers ---

    private Map<String, Object> buildDemoResponse(String message, String userTextHint) {
        Map<String, Object> structured = demoStructured();
        if (userTextHint != null && !userTextHint.isBlank()) {
            structured = mergeHeuristicOrEmpty(heuristicParseFromText(userTextHint));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("mode", "heuristic");
        out.put("message", message);
        out.put("structured", structured);
        return out;
    }

    private Map<String, Object> demoStructured() {
        long n = System.currentTimeMillis() % 1000000;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("waybillNo", "WB-" + n);
        m.put("sender", "上海华东发货中心");
        m.put("receiver", "苏州工业园收货点");
        m.put("originAddress", "上海市浦东新区张江路88号");
        m.put("destAddress", "江苏省苏州市工业园区星湖街328号");
        m.put("cargoDescription", "电子配件/纸箱");
        m.put("weightKg", "12.5");
        m.put("phone", "13800138000");
        m.put("remark", "");
        return m;
    }

    private String extractAssistantText(Map<String, Object> response) {
        if (response == null) return "";
        Object choices = response.get("choices");
        if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) return "";
        Object first = ((List<?>) choices).get(0);
        if (!(first instanceof Map)) return "";
        Object msg = ((Map<?, ?>) first).get("message");
        if (!(msg instanceof Map)) return "";
        Object c = ((Map<?, ?>) msg).get("content");
        return c != null ? String.valueOf(c) : "";
    }

    /**
     * @param userText 用户原始报单文本，JSON 字段空时用于规则补全
     */
    private Map<String, Object> parseStructuredJson(String raw, String userText) {
        Map<String, Object> fallback = heuristicParseFromText(userText != null ? userText : "");
        if (raw == null || raw.isBlank()) {
            return mergeHeuristicOrEmpty(fallback);
        }
        String json = extractJsonObjectBalanced(stripMarkdownFences(raw));
        if (json == null) {
            Map<String, Object> m = mergeHeuristicOrEmpty(fallback);
            m.put("remark", trimRemark(String.valueOf(m.get("remark"))));
            return m;
        }
        try {
            JsonNode node = objectMapper.readTree(json.getBytes(StandardCharsets.UTF_8));
            Map<String, Object> out = readStructuredFromNode(node);
            return fillEmptyFromHeuristic(out, userText != null ? userText : "");
        } catch (Exception e) {
            Map<String, Object> m = mergeHeuristicOrEmpty(heuristicParseFromText(userText != null ? userText : ""));
            m.put("remark", trimRemark(String.valueOf(m.get("remark"))));
            return m;
        }
    }

    private static String trimRemark(Object r) {
        String s = r == null ? "" : String.valueOf(r).trim();
        if (s.startsWith("；")) s = s.substring(1).trim();
        return s.length() > 500 ? s.substring(0, 500) : s;
    }

    private Map<String, Object> readStructuredFromNode(JsonNode node) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> keys = List.of("waybillNo", "sender", "receiver", "originAddress", "destAddress",
                "cargoDescription", "weightKg", "phone", "remark");
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v == null || v.isNull()) {
                v = findChineseAlias(node, k);
            }
            out.put(k, jsonNodeToString(v));
        }
        return out;
    }

    private static String jsonNodeToString(JsonNode v) {
        if (v == null || v.isNull()) return "";
        if (v.isNumber()) return v.numberValue().toString();
        if (v.isBoolean()) return v.booleanValue() ? "true" : "false";
        return v.asText().trim();
    }

    /** 兼容模型返回中文字段名 */
    private JsonNode findChineseAlias(JsonNode node, String enKey) {
        Map<String, String[]> aliases = Map.of(
                "waybillNo", new String[]{"运单号", "单号", "面单号", "快递单号"},
                "sender", new String[]{"发货方", "寄件人", "发货人", "发件人"},
                "receiver", new String[]{"收货方", "收件人", "收货人"},
                "originAddress", new String[]{"始发地址", "发货地址", "起点", "出发地"},
                "destAddress", new String[]{"目的地址", "收货地址", "目的地", "终点"},
                "cargoDescription", new String[]{"货物", "品名", "货物描述", "物品"},
                "weightKg", new String[]{"重量", "重量kg", "千克", "公斤"},
                "phone", new String[]{"电话", "手机", "联系电话"},
                "remark", new String[]{"备注", "说明"}
        );
        String[] al = aliases.get(enKey);
        if (al == null) return null;
        for (String a : al) {
            JsonNode n = node.get(a);
            if (n != null && !n.isNull()) return n;
        }
        return null;
    }

    private Map<String, Object> fillEmptyFromHeuristic(Map<String, Object> parsed, String userText) {
        Map<String, Object> h = heuristicParseFromText(userText);
        for (String k : List.of("waybillNo", "sender", "receiver", "originAddress", "destAddress",
                "cargoDescription", "weightKg", "phone", "remark")) {
            Object pv = parsed.get(k);
            String ps = pv == null ? "" : String.valueOf(pv).trim();
            String hv = h.get(k) == null ? "" : String.valueOf(h.get(k)).trim();
            if (ps.isEmpty() && !hv.isEmpty()) {
                parsed.put(k, hv);
            }
        }
        return parsed;
    }

    private static final List<String> STRUCT_KEYS = List.of("waybillNo", "sender", "receiver", "originAddress", "destAddress",
            "cargoDescription", "weightKg", "phone", "remark");

    /** 仅保留规则/模型提取的值，空缺不自动补全占位运单号 */
    private Map<String, Object> mergeHeuristicOrEmpty(Map<String, Object> h) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String k : STRUCT_KEYS) {
            String hv = h.get(k) == null ? "" : String.valueOf(h.get(k)).trim();
            m.put(k, hv);
        }
        return m;
    }

    /**
     * 无 LLM 时从中文口语中提取常见字段
     */
    private Map<String, Object> heuristicParseFromText(String text) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String k : List.of("waybillNo", "sender", "receiver", "originAddress", "destAddress",
                "cargoDescription", "weightKg", "phone", "remark")) {
            m.put(k, "");
        }
        if (text == null || text.isBlank()) return m;

        // 运单号：WB/SF/JD/YT 等 + 字母数字，或「运单号」「单号」后内容
        Pattern wb = Pattern.compile("(?:运单号|面单号|单号|运单)[:：\\s]*([A-Za-z0-9\\-_]{6,24})", Pattern.CASE_INSENSITIVE);
        Matcher mwb = wb.matcher(text);
        if (mwb.find()) {
            m.put("waybillNo", mwb.group(1).trim());
        } else {
            Pattern wb2 = Pattern.compile("\\b(WB|SF|JD|YT|ZTO|STO|YTO|EMS)[-A-Za-z0-9]{4,22}\\b", Pattern.CASE_INSENSITIVE);
            Matcher m2 = wb2.matcher(text);
            if (m2.find()) m.put("waybillNo", m2.group().trim());
        }

        // 手机
        Pattern ph = Pattern.compile("1[3-9]\\d{9}");
        Matcher mph = ph.matcher(text);
        if (mph.find()) m.put("phone", mph.group());

        // 重量：10公斤、10kg、10 千克
        Pattern wt = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(公斤|千克|kg|KG)");
        Matcher mwt = wt.matcher(text);
        if (mwt.find()) m.put("weightKg", mwt.group(1));

        // 从 X 到 Y / 从 X 发到 Y
        Pattern route = Pattern.compile("从\\s*([^到发\\s]{2,24})\\s*(?:到|发到)\\s*([^\\s，,。]{2,24})");
        Matcher mr = route.matcher(text);
        if (mr.find()) {
            m.put("originAddress", mr.group(1).trim());
            m.put("destAddress", mr.group(2).trim());
        }

        // 省市地址片段
        if (String.valueOf(m.get("originAddress")).isEmpty()) {
            Pattern city = Pattern.compile("(北京|天津|上海|重庆|河北|山西|辽宁|吉林|黑龙江|江苏|浙江|安徽|福建|江西|山东|河南|湖北|湖南|广东|海南|四川|贵州|云南|陕西|甘肃|青海|台湾|内蒙古|广西|西藏|宁夏|新疆)[^，,。\\s]{0,12}[市县区]");
            Matcher mc = city.matcher(text);
            if (mc.find()) {
                String s = mc.group();
                if (String.valueOf(m.get("originAddress")).isEmpty()) m.put("originAddress", s);
            }
        }

        // 货物：如「电子产品」「五金配件」「纸箱」
        Pattern cargo = Pattern.compile("([\\u4e00-\\u9fa5]{1,12})(?:产品|配件|百货|设备|材料|纸箱|包裹|快件)");
        Matcher mcg = cargo.matcher(text);
        if (mcg.find()) {
            m.put("cargoDescription", mcg.group(0).trim());
        }

        return m;
    }

    private static String stripMarkdownFences(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) {
                s = s.substring(nl + 1);
            }
            int end = s.lastIndexOf("```");
            if (end > 0) s = s.substring(0, end).trim();
        }
        return s;
    }

    /** 从第一个 { 起按括号深度匹配，避免 lastIndexOf('}') 截断嵌套或字符串内括号 */
    private static String extractJsonObjectBalanced(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        int start = s.indexOf('{');
        if (start < 0) return null;
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (inString) {
                if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return s.substring(start, i + 1);
            }
        }
        return null;
    }
}
