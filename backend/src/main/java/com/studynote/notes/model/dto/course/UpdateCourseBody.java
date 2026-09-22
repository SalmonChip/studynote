package com.studynote.notes.model.dto.course;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UpdateCourseBody {
    String title;
    String description;
    String coverUrl;
    @Min(value = 0, message = "价格不能为负") BigDecimal price;   // 只加 @Min，不加 @NotNull（可选）
    Integer status;

}
