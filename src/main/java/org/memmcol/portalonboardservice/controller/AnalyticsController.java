package org.memmcol.portalonboardservice.controller;

import org.memmcol.portalonboardservice.service.analytics.AnalyticsService;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/gfPortal/analytic/service")
public class AnalyticsController {

    @Autowired private AnalyticsService service;
    @Autowired private GlobalExceptionHandler exception;

    @GetMapping("/all")
    ResponseEntity<?> getAnalytics(@RequestParam int year,
                                   @RequestParam int month) {
        try {
            Map<String, Object> result = service.getAnalytics(year, month);

            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
