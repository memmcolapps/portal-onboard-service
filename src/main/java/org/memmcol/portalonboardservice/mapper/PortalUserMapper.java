package org.memmcol.portalonboardservice.mapper;


import org.apache.ibatis.annotations.*;
import org.memmcol.portalonboardservice.model.Operator;
import org.memmcol.portalonboardservice.model.Role;

import java.util.List;
import java.util.UUID;
@Mapper
public interface PortalUserMapper {

    @Select("SELECT * FROM portal_roles WHERE user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "userRole", column = "user_role"),
    })
    List<Role> getRolesByOperatorEmail(UUID userId);

    @Select("SELECT o.*, r.* " +
            "FROM portal_users o " +
            "INNER JOIN portal_roles r ON o.id = r.user_id " +
            "WHERE o.Email = #{email}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "roles", column = "id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.PortalUserMapper.getRolesByOperatorEmail")),
            @Result(property = "createdAt", column = "Created_at"),
            @Result(property = "updatedAt", column = "Updated_at"),
    })
    Operator findByAuthEmail(String email);


    @Update("UPDATE portal_users SET status = #{status} WHERE Email = #{operator}")
    int blockOperator(String operator, Boolean status);

    @Update("UPDATE portal_users SET Active = true WHERE Email = #{email}")
    void updateLoginState(String email);

    @Update("UPDATE portal_users SET Active = false WHERE Email = #{email}")
    void updateLogoutState(String email);
}