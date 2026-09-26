package com.studynote.notes.mapper;

import com.studynote.notes.model.entity.SeckillOrder;
import com.studynote.notes.model.vo.seckill.SeckillOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Mapper
public interface SeckillOrderMapper {

    /**
     * 插入一条秒杀订单。主键自增，插入后回填到 order.orderId。
     *
     * @param order 订单实体
     * @return 受影响行数
     */
    int insert(SeckillOrder order);

    /**
     * 根据用户id和活动id查询是否已存在该订单
     *
     * @param activityId,userId 订单实体
     * @return
     */
    SeckillOrder findByActivityIdAndUserId(int activityId, Long userId);

    /**
     * 根据订单id查询该订单
     *
     * @param orderId 订单实体
     * @return
     */
    SeckillOrder findByOrderId(int orderId);

    /**
     * 查询某个用户的全部秒杀订单，按下单时间倒序。
     * 与 findByActivityIdAndUserId 不同，这里不过滤 status，因为“我的订单”需要展示已取消的历史记录。
     * 订单是异步落库的（Redis Stream 消费），抢购接口拿不到 orderId，支付需靠此方法反查。
     * 排序用 created_at DESC, order_id DESC：同一秒下的单靠自增主键兜底，保证翻页顺序确定。
     *
     * @param userId 用户ID（从 token 解析，不由前端传入）
     * @return 该用户的订单列表；无订单时返回空集合而非 null
     */
    List<SeckillOrderVO> findVOByUserId(@Param("userId") Long userId);


    List<Long> findActiveUserIdsByActivityId(int activityId);

    int updateStatus(int orderId);

    List<SeckillOrder> findTimeoutOrders(Date deadline, int limit);

    int cancelOrder(int orderId);

}
