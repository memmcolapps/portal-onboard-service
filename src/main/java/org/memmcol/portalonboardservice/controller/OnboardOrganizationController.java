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
    private final OnboardOrganizationService onboardOrganizationService;

    @Autowired
    private GlobalExceptionHandler exception;

    public OnboardOrganizationController(OnboardOrganizationService onboardOrganizationService) {
        this.onboardOrganizationService = onboardOrganizationService;
    }

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
            Map<String, Object> result = onboardOrganizationService.addOrganization(organization,
                    userModel);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/edit")
    public ResponseEntity<Map<String, Object>> updateOrganization(@RequestBody OnboardingOrganizationDTO request,
                                                                  @RequestParam UUID orgId) {
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
        userModel.setPhoneNumber(request.getPhoneNumber());

        try {

            Map<String, Object> result = onboardOrganizationService.updateOrganization(organization,userModel,orgId);
            return ResponseEntity.ok(result);
        }catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getOrganization(
            @RequestParam(value = "page", required = false,  defaultValue = "0") int page,
            @RequestParam(value = "size", required = false,  defaultValue = "0") int size) {
        try {
            Map<String, Object> result = onboardOrganizationService.getOrganization(page,size);
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

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}
