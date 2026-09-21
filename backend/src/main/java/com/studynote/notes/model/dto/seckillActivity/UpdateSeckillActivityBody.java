package com.studynote.notes.model.dto.seckill;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Min;
import java.util.Date;

@Data
public class UpdateSeckillActivityBody {

    private Integer courseId;

    @Min(value = 0, message = "秒杀价不能为负")
    private Long seckillPrice;

    @Min(value = 1, message = "库存必须大于0")
    private Integer stock;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    private Integer status;
}
