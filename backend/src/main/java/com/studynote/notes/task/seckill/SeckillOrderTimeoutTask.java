package com.studynote.notes.task.seckill;

import com.studynote.notes.model.entity.SeckillOrder;
import com.studynote.notes.service.SeckillService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.List;

@Log4j2
@Component
public class SeckillOrderTimeoutTask {


    @Autowired
    SeckillService seckillService;

    @Value("${seckill.order.timeout-minutes}")
    private int timeoutMinutes;

    @Value("${seckill.order.scan-batch-size}")
    private int scanBatchSize;

    @Scheduled(fixedDelay = 5000)
    public void cancelTimeoutOrders() {
        log.debug("定时任务扫描过期订单");
        //1.计算当前时间 - 15分钟
        Date now = new Date();
        long time = now.getTime() - timeoutMinutes * 60L * 1000L;
        Date deadline = new Date(time);
        //2.计算回滚的订单库存和取消订单
        List<SeckillOrder> orderList = seckillService.findTimeoutOrders(deadline, scanBatchSize);

        for (SeckillOrder order : orderList) {
            try {
                //取消订单并回补库存
                Boolean isCancelled = seckillService.cancelTimeoutOrderOne(order);
                if (isCancelled) {
                    seckillService.syncRedisAfterCancel(order.getActivityId(), order.getUserId());
                } else {
                    log.debug("订单已被支付或已取消，跳过 orderId={}",order.getOrderId());
                }
            } catch (Exception e) {
                log.error("超时订单任务处理", e);
            }
        }

    }
}

