package com.example.controller;

import com.example.service.EtaRealtimeService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 当前天气：复用 {@link EtaRealtimeService#fetchOpenMeteoCurrent}。
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final EtaRealtimeService etaRealtimeService;

    public WeatherController(EtaRealtimeService etaRealtimeService) {
        this.etaRealtimeService = etaRealtimeService;
    }

    /**
     * 当前天气（WMO weather_code）与 10m 风速。
     * <p>示例：<code>GET /api/weather/open-meteo/current?latitude=31.23&longitude=121.47</code></p>
     */
    @GetMapping("/open-meteo/current")
    public Map<String, Object> openMeteoCurrent(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", "纬度应在 [-90,90]，经度应在 [-180,180]");
            return err;
        }
        return etaRealtimeService.fetchOpenMeteoCurrent(latitude, longitude);
    }
}
