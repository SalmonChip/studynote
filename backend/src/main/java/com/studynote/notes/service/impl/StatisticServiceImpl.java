package com.studynote.notes.service.impl;

import com.studynote.notes.mapper.StatisticMapper;
import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.Pagination;
import com.studynote.notes.model.dto.statistic.StatisticQueryParam;
import com.studynote.notes.model.entity.Statistic;
import com.studynote.notes.service.StatisticService;
import com.studynote.notes.utils.ApiResponseUtil;
import com.studynote.notes.utils.PaginationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatisticServiceImpl implements StatisticService {
    @Autowired
    private StatisticMapper statisticMapper;

    @Override
    public ApiResponse<List<Statistic>> getStatistic(StatisticQueryParam queryParam) {

        Integer page = queryParam.getPage();
        Integer pageSize = queryParam.getPageSize();
        int offset = PaginationUtils.calculateOffset(page, pageSize);
        int total = statisticMapper.countStatistic();

        Pagination pagination = new Pagination(page, pageSize, total);

        try {
            List<Statistic> statistics = statisticMapper.findByPage(pageSize, offset);
            return ApiResponseUtil.success("获取统计列表成功", statistics, pagination);
        } catch (Exception e) {
            return ApiResponseUtil.error(e.getMessage());
        }
    }
}
