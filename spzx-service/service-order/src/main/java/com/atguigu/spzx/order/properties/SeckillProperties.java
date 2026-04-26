package com.atguigu.spzx.order.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spzx.seckill")
public class SeckillProperties {

    private String activityId = "default";

    private long userLimitTtlMinutes = 30L;

    private int reservationCompensationBatchSize = 100;

    private int reservationReconcileBatchSize = 100;

    private int manualReconcileBatchSize = 500;

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public long getUserLimitTtlMinutes() {
        return userLimitTtlMinutes;
    }

    public void setUserLimitTtlMinutes(long userLimitTtlMinutes) {
        this.userLimitTtlMinutes = userLimitTtlMinutes;
    }

    public int getReservationCompensationBatchSize() {
        return reservationCompensationBatchSize;
    }

    public void setReservationCompensationBatchSize(int reservationCompensationBatchSize) {
        this.reservationCompensationBatchSize = reservationCompensationBatchSize;
    }

    public int getReservationReconcileBatchSize() {
        return reservationReconcileBatchSize;
    }

    public void setReservationReconcileBatchSize(int reservationReconcileBatchSize) {
        this.reservationReconcileBatchSize = reservationReconcileBatchSize;
    }

    public int getManualReconcileBatchSize() {
        return manualReconcileBatchSize;
    }

    public void setManualReconcileBatchSize(int manualReconcileBatchSize) {
        this.manualReconcileBatchSize = manualReconcileBatchSize;
    }
}
