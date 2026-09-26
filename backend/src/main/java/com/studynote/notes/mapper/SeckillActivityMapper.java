package com.studynote.notes.mapper;

import com.studynote.notes.model.entity.SeckillActivity;
import com.studynote.notes.model.vo.seckill.SeckillActivityDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface SeckillActivityMapper {

    int insert(SeckillActivity activity);

    List<SeckillActivity> activityList();

    /**
     * 用户端：查询当前可参与的秒杀活动，并 JOIN 出绑定的课程信息。
     * 与 activityList 的区别：后者是管理端用的，返回全部活动且只返回实体；
     * 本方法只返回用户能参与的活动，并带出课程标题/封面/原价，供前端直接渲染。
     * 用 JOIN 而非先查活动再循环查课程，避免 N+1 查询（该接口访问频繁）。
     * 未开始的活动也要返回，前端需要显示“即将开始”倒计时。
     *
     * @return 可参与的活动（含课程信息），按开始时间正序；无数据时返回空集合而非 null
     */
    List<SeckillActivityDetailVO> findAvailableWithCourse();

    SeckillActivity findById(Integer activityId);

    int update(SeckillActivity activity);

    int deleteById(Integer activityId);

    int deductStock(Integer activityId);

    int addStock(Integer activityId);

    /**
     * 对账：查询这一轮需要参与对账的活动ID。
     * 筛选条件是当前不在秒杀时间窗口内的活动（已结束 end_time &lt; now，或未开始 start_time &gt; now）。
     * 排除进行中的活动是正确性要求（原因见 SeckillService#reconcileOne），不是性能优化。
     * from 是时间下界，避免每轮把历史活动全部翻出来空刷。
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
