package org.memmcol.portalonboardservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.portalonboardservice.model.audit.IncidentReport;
import org.memmcol.portalonboardservice.model.user.Organization;
import org.memmcol.portalonboardservice.model.user.UserModel;

import java.util.List;

@Mapper
public interface AnalyticsMapper {

    @Select("SELECT * FROM organizations")
    List<Organization> getAllOrganizations();

    @Select("SELECT COUNT(*) FROM customers")
    long getTotalCustomer();


    @Select("SELECT * FROM incident_report")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "organization", column = "org_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.AnalyticsMapper.getOrganization")),
            @Result(property = "user", column = "user_id",
                    one = @One(select = "org.memmcol.gridflexbackendservice.mapper.AnalyticsMapper.getUser"))
    })
    IncidentReport getIncidentReport(String type);

    @Select("SELECT business_name FROM organizations WHERE id = #{org_id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "businessName", column = "business_name"),
    })
    Organization getOrganization(String org_id);

    @Select("SELECT first_name, last_name FROM users WHERE id = #{org_id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "firstName", column = "first_name"),
            @Result(property = "lastName", column = "last_name"),
    })
    UserModel getUser(String org_id);

    @Select("UPDATE incident_report SET status = #{status}")
    IncidentReport getIncidentReportResolve(Boolean state);
}