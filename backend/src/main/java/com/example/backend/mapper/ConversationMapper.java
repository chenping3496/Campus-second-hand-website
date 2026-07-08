package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT SUM(CASE WHEN user1_id = #{userId} THEN user1_unread ELSE user2_unread END) " +
            "FROM conversations WHERE user1_id = #{userId} OR user2_id = #{userId}")
    Integer countUnreadByUserId(@Param("userId") Long userId);
}
