package org.memmcol.portalonboardservice.model.audit;

import org.memmcol.portalonboardservice.model.user.Organization;
import org.memmcol.portalonboardservice.model.user.UserModel;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.UUID;

public class IncidentReport implements Serializable {

    @Id
    private UUID id;
//    private UUID orgId;
//    private UUID userId;
    private String message;
    private String createdAt;
    private Organization organization;
    private Boolean status;
    private UserModel user;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

//    public UUID getOrgId() {
//        return orgId;
//    }
//
//    public void setOrgId(UUID orgId) {
//        this.orgId = orgId;
//    }
//
//    public UUID getUserId() {
//        return userId;
//    }

//    public void setUserId(UUID userId) {
//        this.userId = userId;
//    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}
