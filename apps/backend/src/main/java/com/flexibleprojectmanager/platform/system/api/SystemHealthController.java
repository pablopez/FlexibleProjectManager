package com.flexibleprojectmanager.platform.system.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flexibleprojectmanager.platform.system.application.HealthService;
import com.flexibleprojectmanager.platform.system.application.HealthService.HealthCheckResult;
import com.flexibleprojectmanager.platform.system.application.HealthService.HealthStatus;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    private final HealthService healthService;

    public SystemHealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthCheckResult result = healthService.check();
        HealthResponse response = new HealthResponse(
                toApiStatus(result.status()),
                toApiStatus(result.backend()),
                toApiStatus(result.database()),
                result.timestamp());
        int status = result.database() == HealthStatus.UP ? 200 : 503;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/readiness")
    public ResponseEntity<HealthResponse> readiness() {
        return health();
    }

    @GetMapping("/liveness")
    public HealthStatusResponse liveness() {
        return new HealthStatusResponse("UP");
    }

    private HealthComponentStatus toApiStatus(HealthStatus status) {
        return HealthComponentStatus.valueOf(status.name());
    }

    public record HealthStatusResponse(String status) {}
}
