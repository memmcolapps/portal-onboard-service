package org.memmcol.portalonboardservice.controller;


import org.memmcol.portalonboardservice.model.user.Organization;
import org.memmcol.portalonboardservice.model.user.OnboardingOrganizationDTO;
import org.memmcol.portalonboardservice.model.user.UserModel;
import org.memmcol.portalonboardservice.service.organization.OnboardOrganizationService;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/gfPortal/service/organization")
public class OnboardOrganizationController {

    @Autowired
    private OnboardOrganizationService onboardOrganizationService;

    @Autowired
    private GlobalExceptionHandler exception;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createOrganization(@RequestBody OnboardingOrganizationDTO request) {

        Organization organization = new Organization();
        organization.setBusinessName(request.getBusinessName());
        organization.setPostalCode(request.getPostalCode());
        organization.setAddress(request.getAddress());
        organization.setCountry(request.getCountry());
        organization.setState(request.getState());
        organization.setCity(request.getCity());

        UserModel userModel = new UserModel();
        userModel.setFirstname(request.getFirstName());
        userModel.setLastname(request.getLastName());
        userModel.setEmail(request.getEmail());
        userModel.setPassword(request.getPassword());
        userModel.setPhoneNumber(request.getPhoneNumber());

        try {
            Map<String, Object> result = onboardOrganizationService.addOrganization(organization, userModel);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/update")
    public ResponseEntity<Map<String, Object>> updateOrganization(@RequestBody OnboardingOrganizationDTO request) {
        Organization organization = new Organization();
        organization.setId(request.getId());
        organization.setBusinessName(request.getBusinessName());
        organization.setPostalCode(request.getPostalCode());
        organization.setAddress(request.getAddress());
        organization.setCountry(request.getCountry());
        organization.setState(request.getState());
        organization.setCity(request.getCity());

        UserModel userModel = new UserModel();
        userModel.setId(request.getUserId());
        userModel.setFirstname(request.getFirstName());
        userModel.setLastname(request.getLastName());
        userModel.setEmail(request.getEmail());
        userModel.setPhoneNumber(request.getPhoneNumber());

        try {

            Map<String, Object> result = onboardOrganizationService.updateOrganization(organization,userModel);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getOrganization() {
        try {
            Map<String, Object> result = onboardOrganizationService.getOrganization();
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single")
    public ResponseEntity<Map<String, Object>> getOrganizationById(@RequestParam UUID id) {
        try {
            Map<String, Object> result = onboardOrganizationService.getOrganizationById(id);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/suspend")
    public ResponseEntity<Map<String, Object>> suspendOrganization(@RequestParam UUID id, @RequestParam Boolean status) {
        try {
            Map<String, Object> result = onboardOrganizationService.suspendOrganization(id, status);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
