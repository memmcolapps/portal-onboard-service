package org.memmcol.portalonboardservice.mapper;

import org.apache.ibatis.annotations.*;
import org.memmcol.portalonboardservice.model.user.ContactMessage;
import org.memmcol.portalonboardservice.model.user.ContactMessageSearchCriteria;
import org.memmcol.portalonboardservice.model.user.ReadMessages;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ContactMessageMapper {

    @Insert("""
    INSERT INTO contact_messages(organization_name, organization_size, email, phone_no, message, created_at)
    VALUES (#{organizationName},#{organizationSize},#{email},#{phoneNo},#{message},#{createdAt})       
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertContactMessage(ContactMessage contactMessage);

    @Insert("""
    INSERT INTO contact_message_reads(message_id, portal_user_id, status, read_at)
    VALUES (#{messageId},#{userId},#{status},#{readAt})       
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertReadMessage(ReadMessages readMessages);

    @Select("SELECT * FROM contact_message_reads WHERE portal_user_id = #{userId} And message_id = #{messageId} LIMIT 1")
    ReadMessages getReadMessagesByMessageIdAndUserId(@Param("userId") UUID userId,@Param("messageId")  UUID messageId);

    @Select("""
    <script>
    SELECT cm.*, 
           CASE 
               WHEN cmr.message_id IS NULL OR cmr.portal_user_id != #{userId} THEN 'New' 
               ELSE 'Read' 
           END as calculated_status
    FROM contact_messages cm
    LEFT JOIN contact_message_reads cmr ON cm.id = cmr.message_id AND cmr.portal_user_id = #{userId}
    WHERE 1=1
    <if test='criteria.organizationSize != null and criteria.organizationSize != ""'>
        AND cm.organization_size = #{criteria.organizationSize}
    </if>
    <if test='criteria.status != null and criteria.status != ""'>
        AND (
            <choose>
                <when test='criteria.status == "New"'>
                    (cmr.message_id IS NULL OR cmr.portal_user_id != #{userId})
                </when>
                <when test='criteria.status == "Read"'>
                    cmr.message_id IS NOT NULL AND cmr.portal_user_id = #{userId}
                </when>
            </choose>
        )
    </if>
    <if test='criteria.dateEntered != null'>
        AND cm.created_at &gt;= #{criteria.dateEntered}
    </if>
    <if test='search != null and search != ""'>
        AND (LOWER(cm.organization_name) LIKE CONCAT('%', LOWER(#{search}), '%')
             OR LOWER(cm.email) LIKE CONCAT('%', LOWER(#{search}), '%'))
    </if>
    ORDER BY cm.created_at DESC
    </script>
    """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "organizationName", column = "organization_name"),
            @Result(property = "organizationSize", column = "organization_size"),
            @Result(property = "email", column = "email"),
            @Result(property = "phoneNo", column = "phone_no"),
            @Result(property = "message", column = "message"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "status", column = "calculated_status"),
            @Result(property = "readMessages", column = "id",
                    one = @One(select = "getReadMessagesByMessageIdAndUser"))
    })
    List<ContactMessage> searchContactMessages(@Param("search") String search,@Param("criteria") ContactMessageSearchCriteria criteria,
                                               @Param("userId") UUID userId);

    @Select("SELECT * FROM contact_message_reads WHERE message_id = #{messageId} AND portal_user_id = #{userId}")
    ReadMessages getReadMessagesByMessageIdAndUser(@Param("messageId") UUID messageId,
                                                   @Param("userId") UUID userId);

}
