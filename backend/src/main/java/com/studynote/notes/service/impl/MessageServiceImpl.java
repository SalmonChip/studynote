package com.studynote.notes.service.impl;

import com.studynote.notes.mapper.MessageMapper;
import com.studynote.notes.mapper.NoteMapper;
import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.dto.message.MessageDTO;
import com.studynote.notes.model.entity.Message;
import com.studynote.notes.model.entity.User;
import com.studynote.notes.model.enums.message.MessageType;
import com.studynote.notes.model.vo.message.MessageVO;
import com.studynote.notes.model.vo.note.NoteQuestionItem;
import com.studynote.notes.scope.RequestScopeData;
import com.studynote.notes.service.MessageService;
import com.studynote.notes.service.UserService;
import com.studynote.notes.utils.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studynote.notes.model.enums.redisKey.RedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 消息服务实现类
 */
@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private RequestScopeData requestScopeData;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public Integer createMessage(MessageDTO messageDTO) {
        try {
            Message message = new Message();
            BeanUtils.copyProperties(messageDTO, message);

            if (messageDTO.getContent() == null) {
                message.setContent("");
            }

            return messageMapper.insert(message);
        } catch (Exception e) {
            throw new RuntimeException("创建消息通知失败: " + e.getMessage());
        }
    }

    @Override
    public void asyncCreateMessage(MessageDTO messageDTO) {
        try {
            String messageJson = objectMapper.writeValueAsString(messageDTO);
            redisTemplate.opsForList().leftPush(RedisKey.messageTaskQueue(), messageJson);
        } catch (Exception e) {
            log.error("消息通知入队失败", e);
        }
    }

    @Override
    public ApiResponse<List<MessageVO>> getMessages() {

        Long currentUserId = requestScopeData.getUserId();

        // 获取用户所有的消息对象
        List<Message> messages = messageMapper.selectByUserId(currentUserId);

        // 一条消息都没有就不用往下查了，省掉后面两次必然为空的批量查询
        if (messages.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        // 批量取发送者。distinct 是因为同一个人可能给我发了好几条，先把 map 的 key 压下来。
        List<Long> senderIds = messages.stream()
                .map(Message::getSenderId)
                .distinct()
                .toList();
        Map<Long, User> userMap = userService.getUserMapByIds(senderIds);

        // 批量取「笔记 ID → 题目摘要」。
        // target_id 存的是笔记 ID（三个发送方传的都是 MessageTargetType.NOTE + noteId），
        // 所以要 note → question 两跳。系统通知没有目标，先剔掉。
        List<Integer> noteIds = messages.stream()
                .filter(message -> !Objects.equals(message.getType(), MessageType.SYSTEM))
                .map(Message::getTargetId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Integer, MessageVO.QuestionSummary> questionByNoteId = noteIds.isEmpty()
                ? Collections.emptyMap()
                : noteMapper.findQuestionItemsByNoteIds(noteIds).stream()
                        .collect(Collectors.toMap(
                                NoteQuestionItem::getNoteId,
                                item -> {
                                    MessageVO.QuestionSummary question = new MessageVO.QuestionSummary();
                                    question.setQuestionId(item.getQuestionId());
                                    question.setTitle(item.getTitle());
                                    return question;
                                }));

        // 将 message 转成 messageVO
        List<MessageVO> messageVOS = messages.stream().map(message -> {
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);

            // 设置发送者信息
            messageVO.setSender(buildSender(message.getSenderId(), userMap));

            // 设置 target 信息
            if (!Objects.equals(message.getType(), MessageType.SYSTEM)) {
                MessageVO.Target target = new MessageVO.Target();
                target.setTargetId(message.getTargetId());
                target.setTargetType(message.getTargetType());
                // 笔记或题目已被删除时这里取到 null，前端据此隐藏「跳转题目」按钮
                target.setQuestion(questionByNoteId.get(message.getTargetId()));
                messageVO.setTarget(target);
            }

            return messageVO;
        }).toList();

        return ApiResponse.success(messageVOS);
    }

    /**
     * 组装发送者信息。
     * <p>
     * 项目禁用了外键，用户注销/被删后 message 里会留下指向不存在用户的孤儿 sender_id，
     * {@link UserService#getUserMapByIds} 返回的 map 里没有它，这里要兜底，
     * 否则一条孤儿消息就会让整个消息列表 500。
     */
    private MessageVO.Sender buildSender(Long senderId, Map<Long, User> userMap) {
        MessageVO.Sender sender = new MessageVO.Sender();
        sender.setUserId(senderId);

        User user = userMap.get(senderId);

        if (user == null) {
            // 前端直接渲染 sender.username，留 null 会显示成一片空白，给个明确的占位
            sender.setUsername("已注销用户");
            return sender;
        }

        sender.setUsername(user.getUsername());
        sender.setAvatarUrl(user.getAvatarUrl());

        return sender;
    }

    @Override
    public ApiResponse<EmptyVO> markAsRead(Integer messageId) {
        Long currentUserId = requestScopeData.getUserId();
        messageMapper.markAsRead(messageId, currentUserId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<EmptyVO> markAsReadBatch(List<Integer> messageIds) {
        Long currentUserId = requestScopeData.getUserId();
        messageMapper.markAsReadBatch(messageIds, currentUserId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<EmptyVO> markAllAsRead() {
        Long currentUserId = requestScopeData.getUserId();
        messageMapper.markAllAsRead(currentUserId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<EmptyVO> deleteMessage(Integer messageId) {
        Long currentUserId = requestScopeData.getUserId();
        messageMapper.deleteMessage(messageId, currentUserId);
        return ApiResponse.success();
    }

    @Override
    public ApiResponse<Integer> getUnreadCount() {
        Long currentUserId = requestScopeData.getUserId();
        Integer count = messageMapper.countUnread(currentUserId);
        return ApiResponse.success(count);
    }
}
