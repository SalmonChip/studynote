package com.studynote.notes.model.vo.message;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息视图对象
 */
@Data
public class MessageVO {
    /**
     * 消息ID
     */
    private Integer messageId;

    /**
     * 发送者信息
     */
    private Sender sender;

    /**
     * 消息类型
     */
    private Integer type;

    /**
     * 目标ID
     */
    private Target target;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 简单用户信息
     */
    @Data
    public static class Sender {
        private Long userId;
        private String username;
        private String avatarUrl;
    }

    @Data
    public static class Target {
        private Integer targetId;
        private Integer targetType;
        /** 目标所属的题目，名字跟 NoteVO.question 保持一致；目标笔记或题目已删除时为 null，前端据此决定是否显示「跳转题目」 */
        private QuestionSummary question;
    }

    @Data
    public static class QuestionSummary {
        private Integer questionId;
        private String title;
    }
} 