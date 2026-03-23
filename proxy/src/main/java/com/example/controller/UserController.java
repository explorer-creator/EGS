package com.example.controller;

import com.example.config.XdmProxyProperties;
import com.example.service.XdmMockService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 用户管理接口，转发到 xDM-F User_Infor 实体。xdm.mock-mode=true 时返回 Mock 数据。
 */
@RestController
@RequestMapping("/api")
public class UserController extends BaseXdmProxyController {

    private final XdmProxyProperties xdmConfig;
    private final XdmMockService mockService;

    public UserController(XdmProxyProperties xdmConfig, XdmMockService mockService) {
        super(xdmConfig);
        this.xdmConfig = xdmConfig;
        this.mockService = mockService;
    }

    @PostMapping("/dynamic/api/User_Infor/list")
    public Object getUserList(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("User_Infor")) return mockService.listUser();
        HttpHeaders headers = buildForwardHeaders(req);
        Map<String, Object> body = simplifyListParams(requestBody);
        return postWith404Fallback(getBaseUrl() + "/dynamic/api/User_Infor/list",
            new HttpEntity<>(body, headers));
    }

    @PostMapping("/dynamic/api/User_Infor/create")
    public Object createUser(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("User_Infor")) return mockService.createUser(requestBody);
        return postCreateWith404Fallback(getBaseUrl() + "/dynamic/api/User_Infor/create",
            new HttpEntity<>(requestBody, buildForwardHeaders(req)));
    }

    @PostMapping("/dynamic/api/User_Infor/update")
    public Object updateUser(@RequestBody Map<String, Object> requestBody, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("User_Infor")) return mockService.updateUser(requestBody);
        return postCreateWith404Fallback(getBaseUrl() + "/dynamic/api/User_Infor/update",
            new HttpEntity<>(requestBody, buildForwardHeaders(req)));
    }

    @DeleteMapping("/dynamic/api/User_Infor/delete/{id}")
    public Object deleteUser(@PathVariable String id, HttpServletRequest req) {
        if (xdmConfig.useMockForEntity("User_Infor")) {
            mockService.deleteUser(id);
            return Map.of("success", true, "message", "删除成功");
        }
        return deleteWith404Fallback(getBaseUrl() + "/dynamic/api/User_Infor/delete/" + id, req,
            "User_Infor 模型在 xDM-F 中未部署");
    }

    /** 从 xDM-F list 响应中解析出数据列表，兼容 data/rows/list/data.data.rows 等格式 */
    @SuppressWarnings("unchecked")
    private List<?> parseListFromResponse(Object res) {
        if (res == null) return Collections.emptyList();
        Map<String, Object> resMap = res instanceof Map ? (Map<String, Object>) res : Map.of();
        Object data = resMap.get("data");
        if (data == null) data = resMap;
        if (data instanceof List) return (List<?>) data;
        if (data instanceof Map) {
            Map<String, Object> dataMap = (Map<String, Object>) data;
            Object arr = dataMap.get("rows");
            if (arr == null) arr = dataMap.get("list");
            if (arr == null) arr = dataMap.get("data");
            if (arr == null) arr = dataMap.get("records");
            if (arr instanceof List) return (List<?>) arr;
        }
        return Collections.emptyList();
    }

    /** 登录：根据用户名查询用户并验证密码。Mock 模式下 admin/admin123 可登录 */
    @PostMapping("/user/login")
    public Object login(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String username = body != null && body.get("username") != null ? body.get("username").toString().trim() : "";
        String password = body != null && body.get("password") != null ? body.get("password").toString() : "";
        if (username.isEmpty() || password.isEmpty()) {
            return Map.of("success", false, "message", "用户名和密码不能为空");
        }
        if (xdmConfig.useMockForEntity("User_Infor")) {
            Map<String, Object> user = mockService.getUserByUsername(username);
            if (user == null && "admin".equals(username)) {
                user = Map.of("id", "mock_user_1", "user_name", "admin", "username", "admin", "password", "admin123");
            }
            if (user != null) {
                String storedPwd = getPasswordFromUser(user);
                if (storedPwd.isEmpty() || password.equals(storedPwd)) {
                    Map<String, Object> safeUser = new java.util.HashMap<>(user);
                    safeUser.remove("password");
                    safeUser.remove("Password");
                    return Map.of("success", true, "user", safeUser);
                }
            }
            return Map.of("success", false, "message", "用户名或密码错误");
        }
        try {
            Map<String, Object> listBody = Map.of("params", Map.of(
                "conditions", List.of(Map.of("conditionName", "username", "operator", "=", "conditionValues", List.of(username))),
                "pageVO", Map.of("curPage", 1, "pageSize", 10, "totalRows", 0, "totalPages", 0, "limit", 10, "offset", 0)
            ));
            Object res = restTemplate.postForObject(getBaseUrl() + "/dynamic/api/User_Infor/list",
                new HttpEntity<>(listBody, buildForwardHeaders(req)), Object.class);
            List<?> list = parseListFromResponse(res);
            if (list.isEmpty()) {
                return Map.of("success", false, "message", "用户名或密码错误");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) list.get(0);
            String storedPwd = getPasswordFromUser(user);
            if (!storedPwd.isEmpty()) {
                if (!password.equals(storedPwd)) {
                    return Map.of("success", false, "message", "用户名或密码错误");
                }
            } else if (!xdmConfig.isLoginAllowNoPassword()) {
                return Map.of("success", false, "message", "xDM-F list 未返回密码字段，无法验证。请在设计态配置 User_Infor 使 list 返回 password，或设置 xdm.login-allow-no-password=true");
            }
            Map<String, Object> safeUser = new java.util.HashMap<>(user);
            safeUser.remove("password");
            safeUser.remove("Password");
            return Map.of("success", true, "user", safeUser);
        } catch (Exception e) {
            return Map.of("success", false, "message", "登录失败：" + e.getMessage());
        }
    }

    /** 从用户对象中获取密码，兼容 password/Password 等字段名 */
    private String getPasswordFromUser(Map<String, Object> user) {
        if (user == null) return "";
        Object p = user.get("password");
        if (p == null) p = user.get("Password");
        if (p == null) p = user.get("pwd");
        return p != null ? p.toString() : "";
    }
}
