package org.memmcol.portalonboardservice.controller;

import org.memmcol.portalonboardservice.model.user.ContactMessage;
import org.memmcol.portalonboardservice.model.user.ContactMessageSearchCriteria;
import org.memmcol.portalonboardservice.service.contact_message.ContactService;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/gfPortal/service/message")
public class ContactMessageController {

    @Autowired
    private ContactService contactService;

    @Autowired
    private GlobalExceptionHandler exception;

    @PostMapping("/enquiries")
    public ResponseEntity<Map<String, Object>> insertContactMessage(
            @RequestBody ContactMessage contactMessage) {
        try {

            Map<String, Object> result = contactService.addMessage(contactMessage);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PostMapping("/read")
    public ResponseEntity<Map<String, Object>> insertReadMessage(@RequestParam UUID id) {
        try {
            Map<String, Object> result = contactService.addReadMessage(id);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchMessages(
            @RequestParam(required = false) String organizationName,
            @RequestParam(required = false) String organizationSize,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            ContactMessageSearchCriteria criteria = new ContactMessageSearchCriteria();
            criteria.setOrganizationName(organizationName);
            criteria.setOrganizationSize(organizationSize);
            criteria.setEmail(email);
            criteria.setStatus(status);
            criteria.setStartDate(startDate);
            criteria.setEndDate(endDate);
            criteria.setSearchTerm(searchTerm);

            Map<String, Object> result = contactService.searchMessages(criteria, page, size);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
