package org.memmcol.portalonboardservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.portalonboardservice.model.node.Node;
import org.memmcol.portalonboardservice.model.node.NodeInfo;
import org.memmcol.portalonboardservice.model.user.*;
import org.memmcol.portalonboardservice.model.user.Module;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Mapper
public interface OrganizationMapper {

    @Insert(""" 
            INSERT INTO organizations(
                        business_name, postal_code, address, country, state, city, created_at, updated_at, image)
                        VALUES(#{businessName},#{postalCode},#{address},#{country},#{state},#{city},#{createdAt},#{updatedAt}, #{image}
                   ) """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertOrganization(Organization organization);

    @Update("<script>" +
            "UPDATE organizations o " +
            "SET " +
            "  <if test='organization.businessName != null'>business_name = #{organization.businessName},</if>" +
            "  <if test='organization.postalCode != null'>postal_code = #{organization.postalCode},</if>" +
            "  <if test='organization.address != null'>address = #{organization.address},</if>" +
            "  <if test='organization.country != null'>country = #{organization.country},</if>" +
            "  <if test='organization.state != null'>state = #{organization.state},</if>" +
            "  <if test='organization.city != null'>city = #{organization.city},</if>" +
            "  updated_at = #{organization.updatedAt} " +
            "WHERE o.id = #{organization.id} " +
            "</script>")
    int updateOrganizationSelective(@Param("organization") Organization organization);

//    @Update("<script>" +
//            "UPDATE users " +
//            "<trim prefix='SET' suffixOverrides=','>" +
//            "  <if test='userModel.email != null'>email = #{userModel.email},</if>" +
//            "  <if test='userModel.phoneNumber != null'>phone_number = #{userModel.phoneNumber},</if>" +
//            "  <if test='userModel.firstname != null'>firstname = #{userModel.firstname},</if>" +
//            "  <if test='userModel.lastname != null'>lastname = #{userModel.lastname},</if>" +
//            "  updated_at = #{userModel.updatedAt}" +
//            "</trim> " +
//            "WHERE id = #{userModel.id} AND org_id = #{orgId}" +
//            "</script>")
//    int updateUserByOrgId(@Param("userModel") UserModel userModel, @Param("orgId") UUID orgId);


    @Insert("""
            Insert Into users(
            	org_id, firstname, lastname, email, node_id, status, active, password, last_active, created_at, updated_at, phone_number)
            	VALUES(#{orgId}, #{firstname}, #{lastname}, #{email}, #{nodeId}, #{status}, #{active}, #{password}, #{lastActive}, #{createdAt}, #{updatedAt}, #{phoneNumber})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(UserModel userModel);

    @Update("UPDATE organizations SET user_id = #{id} WHERE id = #{organizationId}")
    void updateOrg(UUID id, UUID organizationId);

    @Insert("INSERT INTO user_groups (user_id, group_id, org_id) VALUES (#{id}, #{groupId}, #{orgId})")
    int insertUserGroup(UUID id, UUID orgId, UUID groupId);

    @Select("SELECT count(*) From Customers")
    int countCustomers();

    @Insert("""
            Insert Into nodes(name, org_id) VALUES (#{name}, #{orgId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertNodes(Node node);

    @Insert("""
            INSERT INTO Permissions(view, edit, approve, disable, org_id)
            VALUES(#{view},#{edit},#{approve},#{disable},#{orgId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPermission(Permission permission);

    @Insert("""
            INSERT INTO Groups(title, org_id, created_at, updated_at)
            VALUES(#{groupTitle},#{orgId},#{createdAt},#{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGroup(Group group);

    @Insert("INSERT INTO modules (name, access, org_id, group_id) VALUES (#{name}, #{access}, #{orgId}, #{groupId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertModule(Module module);

    @Insert("INSERT INTO submodules (name, access, org_id, module_id) VALUES (#{name}, #{access}, #{orgId}, #{moduleId})")
    int insertSubModule(SubModule subModule);

    @Insert("""
            INSERT INTO group_permissions(group_id, permission_id, org_id)
            VALUES(#{groupId},#{permissionId},#{orgId})
            """)
    int insertGroupPermission(UUID groupId, UUID permissionId, UUID orgId);

    @Select("SELECT * FROM users WHERE email = #{email}")
    UserModel getUserByEmail(@Param("email") String email);

    @Select("SELECT * FROM users WHERE org_id = #{id}")
    UserModel getUserByOrgId(@Param("id") UUID id);

    @Select("SELECT * FROM organizations ORDER BY created_at DESC ")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "postalCode", column = "postal_code"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "operator", column = "user_id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.OrganizationMapper.getOperator"))
    })
    List<Organization> getAllOrganizations();

    @Select("SELECT u.id, u.firstname, u.lastname, u.email, u.node_id, u.status, u.active, u.org_id, u.last_active, " +
            "u.created_at, u.updated_at, u.phone_number FROM users u WHERE id = #{id} ")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "lastActive", column = "last_active"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "groups", column = "id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.OrganizationMapper.findGroupsWithPermissionsByUserId")),
//            @Result(property = "nodes", column = "id",
//                    one = @One(select = "org.memmcol.portalonboardservice.mapper.OrganizationMapper.getNode")),
    })
    UserModel getOperator(UUID id);

    @Select("SELECT * FROM groups g INNER JOIN user_groups ug ON g.id = ug.group_id WHERE ug.user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "groupTitle", column = "title"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "modules", column = "group_id",
                    many = @Many(select = "org.memmcol.portalonboardservice.mapper.OrganizationMapper.findModulesWithSubModulesByGroupId")),
            @Result(property = "permissions", column = "id",
                    many = @Many(select = "org.memmcol.portalonboardservice.mapper.OrganizationMapper.findPermissionsBySubModuleId"))
    })
    GroupWithPermissionsDTO findGroupsWithPermissionsByUserId(UUID userId);


    @Select("SELECT * FROM permissions p INNER JOIN group_permissions gp ON p.id = gp.permission_id WHERE gp.group_id = #{groupId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "view", column = "view"),
            @Result(property = "edit", column = "edit"),
            @Result(property = "approve", column = "approve"),
            @Result(property = "disable", column = "disable")
    })
    Permission findPermissionsBySubModuleId(UUID groupId);

    @Select("SELECT * FROM modules WHERE group_id = #{groupId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "access", column = "access"),
            @Result(property = "groupId", column = "group_id"),
            @Result(property = "subModules", column = "id",
                    many = @Many(select = "org.memmcol.portalonboardservice.mapper.OrganizationMapper.findSubModulesWithPermissionsByModuleId"))
    })
    List<ModuleWithSubModules> findModulesWithSubModulesByGroupId(UUID groupId);

    @Select("SELECT * FROM submodules WHERE module_id = #{moduleId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "access", column = "access"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "moduleId", column = "module_id")
    })
    List<SubModuleWithPermissions> findSubModulesWithPermissionsByModuleId(UUID moduleId);


//    @Select("SELECT * FROM organizations ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}")
//    @Results({
//            @Result(property = "id", column = "id"),
//            @Result(property = "businessName", column = "business_name"),
//            @Result(property = "postalCode", column = "postal_code"),
//            @Result(property = "createdAt", column = "created_at"),
//            @Result(property = "updatedAt", column = "updated_at")
//    })
//    List<Organization> getOrganizations(@Param("size") int size, @Param("offset") int offset);

    @Select("SELECT * FROM organizations WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "businessName", column = "business_name"),
            @Result(property = "postalCode", column = "postal_code"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "operator", column = "user_id",
                    one = @One(select = "org.memmcol.portalonboardservice.mapper.OrganizationMapper.getOperator")),
//            @Result(property = "nodes", column = "id",
//                    one = @One(select = "org.memmcol.portalonboardservice.mapper.OrganizationMapper.getNode"))
    })
    Organization getOrganizationById(@Param("id") UUID id);

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

    @Select("SELECT * FROM nodes WHERE org_id = #{orgId} AND (id = #{nodeId} " +
            "OR parent_id = #{nodeId} OR parent_id IN (SELECT id FROM nodes WHERE parent_id = #{nodeId}))")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeInfo", column = "id",
                    many = @Many(select = "org.memmcol.portalonboardservice.mapper.OrganizationMapper.getHierarchyById"))
    })
    List<Node> getNodeWithChildren(UUID nodeId, UUID orgId);

    @Select("SELECT * FROM nodes WHERE org_id = #{orgId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "nodeInfo", column = "id",
                    many = @Many(select = "org.memmcol.portalonboardservice.mapper.NodeMapper.getHierarchyById"))
    })
    List<Node> getAllNode(UUID orgId);


    @Select("""
        SELECT
            id, region_id,
            node_id, name, 
            NULL AS serial_no, phone_number, email, contact_person, address, 
            NULL AS status, NULL AS voltage, NULL AS latitude, NULL AS longitude, NULL AS description,
            created_at, updated_at, type, NULL AS asset_id
        FROM region_bhub_service_centers
        WHERE node_id = #{nodeId}
        UNION
        SELECT
            id, NULL AS region_id, 
            node_id, name, serial_no, phone_number, email, contact_person,
            address, status, voltage, latitude, longitude, description, created_at, updated_at, type,  asset_id
        FROM substation_trans_feeder_lines
        WHERE node_id = #{nodeId}
        """)
    @Results({
            @Result(property = "nodeId", column = "node_id"),
            @Result(property = "phoneNo", column = "phone_number"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "orgId", column = "org_id"),
            @Result(property = "assetId", column = "asset_id"),
            @Result(property = "regionId", column = "region_id"),
            @Result(property = "serialNo", column = "serial_no"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    NodeInfo getHierarchyById(UUID nodeId);

    @Select("SELECT COUNT(*) FROM customers WHERE org_id = #{id} ")
    Long totalCustomer(UUID id);

    @Select("SELECT COUNT(*) FROM substation_trans_feeder_lines WHERE org_id = #{id} AND type = 'feeder line'")
    Long totalFeeder(UUID id);

    @Update("UPDATE organizations SET status = #{suspend} WHERE id = #{id}")
    void suspendOrganization(UUID id, Boolean suspend);

    @Select("SELECT SUM(initial_amount) FROM vw_vending_transactions_summary WHERE org_id = #{id} And status = 'Successful' ")
    BigDecimal totalVending(UUID id);

}
