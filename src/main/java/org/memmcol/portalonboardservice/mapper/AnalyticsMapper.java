package org.memmcol.portalonboardservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.portalonboardservice.model.user.Organization;

import java.util.List;

@Mapper
public interface AnalyticsMapper {

    @Select("SELECT * FROM organizations")
    List<Organization> getAllOrganizations();
}

//    @Select("SELECT * FROM organizations ORDER BY created_at DESC ")
//    @Results({
//            @Result(property = "id", column = "id"),
//            @Result(property = "userId", column = "user_id"),
//            @Result(property = "businessName", column = "business_name"),
//            @Result(property = "postalCode", column = "postal_code"),
//            @Result(property = "createdAt", column = "created_at"),
//            @Result(property = "updatedAt", column = "updated_at"),
//    })