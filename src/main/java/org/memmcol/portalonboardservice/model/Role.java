package org.memmcol.portalonboardservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Role implements Serializable{

	private static final long serialVersionUID = 1L;


	private UUID id;
	private UUID orgId;
	private UUID userId;
    private String userRole;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getOrgId() {
		return orgId;
	}

	public void setOrgId(UUID orgId) {
		this.orgId = orgId;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getUserRole() {
		return userRole;
	}

	public void setUserRole(String userRole) {
		this.userRole = userRole;
	}
}
