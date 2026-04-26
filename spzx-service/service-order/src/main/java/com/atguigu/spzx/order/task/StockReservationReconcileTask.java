package com.atguigu.spzx.order.task;

import com.atguigu.spzx.order.properties.SeckillProperties;
import com.atguigu.spzx.order.service.StockReservationReconcileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StockReservationReconcileTask {

    @Autowired
    private StockReservationReconcileService stockReservationReconcileService;

    @Autowired
    private SeckillProperties seckillProperties;

    @Scheduled(fixedDelayString = "${spzx.seckill.reservation-reconcile-delay-ms:300000}")
    public void reconcile() {
        stockReservationReconcileService.reconcile(seckillProperties.getReservationReconcileBatchSize());
    }
}
