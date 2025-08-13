package org.memmcol.portalonboardservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.portalonboardservice.model.user.Operator;
import org.memmcol.portalonboardservice.service.portal_user.PortalUserService;
import org.memmcol.portalonboardservice.service.portal_user.PortalUserServiceImpl;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/gfPortal/auth/service")
public class PortalUserController {

    @Autowired private PortalUserService service;

    @Autowired
    private GlobalExceptionHandler exception;
    @Autowired
    private PortalUserServiceImpl portalUserServiceImpl;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        try {
            Map<String, Object> result = service.logout();
            return ResponseEntity.ok(result);

        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (Exception ex) {
            // Catch other errors (optional)
            Map<String, Object> errorResponse = ResponseMap.response(
                    HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    "An error occurred during logout",
                    ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody Operator operator) {
        try {
            Map<String, Object> result = service.createOperator(operator);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/update")
    public ResponseEntity<?> update(@RequestBody Operator operator) {
        try {
            Map<String, Object> result = service.updateOperator(operator);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/change-status")
    public ResponseEntity<?> block(@RequestParam UUID id,@RequestParam boolean status) {
        try {
            Map<String, Object> result = service.blockOperator(id, status);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getSingle(@RequestParam UUID id) {
        try {
            Map<String, Object> result = service.getSingle(id);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/generate-otp")
    public ResponseEntity<?> generateOtp(@RequestParam String username) {
        try {
            Map<String, Object> result = service.generateOtp(username);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }

    }

    @PostMapping("/forget-password")
    public ResponseEntity<?> verifyOtp(@RequestParam String username, @RequestParam String password, @RequestParam String retype_password, @RequestParam String otp) {
        try {
            Map<String, Object> result = service.verifyOtp(username, otp, password, retype_password);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }


    @GetMapping("/all")
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
