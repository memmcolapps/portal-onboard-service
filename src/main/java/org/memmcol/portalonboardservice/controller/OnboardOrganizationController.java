package org.memmcol.portalonboardservice.controller;


import org.memmcol.portalonboardservice.model.user.Organization;
import org.memmcol.portalonboardservice.model.user.OnboardingOrganizationDTO;
import org.memmcol.portalonboardservice.model.user.UserModel;
import org.memmcol.portalonboardservice.service.organization.FileStorageService;
import org.memmcol.portalonboardservice.service.organization.OnboardOrganizationService;
import org.memmcol.portalonboardservice.util.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/gfPortal/service/organization")
public class OnboardOrganizationController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private OnboardOrganizationService onboardOrganizationService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GlobalExceptionHandler exception;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createOrganization(
            @RequestParam(value = "businessName", required = true) String businessName,
            @RequestParam(value = "postalCode", required = true) String postalCode,
            @RequestParam(value = "address", required = true) String address,
            @RequestParam(value = "country", required = true) String country,
            @RequestParam(value = "state", required = true) String state,
            @RequestParam(value = "city", required = true) String city,
            @RequestParam(value = "firstName", required = true) String firstName,
            @RequestParam(value = "lastName", required = true) String lastName,
            @RequestParam(value = "email", required = true) String email,
            @RequestParam(value = "password", required = true) String password,
            @RequestParam(value = "phoneNumber", required = true) String phoneNumber,
            @RequestParam(value = "logo", required = false) MultipartFile file) {

        Organization organization = new Organization();
        organization.setBusinessName(businessName);
        organization.setPostalCode(postalCode);
        organization.setAddress(address);
        organization.setCountry(country);
        organization.setState(state);
        organization.setCity(city);

        UserModel userModel = new UserModel();
        userModel.setFirstname(firstName);
        userModel.setLastname(lastName);
        userModel.setEmail(email);
        userModel.setPassword(password);
        userModel.setPhoneNumber(phoneNumber);

        try {
            if (file != null) {
                String fileUrl = fileStorageService.saveFile(file);
                organization.setImage(fileUrl);
            }

            Map<String, Object> result = onboardOrganizationService.addOrganization(organization, userModel);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PatchMapping("/update")
    public ResponseEntity<Map<String, Object>> updateOrganization(
            @RequestBody OnboardingOrganizationDTO request) {
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

            Map<String, Object> result = onboardOrganizationService.updateOrganization(organization, userModel);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getOrganization(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "0") int size
    ) {
        try {
            Map<String, Object> result = onboardOrganizationService.getOrganization(name, status,page,size);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

    @GetMapping("/single")
    public ResponseEntity<Map<String, Object>> getOrganizationById(@RequestParam UUID id) {
        try {
            Map<String, Object> result = onboardOrganizationService.getOrganizationById(id);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

@PatchMapping("/suspend")
    public ResponseEntity<Map<String, Object>> suspendOrganization(@RequestParam UUID id, @RequestParam Boolean status) {
        try {
            Map<String, Object> result = onboardOrganizationService.suspendOrganization(id, status);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        }
    }

@PostMapping("/module-activated")
    public ResponseEntity<Map<String, Object>> addOrgModuleActivated(@RequestBody Map<String, Object> request) {
        try {
            UUID orgId = UUID.fromString((String) request.get("orgId"));
            Map<String, Boolean> module = (Map<String, Boolean>) request.get("module");
            
            Map<String, Object> result = onboardOrganizationService.addOrgModuleActivated(orgId, module);
            return ResponseEntity.ok(result);
        } catch (GlobalExceptionHandler.SQLServerException e) {
            return handleException(e);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, Object>> handleException(GlobalExceptionHandler.SQLServerException e) {
        return (ResponseEntity<Map<String, Object>>) exception.handleSQLServerException(e);
    }
}

//
//    @PostMapping("/create")
//    public ResponseEntity<Map<String, Object>> createOrganization(
//            @RequestBody OnboardingOrganizationDTO request,
//            @RequestParam MultipartFile file) {
//
//        Organization organization = new Organization();
//        organization.setBusinessName(request.getBusinessName());
//        organization.setPostalCode(request.getPostalCode());
//        organization.setAddress(request.getAddress());
//        organization.setCountry(request.getCountry());
//        organization.setState(request.getState());
//        organization.setCity(request.getCity());
//
//        UserModel userModel = new UserModel();
//        userModel.setFirstname(request.getFirstName());
//        userModel.setLastname(request.getLastName());
//        userModel.setEmail(request.getEmail());
//        userModel.setPassword(request.getPassword());
//        userModel.setPhoneNumber(request.getPhoneNumber());
//
//        try {
//            Map<String, Object> result = onboardOrganizationService.addOrganization(organization, userModel);
//            return ResponseEntity.ok(result);
//        }catch (GlobalExceptionHandler.SQLServerException e) {
//            return handleException(e);
//        }
//    }


//    File dir = new File(uploadDir);
//            if (!dir.exists()) dir.mkdirs();
// Save file
////            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
////            Path filePath = Paths.get(uploadDir, fileName);
//            String fileName = StringUtils.getFilenameExtension(file.getOriginalFilename());
//            String uniqueName = UUID.randomUUID().toString() + "." + fileName;
//            Path filePath = Paths.get(uploadDir, uniqueName);
//            Files.createDirectories(filePath.getParent());
//            Files.write(filePath, file.getBytes());
//
//            // Save file path (absolute or relative) in DB
//            String fileUrl = "/uploads/"+ fileName;
//            organization.setImagePath(fileUrl);