package org.memmcol.portalonboardservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.memmcol.portalonboardservice.service.portal_user.PortalUserService;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.memmcol.portalonboardservice.util.ResponseMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/gfPortal/operator/service")
public class PortalUserController {

    @Autowired private PortalUserService service;

    @Autowired
    private GlobalExceptionHandler exception;

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

//    @PostMapping("/logout")
//    public ResponseEntity<?> logout(HttpServletRequest request) {
//        String token = request.getHeader("Authorization");
//        try {
//            if (token != null && token.startsWith("Bearer ")) {
//                token = token.substring(7); // Remove "Bearer "
//
//                String finalToken = token;
//                Map<String, Object> result = service.logout(finalToken, 1800);
//                // Return the map wrapped in ResponseEntity
//                return ResponseEntity.ok(result);
//            }
//            Map<String, Object> errorResponse = ResponseMap.response(HttpStatus.UNAUTHORIZED.toString(), "Invalid Token", "");
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
//
//        } catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//    }

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
