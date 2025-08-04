package org.memmcol.portalonboardservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.portalonboardservice.model.node.Node;
import org.memmcol.portalonboardservice.model.user.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface OrganizationMapper {

    @Insert(""" 
            INSERT INTO organizations(
                        business_name, postal_code, address, country, state, city, created_at, updated_at)
                        VALUES(#{businessName},#{postalCode},#{address},#{country},#{state},#{city},#{createdAt},#{updatedAt}
                   ) """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertOrganization(Organization organization);

    @Update("<script>" +
            "UPDATE organizations O " +
            "SET " +
            "  <if test='organization.businessName != null'>business_name = #{organization.businessName},</if>" +
            "  <if test='organization.postalCode != null'>postal_code = #{organization.postalCode},</if>" +
            "  <if test='organization.address != null'>address = #{organization.address},</if>" +
            "  <if test='organization.country != null'>country = #{organization.country},</if>" +
            "  <if test='organization.state != null'>state = #{organization.state},</if>" +
            "  <if test='organization.city != null'>city = #{organization.city},</if>" +
            "  updated_at = NOW() " +
            "WHERE O.id = #{organization.id} " +
            "</script>")
    void updateOrganizationSelective(@Param("organization") Organization organization);

    @Update("<script>" +
            "UPDATE users " +
            "<trim prefix='SET' suffixOverrides=','>" +
            "  <if test='email != null'>email = #{email},</if>" +
            "  <if test='phone != null'>phone = #{phone},</if>" +
            "  <if test='firstname != null'>firstname = #{firstname},</if>" +
            "  <if test='lastname != null'>lastname = #{lastname},</if>" +
            "</trim>" +
            "WHERE org_id = #{orgId}" +
            "</script>")
    void updateUserByOrgId(@Param("orgId") UUID orgId,
                           @Param("email") String email,
                           @Param("phone") String phone,
                           @Param("firstname") String firstname,
                           @Param("lastname") String lastname);


    @Insert("""
            Insert Into users(
            	org_id, firstname, lastname, email, node_id, status, active, password, last_active, created_at, updated_at, phone_number)
            	VALUES(#{orgId}, #{firstname}, #{lastname}, #{email}, #{nodeId}, #{status}, #{active}, #{password}, #{lastActive}, #{createdAt}, #{updatedAt}, #{phoneNumber})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertUser(UserModel userModel);

    @Select("SELECT count(*) From Customers")
    int countCustomers();

    @Insert("""
            Insert Into nodes(name, org_id)
            VALUES(#{name}, #{orgId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertNodes(Node node);

    @Insert("""
            INSERT INTO Permissions(view, edit, approve, disable, org_id)
            VALUES(#{view},#{edit},#{approve},#{disable},#{orgId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertPermission(Permission permission);

    @Insert("""
            INSERT INTO Groups(title, org_id,created_at,updated_at)
            VALUES(#{groupTitle},#{orgId},#{createdAt},#{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertGroup(Group group);

    @Insert("""
            INSERT INTO group_permissions(group_id, permission_id, org_id)
            VALUES(#{groupId},#{permissionId},#{orgId})
            """)
    void insertGroupPermission(UUID groupId, UUID permissionId, UUID orgId);

    @Select("SELECT * FROM users WHERE email = #{email}")
    UserModel getUserByEmail(@Param("email") String email);

    @Select("SELECT * FROM users WHERE org_id = #{id}")
    UserModel getUserByOrgId(@Param("id") UUID id);

    @Select("SELECT * FROM organizations ORDER BY created_at DESC ")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "postalCode", column = "postal_code")
    })
    List<Organization> getAllOrganizations();


    @Select("SELECT * FROM organizations ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "postalCode", column = "postal_code"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<Organization> getOrganizations(@Param("size") int size, @Param("offset") int offset);

    @Select("SELECT * FROM organizations WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "postalCode", column = "postal_code"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<Organization> getOrganizationById(@Param("id") UUID id);

    @Select("SELECT * FROM permissions WHERE org_id = #{org_id}")
    @Results({
            @Result(property = "id", column = "id", id = true),
            @Result(property = "orgId", column = "org_id"),
    })
    Permission getPermissionByOrgId(@Param("org_id") UUID org_id);

    @Select("SELECT * FROM Groups Where org_id = #{org_id}")
    @Results({
            @Result(property = "id", column = "id", id = true),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "groupTitle", column = "title")
    })
    Group getGroupByOrgId(@Param("org_id") UUID org_id);

    @Select("SELECT * FROM nodes WHERE name = #{name} AND org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id", id = true),
            @Result(property = "name", column = "name"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "orgId", column = "org_id")
    })
    Node getNodeByNameAndOrgId(@Param("name") String name, @Param("orgId") UUID orgId);

    @Select("SELECT COUNT(*) FROM organizations")
    long getOrganizationCount();

}
