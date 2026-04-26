package com.atguigu.spzx.order.task;

import com.atguigu.spzx.order.service.StockReservationReconcileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StockReservationReconcileTask {

    private static final int RECONCILE_BATCH_SIZE = 100;

    @Autowired
    private StockReservationReconcileService stockReservationReconcileService;

    @Scheduled(fixedDelayString = "${spzx.seckill.reservation-reconcile-delay-ms:300000}")
    public void reconcile() {
        stockReservationReconcileService.reconcile(RECONCILE_BATCH_SIZE);
    }
}
