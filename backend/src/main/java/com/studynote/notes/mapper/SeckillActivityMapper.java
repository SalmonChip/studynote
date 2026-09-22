package com.studynote.notes.mapper;

import com.studynote.notes.model.entity.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface SeckillActivityMapper {

    int insert(SeckillActivity activity);

    List<SeckillActivity> activityList();

    SeckillActivity findById(Integer activityId);

    int update(SeckillActivity activity);

    int deleteById(Integer activityId);

    int deductStock(Integer activityId);

    int addStock(Integer activityId);

    /**
     * 【对账】查询这一轮需要参与对账的活动ID。
     * <p>
     * 筛选条件是"当前【不在】秒杀时间窗口内"的活动：
     * <pre>
     *     end_time &lt; now      已经结束的
     * 或  start_time &gt; now    还没开始的
     * </pre>
     * 为什么必须排除"正在进行中"的活动？这不是性能优化，是正确性要求 ——
     * 两个理由都写在 {@code SeckillService#reconcileOne} 的注释里，去看一遍再写 SQL。
     * <p>
     * {@code from} 是时间下界：只有"最近结束"的活动才可能残留没被修掉的漂移，
     * 不加这个下界的话，三年前结束的活动每次都会被翻出来空刷一遍。
     *
     * @param now   当前时间
     * @param from  只关心 end_time 在这个时间之后的活动（通常是 now 往前推若干小时）
     * @param limit 单次最多捞几个活动
     * @return 活动ID列表（可能为空，调用方要能处理空）
     */
    List<Integer> findReconcileCandidates(@Param("now") Date now,
                                          @Param("from") Date from,
                                          @Param("limit") int limit);
}
