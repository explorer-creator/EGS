package com.example.controller;

import com.example.config.XdmProxyProperties;
import com.example.service.XdmMockService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 反馈箱接口，转发到 xDM-F Feedback 实体。xdm.mock-mode=true 时返回 Mock 数据。
 */
@RestController
@RequestMapping("/api")
public class FeedbackController extends BaseXdmProxyController {

    private final XdmMockService mockService;

    public FeedbackController(XdmProxyProperties xdmConfig, XdmMockService mockService) {
        super(xdmConfig);
        this.mockService = mockService;
    }

    @PostMapping("/dynamic/api/Feedback/list")
    public Object getFeedbackList(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.listFeedback();
        HttpHeaders headers = buildForwardHeaders(req);
        Map<String, Object> body = simplifyListParams(requestBody);
        return postWith404Fallback(getBaseUrl() + "/dynamic/api/Feedback/list",
            new HttpEntity<>(body, headers));
    }

    @PostMapping("/dynamic/api/Feedback/create")
    public Object createFeedback(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.isMockMode()) return mockService.createFeedback(requestBody);
        return postCreateWith404Fallback(getBaseUrl() + "/dynamic/api/Feedback/create",
            new HttpEntity<>(requestBody, buildForwardHeaders(req)));
    }
}
