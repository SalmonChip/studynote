package com.studynote.notes.model.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Category {
    /*
     * 分类ID（主键）
     */
    private Integer categoryId;

    /*
     * 分类名称
     */
    private String name;

    /*
     * 上级分类ID
     * 为0时表示当前分类是一级分类
     */
    private Integer parentCategoryId;

    /*
     * 创建时间
     */
    private Date createdAt;

    /*
     * 更新时间
     */
    private Date updatedAt;
}
