package com.example.controller;

import com.example.exception.XdmForwardException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class XdmProxyExceptionHandler {

    @ExceptionHandler(XdmForwardException.class)
    public ResponseEntity<Map<String, Object>> handleForward(XdmForwardException e) {
        return ResponseEntity.ok(e.getBody());
    }
}
