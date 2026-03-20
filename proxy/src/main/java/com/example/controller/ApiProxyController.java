package com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 通用 API 代理：处理 OPTIONS 预检（避免 405 Method Not Allowed）。
 * 浏览器 CORS 预检时发送 OPTIONS，若代理层无 OPTIONS 映射会返回 405。
 */
@RestController
@RequestMapping("/api")
public class ApiProxyController {

    /** CORS 预检：OPTIONS 请求直接返回 200，避免 405 */
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok().build();
    }
}
