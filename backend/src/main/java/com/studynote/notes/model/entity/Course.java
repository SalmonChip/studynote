package com.studynote.notes.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 课程实体类。
 * 字段与 course 表一一对应：数据库 snake_case 列名（cover_url）
 * 由 MyBatis 自动映射成这里的 camelCase 字段（coverUrl），不用写任何 @Column。
 */
@Data
public class Course {

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
