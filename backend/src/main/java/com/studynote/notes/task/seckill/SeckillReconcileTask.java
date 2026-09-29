package com.studynote.notes.task.seckill;

import com.studynote.notes.service.SeckillService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 对账任务：以 MySQL 为准，修复 Redis 里漂移的秒杀状态。
 */
@Log4j2
@Component
public class SeckillReconcileTask {

    @Autowired
    SeckillService seckillService;

    /** 单次最多对账几个活动 */
    @Value("${seckill.reconcile.batch-size}")
    private int batchSize;

    /** 只对账最近几小时内结束的活动 */
    @Value("${seckill.reconcile.retain-hours}")
    private int retainHours;

    @Scheduled(fixedDelayString = "${seckill.reconcile.interval-ms}")
    public void reconcile() {
        // 先转 long 防 int 溢出
        Date now = new Date();
        Date from = new Date(now.getTime() - retainHours * 3600L * 1000L);

        // 排除正在进行中的活动
        List<Integer> activityIds = seckillService.findReconcileCandidates(now, from, batchSize);
        if (activityIds.isEmpty()) {
            return;
        }

        // 单个活动失败不连累整批
        int repaired = 0;
        for (Integer activityId : activityIds) {
            try {
                if (seckillService.reconcileOne(activityId)) {
                    repaired++;
                }
            } catch (Exception e) {
                log.error("对账失败 activityId={}", activityId, e);
            }
        }

        if (repaired > 0) {
            log.info("对账完成 扫描={} 修复={}", activityIds.size(), repaired);
        } else {
            log.debug("对账完成 扫描={} 无漂移", activityIds.size());
        }
    }
}
