package com.studynote.notes.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Note {
    /**
     * 笔记ID
     */
    private Integer noteId;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 问题ID
     */
    private Integer questionId;

    /**
     * 笔记内容
     */
    private String content;

    /**
     * 全文搜索分词向量（笔记正文的分词结果）
     */
    private String searchVector;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 收藏数
     */
    private Integer collectCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
