package org.memmcol.portalonboardservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.portalonboardservice.model.user.Organization;

import java.util.List;

@Mapper
public interface AnalyticsMapper {

    @Select("SELECT * FROM organizations")
    List<Organization> getAllOrganizations();
}