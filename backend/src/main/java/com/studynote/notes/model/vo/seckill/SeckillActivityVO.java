package com.studynote.notes.model.vo.seckill;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SeckillActivityVO {

    private Integer activityId;
    private Integer courseId;
    private BigDecimal seckillPrice;
    private Integer stock;
    private Date startTime;
    private Date endTime;
    private Integer status;
    private Integer version;
    private Date createdAt;
    private Date updatedAt;
}
