package org.memmcol.portalonboardservice.model.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Id;
import lombok.*;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ContactMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private UUID id;
    private String organizationName;

    private String organizationSize;

    private String email;

    private String phoneNo;

    private String message;

    private ReadMessages readMessages;

    private transient String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public ContactMessage() {
        this.createdAt = new Date();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationSize() {
        return organizationSize;
    }

    public void setOrganizationSize(String organizationSize) {
        this.organizationSize = organizationSize;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        if (this.status == null) {
            if (readMessages != null) {
                this.status = "Read";
            } else {
                this.status = "New";
            }
        }
        return this.status;
    }

}
