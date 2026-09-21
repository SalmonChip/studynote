package com.studynote.notes.mapper;

import com.studynote.notes.model.entity.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SeckillActivityMapper {

    int insert(SeckillActivity activity);

    List<SeckillActivity> activityList();

    SeckillActivity findById(Integer activityId);

    int update(SeckillActivity activity);

    int deleteById(Integer activityId);

    int deduckStock(Integer activityId);

    int addStock(Integer activityId);
}
