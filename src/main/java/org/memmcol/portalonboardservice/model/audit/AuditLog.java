package org.memmcol.portalonboardservice.model.audit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.memmcol.portalonboardservice.model.Operator;
import org.memmcol.portalonboardservice.model.node.RegionBhubServiceCenter;
import org.memmcol.portalonboardservice.model.node.SubStationTransformerFeederLine;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "portal-audit-logs")
public class AuditLog {

    @Id
    private String id;

    private Operator creator;

    private String description;

    private String reason;

    private String type;

    private String userAgent;

    private String ipAddress;

    private Operator operator;

    private SubStationTransformerFeederLine subStationTransformerFeederLine;

    private RegionBhubServiceCenter regionBhubServiceCenter;


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public AuditLog() {
        this.createdAt = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Operator getCreator() {
        return creator;
    }

    public void setCreator(Operator creator) {
        this.creator = creator;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public SubStationTransformerFeederLine getSubStationTransformerFeederLine() {
        return subStationTransformerFeederLine;
    }

    public void setSubStationTransformerFeederLine(SubStationTransformerFeederLine subStationTransformerFeederLine) {
        this.subStationTransformerFeederLine = subStationTransformerFeederLine;
    }

    public RegionBhubServiceCenter getRegionBhubServiceCenter() {
        return regionBhubServiceCenter;
    }

    public void setRegionBhubServiceCenter(RegionBhubServiceCenter regionBhubServiceCenter) {
        this.regionBhubServiceCenter = regionBhubServiceCenter;
    }
}
