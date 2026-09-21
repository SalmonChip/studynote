package com.studynote.notes.model.entity;

import lombok.Data;

import java.util.Date;

/**
 * 秒杀活动实体类。
 */
@Data
public class SeckillActivity {

    /** 秒杀活动ID（主键，自增） */
    private Integer activityId;

    /** 关联课程ID */
    private Integer courseId;

    /** 秒杀价（分） */
    private Long seckillPrice;

    /** 秒杀库存 */
    private Integer stock;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;

    /** 状态：0 下架，1 上架 */
    private Integer status;

    /** 乐观锁版本号（阶段②c 防超卖用） */
    private Integer version;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;
}
