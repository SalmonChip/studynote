package com.studynote.notes.service;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.entity.SeckillOrder;

import java.util.Date;
import java.util.List;

public interface SeckillService {

    /**
     * 用户抢购秒杀活动（②b 朴素版，故意不防并发，会超卖）。
     *
     * @param activityId 活动ID
     * @return 抢购结果
     */
    ApiResponse<EmptyVO> seckill(Integer activityId);

    ApiResponse<EmptyVO> pay(Integer orderId);

    List<SeckillOrder> findTimeoutOrders(Date deadline, int limit);

    void cancelTimeoutOrderOne(SeckillOrder seckillOrder);
    /**
     * 【预热】把 MySQL 中某个活动的库存 + 已下单用户，装载到 Redis。
     *
     * 为什么要"已下单用户"也要预热？
     *   活动开始前可能已经有人下过单（比如活动提前建好、用户提前能点）。
     *   如果只灌库存不灌已购集合，这些人就能在 Redis 里再抢一次。
     *
     * @param activityId 活动ID
     * @return 预热结果
     */
    ApiResponse<EmptyVO> warmUp(Integer activityId);


}
