package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    public PageResult<ConversationDTO> getConversations(Long userId, int page, int size) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return PageResult.of(List.of(), 0, page, size);
        }

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Conversation::getUser1Id, userId).or().eq(Conversation::getUser2Id, userId))
                .orderByDesc(Conversation::getLastMessageTime);
        Page<Conversation> mpPage = conversationMapper.selectPage(new Page<>(page + 1, size), wrapper);

        // Batch load related users and products
        Set<Long> otherUserIds = new HashSet<>();
        Set<Long> productIds = new HashSet<>();
        mpPage.getRecords().forEach(c -> {
            otherUserIds.add(c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id());
            if (c.getProductId() != null) productIds.add(c.getProductId());
        });
        Map<Long, User> userMap = userMapper.selectBatchIds(otherUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<ConversationDTO> list = mpPage.getRecords().stream().map(c -> {
            Long otherUserId = c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id();
            User otherUser = userMap.get(otherUserId);
            Product product = c.getProductId() != null ? productMap.get(c.getProductId()) : null;
            return ConversationDTO.fromEntity(c, user, otherUser, product);
        }).toList();

        return PageResult.of(list, mpPage.getTotal(), page, size);
    }

    @Transactional
    public Result<ConversationDTO> getOrCreateConversation(Long userId, Long otherUserId, Long productId) {
        User user = userMapper.selectById(userId);
        User otherUser = userMapper.selectById(otherUserId);

        if (user == null || otherUser == null) {
            return Result.error("用户不存在");
        }

        if (userId.equals(otherUserId)) {
            return Result.error("不能与自己聊天");
        }

        Product product = null;
        if (productId != null) {
            product = productMapper.selectById(productId);
        }

        // Find existing conversation (checking both user orderings)
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        if (product != null) {
            wrapper.and(w -> w
                    .and(w1 -> w1.eq(Conversation::getUser1Id, userId)
                            .eq(Conversation::getUser2Id, otherUserId)
                            .eq(Conversation::getProductId, productId))
                    .or(w2 -> w2.eq(Conversation::getUser1Id, otherUserId)
                            .eq(Conversation::getUser2Id, userId)
                            .eq(Conversation::getProductId, productId)));
        } else {
            wrapper.and(w -> w
                    .and(w1 -> w1.eq(Conversation::getUser1Id, userId)
                            .eq(Conversation::getUser2Id, otherUserId))
                    .or(w2 -> w2.eq(Conversation::getUser1Id, otherUserId)
                            .eq(Conversation::getUser2Id, userId)));
        }
        Conversation conversation = conversationMapper.selectOne(wrapper);

        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUser1Id(userId);
            conversation.setUser2Id(otherUserId);
            conversation.setProductId(product != null ? product.getId() : null);
            conversationMapper.insert(conversation);
        }

        return Result.success(ConversationDTO.fromEntity(conversation, user, otherUser, product));
    }

    public Result<ConversationDTO> getConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            return Result.error("会话不存在");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (!conversation.getUser1Id().equals(userId) &&
                !conversation.getUser2Id().equals(userId)) {
            return Result.error("无权访问此会话");
        }

        Long otherUserId = conversation.getUser1Id().equals(userId)
                ? conversation.getUser2Id() : conversation.getUser1Id();
        User otherUser = userMapper.selectById(otherUserId);
        Product product = conversation.getProductId() != null
                ? productMapper.selectById(conversation.getProductId()) : null;

        return Result.success(ConversationDTO.fromEntity(conversation, user, otherUser, product));
    }

    public PageResult<MessageDTO> getMessages(Long conversationId, Long userId, int page, int size) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            return PageResult.of(List.of(), 0, page, size);
        }

        if (!conversation.getUser1Id().equals(userId) &&
                !conversation.getUser2Id().equals(userId)) {
            return PageResult.of(List.of(), 0, page, size);
        }

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, conversationId).orderByDesc(Message::getCreatedAt);
        Page<Message> mpPage = messageMapper.selectPage(new Page<>(page + 1, size), wrapper);

        // Batch load senders and receivers
        Set<Long> userIds = new HashSet<>();
        mpPage.getRecords().forEach(m -> {
            userIds.add(m.getSenderId());
            userIds.add(m.getReceiverId());
        });
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<MessageDTO> list = mpPage.getRecords().stream().map(m -> {
            User sender = userMap.get(m.getSenderId());
            User receiver = userMap.get(m.getReceiverId());
            return MessageDTO.fromEntity(m, sender, receiver);
        }).toList();

        return PageResult.of(list, mpPage.getTotal(), page, size);
    }

    @Transactional
    public Result<MessageDTO> sendMessage(Long conversationId, Long senderId, String content, String type) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            return Result.error("会话不存在");
        }

        User sender = userMapper.selectById(senderId);
        if (sender == null) {
            return Result.error("用户不存在");
        }

        if (!conversation.getUser1Id().equals(senderId) &&
                !conversation.getUser2Id().equals(senderId)) {
            return Result.error("无权发送消息");
        }

        Long receiverId = conversation.getUser1Id().equals(senderId)
                ? conversation.getUser2Id()
                : conversation.getUser1Id();

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setType(type != null ? Message.MessageType.valueOf(type) : Message.MessageType.TEXT);

        messageMapper.insert(message);

        conversation.setLastMessage(content.length() > 100 ? content.substring(0, 100) : content);
        conversation.setLastMessageTime(LocalDateTime.now());
        if (conversation.getUser1Id().equals(receiverId)) {
            conversation.setUser1Unread(conversation.getUser1Unread() + 1);
        } else {
            conversation.setUser2Unread(conversation.getUser2Unread() + 1);
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        User receiver = userMapper.selectById(receiverId);
        return Result.success(MessageDTO.fromEntity(message, sender, receiver));
    }

    @Transactional
    public Result<Void> markMessagesAsRead(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            return Result.error("会话不存在");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        messageMapper.markAsRead(conversationId, userId);

        if (conversation.getUser1Id().equals(userId)) {
            conversation.setUser1Unread(0);
        } else {
            conversation.setUser2Unread(0);
        }
        conversationMapper.updateById(conversation);

        return Result.success();
    }

    public int getUnreadCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return 0;
        }
        Integer count = conversationMapper.countUnreadByUserId(userId);
        return count != null ? count : 0;
    }
}
