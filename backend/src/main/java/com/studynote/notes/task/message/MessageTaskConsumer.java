package com.studynote.notes.task.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studynote.notes.model.dto.message.MessageDTO;
import com.studynote.notes.model.enums.redisKey.RedisKey;
import com.studynote.notes.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 站内消息消费者：定时轮询 Redis 消息队列，将点赞/评论产生的通知异步落库
 * 与 EmailTaskConsumer 复用同一套「Redis List 模拟消息队列」模式
 */
@Slf4j
@Component
public class MessageTaskConsumer {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private MessageService messageService;

    /**
     * 每 1 秒轮询一次 Redis 消息队列，消费待落库的站内消息
     */
    @Scheduled(fixedDelay = 1000)
    public void consume() {
        String queueKey = RedisKey.messageTaskQueue();

        while (true) {
            String messageJson = redisTemplate.opsForList().rightPop(queueKey);
            if (messageJson == null) {
                break;
            }

            try {
                MessageDTO messageDTO = objectMapper.readValue(messageJson, MessageDTO.class);
                messageService.createMessage(messageDTO);
            } catch (Exception e) {
                log.error("消费消息通知失败", e);
            }
        }
    }
}
