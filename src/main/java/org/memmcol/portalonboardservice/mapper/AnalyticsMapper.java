package org.memmcol.portalonboardservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.portalonboardservice.model.audit.IncidentReport;
import org.memmcol.portalonboardservice.model.user.Organization;
import org.memmcol.portalonboardservice.model.user.UserModel;

import java.util.List;
import java.util.UUID;

@Mapper
public interface AnalyticsMapper {

    @Select("SELECT * FROM organizations")
    List<Organization> getAllOrganizations();

    @Select("SELECT COUNT(*) FROM customers")
    long getTotalCustomer();


    @Select("SELECT * FROM incident_report ORDER BY created_at DESC ")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "organization", column = "org_id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.AnalyticsMapper.getOrganization")),
            @Result(property = "user", column = "user_id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.AnalyticsMapper.getUser"))
    })
    List<IncidentReport> getIncidentReport();

    @Select("SELECT business_name FROM organizations WHERE id = #{org_id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "businessName", column = "business_name"),
    })
    Organization getOrganization(UUID org_id);

    @Select("SELECT firstname, lastname FROM users WHERE id = #{org_id}")
    UserModel getUser(UUID org_id);

    @Select("UPDATE incident_report SET status = #{status} WHERE id = #{id}")
    IncidentReport getIncidentReportResolve(Boolean status, UUID id);

    @Select("SELECT * FROM incident_report ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "organization", column = "org_id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.AnalyticsMapper.getOrganization")),
            @Result(property = "user", column = "user_id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.AnalyticsMapper.getUser"))
    })
    List<IncidentReport> incidentReportResolveAnalytics();

    @Select("SELECT COUNT(*) FROM incident_report")
    long getIncidentReportCount();

    @Select("""
    SELECT * 
    FROM incident_report 
    WHERE org_id = #{orgId}  AND status = #{status}
    ORDER BY created_at DESC
    LIMIT #{size} OFFSET #{offset}
    """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "organization", column = "org_id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.AnalyticsMapper.getOrganization")),
            @Result(property = "user", column = "user_id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.AnalyticsMapper.getUser"))
    })
    List<IncidentReport> getIncidentReportByCompanyPaged(
            @Param("orgId") UUID orgId,
            @Param("status") Boolean status,
            @Param("offset") int offset,
            @Param("size") int size
    );


    @Select("SELECT COUNT(*) FROM incident_report WHERE org_id = #{orgId} AND status= #{status}")
    int countIncidentReportsByCompany(UUID orgId, Boolean status);

    @Select("""
    SELECT * 
    FROM incident_report
    WHERE status = false
    ORDER BY created_at DESC
    LIMIT #{size} OFFSET #{offset}
    """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "organization", column = "org_id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.AnalyticsMapper.getOrganization")),
            @Result(property = "user", column = "user_id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.AnalyticsMapper.getUser"))
    })
    List<IncidentReport> getUnresolvedIncidentReports(
            @Param("offset") int offset,
            @Param("size") int size
    );

    @Select("SELECT COUNT(*) FROM incident_report WHERE status = false")
    int getUnresolvedIncidentCount();
}