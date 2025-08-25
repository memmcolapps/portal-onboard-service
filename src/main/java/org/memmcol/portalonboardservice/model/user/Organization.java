package org.memmcol.portalonboardservice.model.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Lob;
import lombok.Data;
import org.memmcol.portalonboardservice.model.node.Node;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;


@Data
public class Organization implements Serializable {
    static final long serialVersionUID = 1L;
    @Id
    private UUID id;

    private UUID userId;

    private String businessName;

//    private String businessType;

    private String postalCode;

    private String address;

    private String country;

    private String state;

    private String city;

    private Boolean status;

    private String image;

    private UserModel operator;

    private Node nodes;

    private Long totalCustomer;

    private Long totalFeeder;

    private BigDecimal totalVending;

    private BigDecimal totalBilling;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    public Organization() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserModel getOperator() {
        return operator;
    }

    public void setOperator(UserModel operator) {
        this.operator = operator;
    }

    public BigDecimal getTotalVending() {
        return totalVending;
    }

    public void setTotalVending(BigDecimal totalVending) {
        this.totalVending = totalVending;
    }

    public BigDecimal getTotalBilling() {
        return totalBilling;
    }

    public void setTotalBilling(BigDecimal totalBilling) {
        this.totalBilling = totalBilling;
    }

    public Long getTotalCustomer() {
        return totalCustomer;
    }

    public void setTotalCustomer(Long totalCustomer) {
        this.totalCustomer = totalCustomer;
    }

    public Long getTotalFeeder() {
        return totalFeeder;
    }

    public void setTotalFeeder(Long totalFeeder) {
        this.totalFeeder = totalFeeder;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Node getNodes() {
        return nodes;
    }

    public void setNodes(Node nodes) {
        this.nodes = nodes;
    }
}
