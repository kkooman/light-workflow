package com.kkooman.lightworkflow.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @Value("${lightworkflow.maintenance.file-path:/tmp/lightworkflow-maintenance.marker}")
    private String maintenanceFilePath;

    @GetMapping(value = "/status.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> status() throws IOException {
        boolean maintenance = Files.exists(Paths.get(maintenanceFilePath));
        String body = maintenance
                ? "<html><head><title>Maintenance</title></head><body><h1>서비스 점검 중입니다.</h1><p>배포 진행 중으로 일시적으로 서비스를 중단합니다.</p></body></html>"
                : "<html><head><title>OK</title></head><body><h1>서비스 정상</h1><p>배포 전 상태 확인</p></body></html>";

        if (maintenance) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.TEXT_HTML)
                    .body(body);
        }

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body);
    }

    @GetMapping(value = "/api/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> health() throws IOException {
        boolean maintenance = Files.exists(Paths.get(maintenanceFilePath));
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("status", maintenance ? "MAINTENANCE" : "UP");
        data.put("maintenance", maintenance);
        data.put("service", "light-workflow");

        if (maintenance) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.fail("MAINTENANCE", "서비스 점검 중입니다."));
        }

        return ResponseEntity.ok(ApiResponse.success(data, "서비스 정상 상태"));
    }

    @GetMapping(value = "/api/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> statusJson() throws IOException {
        return health();
    }
}
