package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Update("UPDATE messages SET is_read = true WHERE conversation_id = #{conversationId} AND receiver_id = #{receiverId} AND is_read = false")
    void markAsRead(@Param("conversationId") Long conversationId, @Param("receiverId") Long receiverId);

    @Select("SELECT COUNT(*) FROM messages WHERE receiver_id = #{userId} AND is_read = false")
    long countUnreadByUserId(@Param("userId") Long userId);
}
