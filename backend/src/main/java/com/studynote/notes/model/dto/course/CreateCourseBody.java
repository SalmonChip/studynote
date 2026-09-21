package com.studynote.notes.model.dto.course;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


@Data
public class CreateCourseBody {
    @NotBlank(message = "课程标题不能为空") String title;
    String description;                          // 可选
    String coverUrl;                             // 可选
    @NotNull(message = "价格不能为空") @Min(value = 0, message = "价格不能为负") Long price;
    Integer status;                              // 可选，不传默认当 1 上架处理（自己定）

}
