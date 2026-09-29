package com.studynote.notes.model.vo.course;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 「我的课程」列表项，由 SQL JOIN 直接填出。
 */
@Data
public class UserCourseVO {

    /** 课程ID */
    private Integer courseId;

    /** 课程标题 */
    private String title;

    /** 课程描述 */
    private String description;

    /** 课程封面图 URL */
    private String coverUrl;

    /** 课程价格（元） */
    private BigDecimal price;

    /** 来源 */
    private Integer source;

    /** 拥有时间 */
    private Date createTime;
}
