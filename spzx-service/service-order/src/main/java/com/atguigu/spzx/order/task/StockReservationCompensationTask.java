package com.atguigu.spzx.order.task;

import com.atguigu.spzx.model.entity.order.OrderInfo;
import com.atguigu.spzx.model.entity.order.StockReservation;
import com.atguigu.spzx.order.mapper.OrderInfoMapper;
import com.atguigu.spzx.order.mapper.StockReservationMapper;
import com.atguigu.spzx.order.properties.SeckillProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class StockReservationCompensationTask {

    private static final int ORDER_STATUS_UNPAID = 0;
    private static final int RESERVATION_STATUS_RESERVED = 0;

    @Autowired
    private StockReservationMapper stockReservationMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SeckillProperties seckillProperties;

    private String buildStockKey(Long skuId) {
        return "seckill:stock:" + skuId;
    }

    // 定时补偿过期预占，兜底处理消息丢失或消费者异常导致的未释放场景
    @Scheduled(fixedDelayString = "${spzx.seckill.reservation-compensation-delay-ms:60000}")
    public void compensateExpiredReservation() {
        List<StockReservation> expiredList = stockReservationMapper.findExpiredReserved(
                seckillProperties.getReservationCompensationBatchSize()
        );
        if (expiredList == null || expiredList.isEmpty()) {
            return;
        }
        int releasedCount = 0;
        for (StockReservation reservation : expiredList) {
            OrderInfo orderInfo = orderInfoMapper.getByOrderNo(reservation.getOrderNo());
            if (orderInfo == null) {
                releasedCount += releaseAndRestore(reservation);
                continue;
            }
            if (orderInfo.getOrderStatus() != null && orderInfo.getOrderStatus() == ORDER_STATUS_UNPAID) {
                releasedCount += releaseAndRestore(reservation);
            }
        }
        log.info("reservation_compensation_finished scanned={} released={}", expiredList.size(), releasedCount);
    }

    private int releaseAndRestore(StockReservation reservation) {
        int released = stockReservationMapper.releaseByOrderNo(reservation.getOrderNo(), RESERVATION_STATUS_RESERVED);
        if (released > 0 && reservation.getSkuId() != null && reservation.getReserveNum() != null) {
            redisTemplate.opsForValue().increment(buildStockKey(reservation.getSkuId()), reservation.getReserveNum());
            log.info("reservation_compensation_restock orderNo={} skuId={} reserveNum={}",
                    reservation.getOrderNo(), reservation.getSkuId(), reservation.getReserveNum());
        }
        return released;
    }
}
