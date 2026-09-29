package com.studynote.notes.model.vo.seckill;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户端「秒杀活动」列表项，活动信息 + 绑定的课程信息。
 */
@Data
public class SeckillActivityDetailVO {

    /** 活动ID */
    private Integer activityId;

    /** 课程ID */
    private Integer courseId;

    /** 课程标题 */
    private String courseTitle;

    /** 课程封面图 URL */
    private String courseCoverUrl;

    /** 课程描述 */
    private String courseDescription;

    /** 课程原价（元） */
    private BigDecimal coursePrice;

    /** 秒杀价（元） */
    private BigDecimal seckillPrice;

    /** 总库存（MySQL），不是实时剩余量 */
    private Integer stock;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;
}
