package org.memmcol.portalonboardservice.controller;

import org.memmcol.portalonboardservice.service.portal_user.PortalUserService;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/operator/service")
public class PortalUserController {

    @Autowired private PortalUserService service;

    @Autowired
    private GlobalExceptionHandler exception;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAll() {
        try {
            Map<String, Object> result = service.getAll();
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
