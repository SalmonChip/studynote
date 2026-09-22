package com.studynote.notes.model.vo.course;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;


@Data
public class CourseVO {
    /** 课程ID（主键，自增） */
    private Integer courseId;

    /** 课程标题 */
    private String title;

    /** 课程描述 */
    private String description;

    /** 课程封面图 URL */
    private String coverUrl;

    /** 课程价格（单位：分；绝不用 double/float） */
    private BigDecimal price;

    /** 状态：0 下架，1 上架 */
    private Integer status;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;
}
