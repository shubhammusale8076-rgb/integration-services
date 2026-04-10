package com.integration_service.controller;

import com.integration_service.service.ManualExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/execute")
@RequiredArgsConstructor
public class ManualExecutionController {

    private final ManualExecutionService manualService;

    @PostMapping("/{service}")
    public ResponseEntity<?> execute(
            @PathVariable String service,
            @RequestBody Map<String, Object> data) {

        Object response = manualService.execute(service, data);

        return ResponseEntity.ok(response);
    }
}