package org.memmcol.portalonboardservice.controller;

import org.memmcol.portalonboardservice.service.analytics.AnalyticsService;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Map;

@RestController
@RequestMapping("/gfPortal/analytic/service")
public class AnalyticsController {

    @Autowired private AnalyticsService service;
    @Autowired private GlobalExceptionHandler exception;

    @GetMapping("/all")
    public ResponseEntity<?> getAnalytics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day) {
        try {
            // Default to current date if params missing
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            int resolvedYear = (year != null) ? year : today.getYear();
            int resolvedMonth = (month != null) ? month : today.getMonthValue();
//            int resolvedDay = (day != null) ? day : 0; // keep nullable

            Map<String, Object> result = service.getAnalytics(resolvedYear, resolvedMonth);

            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


//    @GetMapping("/all")
//    public ResponseEntity<?> getAnalytics(
//            @RequestParam(required = false) Integer year,
//            @RequestParam(required = false) Integer month) {
//        try {
//            // Default values if not provided
//            YearMonth current = YearMonth.now(ZoneOffset.UTC);
//            int resolvedYear = (year != null) ? year : current.getYear();
//            int resolvedMonth = (month != null) ? month : current.getMonthValue();
//
//            Map<String, Object> result = service.getAnalytics(resolvedYear, resolvedMonth);
//
//            return ResponseEntity.ok(result);
//        } catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//    }


    ///
//    @GetMapping("/all")
//    ResponseEntity<?> getAnalytics(@RequestParam int year,
//                                   @RequestParam int month) {
//        try {
//            Map<String, Object> result = service.getAnalytics(year, month);
//
//            return ResponseEntity.ok(result);
//        } catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//    }
//
    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
