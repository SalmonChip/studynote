package com.studynote.notes.model.entity;

import lombok.Data;

import java.util.Date;

/**
 * 已购课程实体类（user_course 表），(user_id, course_id) 唯一索引保证幂等。
 */
@Data
public class UserCourse {

    /** 主键 */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 课程ID */
    private Integer courseId;

    /** 来源 */
    private Integer source;

    /** 入库时间 */
    private Date createTime;
}
