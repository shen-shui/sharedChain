package com.atguigu.spzx.order.service;

import com.atguigu.spzx.model.entity.order.StockReservation;
import com.atguigu.spzx.order.mapper.StockReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockReservationReconcileService {

    private static final int ORDER_STATUS_PAID = 1;
    private static final int ORDER_STATUS_CANCELLED = -1;
    private static final int RESERVATION_STATUS_RESERVED = 0;

    @Autowired
    private StockReservationMapper stockReservationMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private String buildStockKey(Long skuId) {
        return "seckill:stock:" + skuId;
    }

    public Map<String, Integer> reconcile(int batchSize) {
        int confirmedCount = reconcilePaidOrders(batchSize);
        int releasedCount = reconcileCancelledOrders(batchSize);
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("confirmedCount", confirmedCount);
        result.put("releasedCount", releasedCount);
        return result;
    }

    private int reconcilePaidOrders(int batchSize) {
        int confirmedCount = 0;
        List<StockReservation> paidOrderReservedList = stockReservationMapper.findReservedByOrderStatus(ORDER_STATUS_PAID, batchSize);
        for (StockReservation reservation : paidOrderReservedList) {
            int updated = stockReservationMapper.confirmByOrderNo(reservation.getOrderNo(), RESERVATION_STATUS_RESERVED);
            if (updated > 0) {
                confirmedCount += 1;
            }
        }
        return confirmedCount;
    }

    private int reconcileCancelledOrders(int batchSize) {
        int releasedCount = 0;
        List<StockReservation> cancelledOrderReservedList = stockReservationMapper.findReservedByOrderStatus(ORDER_STATUS_CANCELLED, batchSize);
        for (StockReservation reservation : cancelledOrderReservedList) {
            int updated = stockReservationMapper.releaseByOrderNo(reservation.getOrderNo(), RESERVATION_STATUS_RESERVED);
            if (updated > 0) {
                releasedCount += 1;
                if (reservation.getSkuId() != null && reservation.getReserveNum() != null) {
                    redisTemplate.opsForValue().increment(buildStockKey(reservation.getSkuId()), reservation.getReserveNum());
                }
            }
        }
        return releasedCount;
    }
}
