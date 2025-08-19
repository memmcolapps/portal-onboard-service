package org.memmcol.portalonboardservice.mapper;


import org.apache.ibatis.annotations.*;
import org.memmcol.portalonboardservice.model.user.Operator;
import org.memmcol.portalonboardservice.model.user.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Mapper
public interface PortalUserMapper {

    @Insert("""
        Insert Into portal_users(
        firstname, lastname, email, status, active, department, password, created_at, updated_at)
        VALUES(#{firstname},#{lastname},#{email},#{status},#{active},#{department},#{password},#{createdAt},#{updatedAt})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createPortalUser(Operator operator);

    @Insert("""
            Insert Into portal_roles(user_id, user_role)
            VALUES(#{userId},#{userRole})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    boolean createPortalRole(Role role);

    @Select("SELECT * FROM portal_users Where email = #{email}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "createdAt", column = "Created_at"),
            @Result(property = "updatedAt", column = "Updated_at")
    })
    Operator findByEmail(String email);

    @Select("SELECT * FROM portal_users WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "createdAt", column = "Created_at"),
            @Result(property = "updatedAt", column = "Updated_at"),
            @Result(property = "roles", column = "id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.PortalUserMapper.getRolesByOperatorId")),

    })
    Operator getSinglePortalUser(UUID id);

    @Select("SELECT * FROM portal_roles WHERE user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "userRole", column = "user_role"),
    })
    Optional<Role> getRolesByOperatorId(UUID userId);

    @Select("SELECT * FROM portal_users")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "createdAt", column = "Created_at"),
            @Result(property = "updatedAt", column = "Updated_at"),
            @Result(property = "roles", column = "id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.PortalUserMapper.getRolesByOperatorId")),

    })
    List<Operator> getAllPortalUser();


    @Select("SELECT o.*, r.* " +
            "FROM portal_users o " +
            "INNER JOIN portal_roles r ON o.id = r.user_id " +
            "WHERE o.Email = #{email}")
    @Results({
            @Result(property = "id", column = "id"),
//            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "roles", column = "id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.PortalUserMapper.getRolesByOperatorId")),
            @Result(property = "createdAt", column = "Created_at"),
            @Result(property = "updatedAt", column = "Updated_at"),
    })
    Operator findByAuthEmail(String email);

    @Update({
            "<script>",
            "UPDATE portal_users",
            "SET "+
            "  <if test='firstname != null'> firstname = #{firstname},</if>"+
            "  <if test='lastname != null'> lastname = #{lastname},</if>"+
            "  <if test='department != null'> department = #{department},</if>"+
            "  updated_at = #{updatedAt}"+
            " WHERE id = #{id}"+
            "</script>"
    })
    int updatePortalUser(Operator operator);

    @Update("UPDATE portal_roles SET user_role = #{role} " +
            "Where user_id = #{id} ")
    int updateRole(String role, UUID id);

    @Update("UPDATE portal_users SET status = #{stat} WHERE id = #{id}")
    int blockAndUnblockOperator(UUID id, boolean stat);

    @Update("UPDATE portal_users SET Active = true WHERE Email = #{email}")
    void updateLoginState(String email);

    @Update("UPDATE portal_users SET Active = false, updated_at = NOW() WHERE Email = #{email}")
    void updateLogoutState(String email);

    @Update("UPDATE portal_users SET password = #{password} WHERE Email = #{email}")
    int resetPassword(String email, String password);
}