package com.example.exception;

import java.util.Map;

/**
 * 转发 xDM 时无法继续时抛出，由全局处理器转为 200 + JSON，避免前端收到 HTTP 403 与鉴权类文案。
 */
public class XdmForwardException extends RuntimeException {

    private final Map<String, Object> body;

    public XdmForwardException(Map<String, Object> body) {
        super();
        this.body = body;
    }

    public Map<String, Object> getBody() {
        return body;
    }
}
