package com.example.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * xDM-F Mock 服务：当 8003 不可用时返回模拟数据，便于队友无 8003 环境本地开发。
 * 数据存储在内存，重启后清空。
 */
@Service
public class XdmMockService {

    private static final String MOCK_PREFIX = "mock_";
    private final AtomicLong idGen = new AtomicLong(1000);

    private final Map<String, Map<String, Object>> equipmentStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> materialStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> procedureStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> routeStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> routeProcStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> procEquipStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> procMaterialStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> userStore = new ConcurrentHashMap<>();

    public XdmMockService() {
        initDemoData();
    }

    private void initDemoData() {
        // 设备
        putEquipment(Map.of(
            "id", "mock_eq_1", "equipment_code", "EQ-001", "equipment_name", "SMT贴片机",
            "manufacturer", "示例厂商", "brand", "", "model", "SMT-100", "location", "产线A"
        ));
        putEquipment(Map.of(
            "id", "mock_eq_2", "equipment_code", "EQ-002", "equipment_name", "AOI检测机",
            "manufacturer", "示例厂商", "brand", "", "model", "AOI-200", "location", "产线A"
        ));
        // 物料
        putMaterial(Map.of(
            "id", "mock_mat_1", "material_code", "MAT-001", "material_name", "PCB基板",
            "model", "", "stock_quantity", 100, "supplier", ""
        ));
        putMaterial(Map.of(
            "id", "mock_mat_2", "material_code", "MAT-002", "material_name", "主控芯片",
            "model", "", "stock_quantity", 50, "supplier", ""
        ));
        // 工序
        putProcedure(Map.of(
            "id", "mock_proc_1", "procedure_code", "PROC-001", "procedure_name", "贴片",
            "production_steps", "SMT贴片", "production_inspection_equipment", "AOI"
        ));
        putProcedure(Map.of(
            "id", "mock_proc_2", "procedure_code", "PROC-002", "procedure_name", "检测",
            "production_steps", "AOI检测", "production_inspection_equipment", ""
        ));
        // 工艺路线
        putRoute(Map.of(
            "id", "mock_route_1", "route_code", "ROUTE-001", "route_name", "示例工艺路线",
            "product", "示例产品", "version", "1.0"
        ));
        // 用户（用于登录）
        userStore.put("admin", Map.of(
            "id", "mock_user_1", "user_name", "admin", "password", "admin123",
            "username", "admin"
        ));
    }

    private String nextId() { return MOCK_PREFIX + idGen.incrementAndGet(); }

    private void putEquipment(Map<String, Object> m) {
        String id = (String) m.get("id");
        if (id == null) id = nextId();
        equipmentStore.put(id, new HashMap<>(m));
    }
    private void putMaterial(Map<String, Object> m) {
        String id = (String) m.get("id");
        if (id == null) id = nextId();
        materialStore.put(id, new HashMap<>(m));
    }
    private void putProcedure(Map<String, Object> m) {
        String id = (String) m.get("id");
        if (id == null) id = nextId();
        procedureStore.put(id, new HashMap<>(m));
    }
    private void putRoute(Map<String, Object> m) {
        String id = (String) m.get("id");
        if (id == null) id = nextId();
        routeStore.put(id, new HashMap<>(m));
    }

    // ========== List ==========
    public Object listEquipment() {
        return Map.of("result", "SUCCESS", "data", Map.of("rows", new ArrayList<>(equipmentStore.values())));
    }
    public Object listMaterial() {
        return Map.of("result", "SUCCESS", "data", Map.of("rows", new ArrayList<>(materialStore.values())));
    }
    public Object listProcedure() {
        return Map.of("result", "SUCCESS", "data", Map.of("rows", new ArrayList<>(procedureStore.values())));
    }
    public Object listProcessRoute() {
        return Map.of("result", "SUCCESS", "data", Map.of("rows", new ArrayList<>(routeStore.values())));
    }
    public Object listProcessRouteProcedure() {
        return Map.of("result", "SUCCESS", "data", Map.of("rows", new ArrayList<>(routeProcStore.values())));
    }
    public Object listProcedureEquipment() {
        return Map.of("result", "SUCCESS", "data", Map.of("rows", new ArrayList<>(procEquipStore.values())));
    }
    public Object listProcedureMaterial() {
        return Map.of("result", "SUCCESS", "data", Map.of("rows", new ArrayList<>(procMaterialStore.values())));
    }
    public Object listUser() {
        return Map.of("result", "SUCCESS", "data", Map.of("rows", new ArrayList<>(userStore.values())));
    }
    /** 按用户名查找用户，用于 Mock 登录 */
    public Map<String, Object> getUserByUsername(String username) {
        if (username == null || username.isEmpty()) return null;
        return userStore.get(username);
    }
    public Object listMaterialVersion() {
        return Map.of("result", "SUCCESS", "data", Map.of("rows", Collections.emptyList()));
    }
    public Object listMaterialCategory() {
        return Map.of("result", "SUCCESS", "data", Map.of("rows", Collections.emptyList()));
    }
    public Object listMaterialBOM() {
        return Map.of("result", "SUCCESS", "data", Map.of("rows", Collections.emptyList()));
    }
    public Object listFeedback() {
        return Map.of("result", "SUCCESS", "data", Map.of("rows", Collections.emptyList()));
    }

    // ========== Create ==========
    @SuppressWarnings("unchecked")
    public Object createEquipment(Map<String, Object> body) {
        Map<String, Object> params = (Map<String, Object>) (body != null ? body.get("params") : null);
        if (params == null) params = Map.of();
        String id = nextId();
        Map<String, Object> m = new HashMap<>(params);
        m.put("id", id);
        if (!m.containsKey("equipment_code")) m.put("equipment_code", "EQ-" + idGen.get());
        if (!m.containsKey("equipment_name")) m.put("equipment_name", "设备");
        putEquipment(m);
        return Map.of("result", "SUCCESS", "params", Map.of("id", id));
    }
    @SuppressWarnings("unchecked")
    public Object createMaterial(Map<String, Object> body) {
        Map<String, Object> params = (Map<String, Object>) (body != null ? body.get("params") : null);
        if (params == null) params = Map.of();
        String id = nextId();
        Map<String, Object> m = new HashMap<>(params);
        m.put("id", id);
        if (!m.containsKey("material_code")) m.put("material_code", "MAT-" + idGen.get());
        if (!m.containsKey("material_name")) m.put("material_name", "物料");
        putMaterial(m);
        return Map.of("result", "SUCCESS", "params", Map.of("id", id));
    }
    @SuppressWarnings("unchecked")
    public Object createProcedure(Map<String, Object> body) {
        Map<String, Object> params = (Map<String, Object>) (body != null ? body.get("params") : null);
        if (params == null) params = Map.of();
        String id = nextId();
        Map<String, Object> m = new HashMap<>(params);
        m.put("id", id);
        if (!m.containsKey("procedure_code")) m.put("procedure_code", "PROC-" + idGen.get());
        if (!m.containsKey("procedure_name")) m.put("procedure_name", "工序");
        putProcedure(m);
        return Map.of("result", "SUCCESS", "params", Map.of("id", id));
    }
    @SuppressWarnings("unchecked")
    public Object createProcessRoute(Map<String, Object> body) {
        Map<String, Object> params = (Map<String, Object>) (body != null ? body.get("params") : null);
        if (params == null) params = Map.of();
        String id = nextId();
        Map<String, Object> m = new HashMap<>(params);
        m.put("id", id);
        if (!m.containsKey("route_code")) m.put("route_code", "ROUTE-" + idGen.get());
        if (!m.containsKey("route_name")) m.put("route_name", "工艺路线");
        putRoute(m);
        return Map.of("result", "SUCCESS", "params", Map.of("id", id));
    }
    @SuppressWarnings("unchecked")
    public Object createProcessRouteProcedure(Map<String, Object> body) {
        Map<String, Object> params = (Map<String, Object>) (body != null ? body.get("params") : null);
        if (params == null) params = Map.of();
        String id = nextId();
        Map<String, Object> m = new HashMap<>(params);
        m.put("id", id);
        routeProcStore.put(id, m);
        return Map.of("result", "SUCCESS", "params", Map.of("id", id));
    }
    @SuppressWarnings("unchecked")
    public Object createProcedureEquipment(Map<String, Object> body) {
        Map<String, Object> params = (Map<String, Object>) (body != null ? body.get("params") : null);
        if (params == null) params = Map.of();
        String id = nextId();
        Map<String, Object> m = new HashMap<>(params);
        m.put("id", id);
        procEquipStore.put(id, m);
        return Map.of("result", "SUCCESS", "params", Map.of("id", id));
    }
    @SuppressWarnings("unchecked")
    public Object createProcedureMaterial(Map<String, Object> body) {
        Map<String, Object> params = (Map<String, Object>) (body != null ? body.get("params") : null);
        if (params == null) params = Map.of();
        String id = nextId();
        Map<String, Object> m = new HashMap<>(params);
        m.put("id", id);
        procMaterialStore.put(id, m);
        return Map.of("result", "SUCCESS", "params", Map.of("id", id));
    }
    @SuppressWarnings("unchecked")
    public Object createUser(Map<String, Object> body) {
        Map<String, Object> params = (Map<String, Object>) (body != null ? body.get("params") : null);
        if (params == null) params = Map.of();
        String id = nextId();
        Map<String, Object> m = new HashMap<>(params);
        m.put("id", id);
        String uname = (String) m.get("user_name");
        if (uname == null) uname = (String) m.get("username");
        if (uname != null) userStore.put(uname, m);
        return Map.of("result", "SUCCESS", "params", Map.of("id", id));
    }
    public Object createFeedback(Map<String, Object> body) {
        return Map.of("result", "SUCCESS", "params", Map.of("id", nextId()));
    }
    public Object createMaterialVersion(Map<String, Object> body) {
        return Map.of("result", "SUCCESS", "params", Map.of("id", nextId()));
    }
    public Object createMaterialCategory(Map<String, Object> body) {
        return Map.of("result", "SUCCESS", "params", Map.of("id", nextId()));
    }
    public Object createMaterialBOM(Map<String, Object> body) {
        return Map.of("result", "SUCCESS", "params", Map.of("id", nextId()));
    }

    // ========== Update ==========
    public Object updateEquipment(Map<String, Object> body) {
        return Map.of("result", "SUCCESS");
    }
    public Object updateMaterial(Map<String, Object> body) {
        return Map.of("result", "SUCCESS");
    }
    @SuppressWarnings("unchecked")
    public Object updateUser(Map<String, Object> body) {
        Map<String, Object> params = (Map<String, Object>) (body != null ? body.get("params") : null);
        if (params != null && params.containsKey("id")) {
            String id = params.get("id").toString();
            for (Map.Entry<String, Map<String, Object>> e : userStore.entrySet()) {
                if (id.equals(e.getValue().get("id"))) {
                    e.getValue().putAll(params);
                    return Map.of("result", "SUCCESS");
                }
            }
        }
        return Map.of("result", "SUCCESS");
    }

    // ========== Delete ==========
    public void deleteEquipment(String id) {
        equipmentStore.remove(id);
    }
    public void deleteMaterial(String id) {
        materialStore.remove(id);
    }
    public void deleteProcedure(String id) {
        procedureStore.remove(id);
    }
    public void deleteProcessRoute(String id) {
        routeStore.remove(id);
    }
    public void deleteProcessRouteProcedure(String id) {
        routeProcStore.remove(id);
    }
    public void deleteProcedureEquipment(String id) {
        procEquipStore.remove(id);
    }
    public void deleteProcedureMaterial(String id) {
        procMaterialStore.remove(id);
    }
    public void deleteUser(String id) {
        userStore.entrySet().removeIf(e -> id.equals(e.getValue().get("id")));
    }
    public void deleteMaterialCategory(String id) {
        // no-op
    }
    public void deleteMaterialBOM(String id) {
        // no-op
    }
}
