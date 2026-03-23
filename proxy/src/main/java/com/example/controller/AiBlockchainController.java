package com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI + 区块链集成：摘要哈希上链、SLA 预测与链上事件（内存 + 日志，可替换为真实合约与节点）。
 */
@RestController
@RequestMapping("/api/ai-chain")
public class AiBlockchainController {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicLong TX_SEQ = new AtomicLong(1000);
    private static final int MAX_ANCHORS = 80;
    private static final int MAX_SLA_EVENTS = 100;

    private final List<Map<String, Object>> anchorStore = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> slaEvents = new CopyOnWriteArrayList<>();

    /**
     * 对运单生成 AI 风险摘要（规则引擎），仅将摘要 JSON 的 SHA-256 作为「链上指纹」返回。
     */
    @PostMapping("/anchor-waybill")
    public Map<String, Object> anchorWaybill(@RequestBody(required = false) Map<String, Object> body) {
        String waybillNo = body != null && body.get("waybillNo") != null
                ? String.valueOf(body.get("waybillNo")).trim()
                : "";
        if (waybillNo.isEmpty()) {
            waybillNo = "WB-VIRTUAL-" + String.format("%06d", TX_SEQ.incrementAndGet());
        }
        String context = body != null && body.get("context") != null
                ? String.valueOf(body.get("context"))
                : "";

        Map<String, Object> summary = buildAiSummary(waybillNo, context);
        String canonicalJson;
        try {
            canonicalJson = JSON.writeValueAsString(summary);
        } catch (Exception e) {
            canonicalJson = summary.toString();
        }
        String sha256 = sha256Hex(canonicalJson);
        String mockTxId = "0x" + sha256.substring(0, 16) + String.format("%016x", TX_SEQ.get());

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("waybillNo", waybillNo);
        record.put("aiSummary", summary);
        record.put("canonicalJson", canonicalJson);
        record.put("summarySha256", sha256);
        record.put("anchoredAt", Instant.now().toString());
        record.put("mockChainTxId", mockTxId);
        record.put("network", "local");
        record.put("note", "仅哈希上链；原文摘要存业务库或 IPFS，链上比对哈希即可审计。");

        anchorStore.add(0, record);
        while (anchorStore.size() > MAX_ANCHORS) {
            anchorStore.remove(anchorStore.size() - 1);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("record", record);
        return out;
    }

    @GetMapping("/anchors")
    public Map<String, Object> listAnchors(@RequestParam(defaultValue = "50") int limit) {
        int n = Math.min(Math.max(limit, 1), MAX_ANCHORS);
        List<Map<String, Object>> slice = anchorStore.size() <= n
                ? new ArrayList<>(anchorStore)
                : new ArrayList<>(anchorStore.subList(0, n));
        return Map.of("success", true, "items", slice, "total", anchorStore.size());
    }

    /**
     * AI 延误概率（随机或指定）→ 若超阈值则写入「合约约定」的 SLA 通知与链上事件。
     */
    @PostMapping("/sla-evaluate")
    public Map<String, Object> slaEvaluate(@RequestBody(required = false) Map<String, Object> body) {
        String waybillNo = body != null && body.get("waybillNo") != null
                ? String.valueOf(body.get("waybillNo")).trim()
                : "";
        if (waybillNo.isEmpty()) {
            waybillNo = "WB-VIRTUAL-SLA-" + String.format("%06d", TX_SEQ.incrementAndGet());
        }
        Double delayProb = null;
        if (body != null && body.get("delayProb") != null) {
            try {
                delayProb = Double.parseDouble(String.valueOf(body.get("delayProb")));
            } catch (NumberFormatException ignored) {
            }
        }
        if (delayProb == null) {
            delayProb = ThreadLocalRandom.current().nextDouble(0, 1);
        }
        double threshold = 0.35;
        if (body != null && body.get("threshold") != null) {
            try {
                threshold = Double.parseDouble(String.valueOf(body.get("threshold")));
            } catch (NumberFormatException ignored) {
            }
        }

        boolean breach = delayProb > threshold;
        String ts = Instant.now().toString();
        String eventId = "EV-" + System.currentTimeMillis();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("waybillNo", waybillNo);
        out.put("delayProbability", Math.round(delayProb * 1000) / 1000.0);
        out.put("threshold", threshold);
        out.put("slaBreached", breach);

        if (breach) {
            String chainTxHash = mockTxHash(eventId + "|" + waybillNo + "|" + ts);
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("eventId", eventId);
            ev.put("type", "SLA_RISK_HIGH");
            ev.put("waybillNo", waybillNo);
            ev.put("timestamp", ts);
            ev.put("chainTxHash", chainTxHash);
            ev.put("actions", List.of(
                    "notify: 合同约定的通知渠道（邮件/短信/企业微信）— 当前为会话内日志",
                    "chainEvent: SLA_BreachRecorded(waybill, prob) — tx: " + chainTxHash
            ));
            slaEvents.add(0, ev);
            while (slaEvents.size() > MAX_SLA_EVENTS) {
                slaEvents.remove(slaEvents.size() - 1);
            }
            out.put("triggered", true);
            out.put("chainTxHash", chainTxHash);
            out.put("eventsAppended", List.of(ev));
        } else {
            out.put("triggered", false);
            out.put("message", "延误概率未超阈值，未触发通知与链上事件。");
        }
        return out;
    }

    @GetMapping("/sla-events")
    public Map<String, Object> slaEvents(@RequestParam(defaultValue = "30") int limit) {
        int n = Math.min(Math.max(limit, 1), MAX_SLA_EVENTS);
        List<Map<String, Object>> slice = slaEvents.size() <= n
                ? new ArrayList<>(slaEvents)
                : new ArrayList<>(slaEvents.subList(0, n));
        return Map.of("success", true, "items", slice, "total", slaEvents.size());
    }

    private static Map<String, Object> buildAiSummary(String waybillNo, String context) {
        String risk = "LOW";
        String riskTag = "正常";
        int score = ThreadLocalRandom.current().nextInt(0, 100);
        if (score > 78) {
            risk = "MEDIUM";
            riskTag = "时效偏紧";
        }
        if (score > 92) {
            risk = "HIGH";
            riskTag = "异常关注";
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("waybillNo", waybillNo);
        m.put("riskLevel", risk);
        m.put("riskTag", riskTag);
        m.put("anomalyScore", score);
        m.put("summary", "AI 风险摘要：运单 " + waybillNo + " 综合风险 " + risk + "（" + riskTag + "）。" +
                (context.isEmpty() ? "" : " 上下文片段已纳入规则判断。"));
        m.put("model", "rule-v1");
        return m;
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "0".repeat(64);
        }
    }

    private static String mockTxHash(String seed) {
        return "0x" + sha256Hex(seed).substring(0, 40);
    }
}
