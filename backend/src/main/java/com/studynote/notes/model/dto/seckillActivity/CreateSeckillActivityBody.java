package com.studynote.notes.model.dto.seckillActivity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class CreateSeckillActivityBody {

    @NotNull(message = "课程ID不能为空")
    private Integer courseId;

    @NotNull(message = "秒杀价不能为空")
    @Min(value = 0, message = "秒杀价不能为负")
    private BigDecimal seckillPrice;

    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存必须大于0")
    private Integer stock;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /** 可选：0下架 1上架，不传默认 1 */
    private Integer status;
}
