package com.studynote.notes.model.vo.note;

import lombok.Data;

/**
 * 笔记到所属题目的摘要。
 */
@Data
public class NoteQuestionItem {
    /** 笔记ID */
    private Integer noteId;

    /** 题目ID */
    private Integer questionId;

    /** 题目标题 */
    private String title;
}
