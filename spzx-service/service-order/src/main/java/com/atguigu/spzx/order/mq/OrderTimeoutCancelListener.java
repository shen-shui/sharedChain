package com.atguigu.spzx.order.mq;

import com.atguigu.spzx.feign.product.ProductFeignClient;
import com.atguigu.spzx.model.constant.MqConst;
import com.atguigu.spzx.model.entity.order.OrderItem;
import com.atguigu.spzx.model.entity.order.OrderInfo;
import com.atguigu.spzx.model.entity.order.OrderLog;
import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.order.mapper.OrderInfoMapper;
import com.atguigu.spzx.order.mapper.OrderItemMapper;
import com.atguigu.spzx.order.mapper.OrderLogMapper;
import com.atguigu.spzx.order.mapper.StockReservationMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RocketMQMessageListener(
        topic = MqConst.TOPIC_ORDER_EVENT,
        consumerGroup = MqConst.CONSUMER_GROUP_TIMEOUT_CANCEL,
        selectorExpression = MqConst.TAG_ORDER_TIMEOUT,
        maxReconsumeTimes = 5
)
@Slf4j
public class OrderTimeoutCancelListener implements RocketMQListener<String> {

    private static final int ORDER_STATUS_UNPAID = 0;
    private static final int ORDER_STATUS_CANCELLED = -1;
    private static final int RESERVATION_STATUS_RESERVED = 0;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

    @Autowired
    private StockReservationMapper stockReservationMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private String buildStockKey(Long skuId) {
        return "seckill:stock:" + skuId;
    }

    private String buildMysqlRestockKey(String orderNo, Long skuId) {
        return "order:restock:mysql:" + orderNo + ":" + skuId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.getByOrderNo(orderNo);
        if (orderInfo == null) {
            log.error("timeout_cancel_retryable_missing_order orderNo={}", orderNo);
            throw new IllegalStateException("timeout_cancel_retryable_missing_order");
        }
        Integer currentStatus = orderInfo.getOrderStatus();
        // 允许重复消息/重试：如果已经取消，继续走“库存释放/回补”兜底；其他状态直接跳过
        if (currentStatus == null) {
            log.info("timeout_cancel_skip_status_null orderNo={}", orderNo);
            return;
        }
        if (currentStatus == ORDER_STATUS_UNPAID) {
            // 使用 CAS 更新订单状态，避免与支付成功并发时相互覆盖
            int statusUpdated = orderInfoMapper.updateStatusByOrderNo(orderNo, ORDER_STATUS_UNPAID, ORDER_STATUS_CANCELLED);
            if (statusUpdated <= 0) {
                log.info("timeout_cancel_skip_status_cas_failed orderNo={}", orderNo);
                return;
            }
            orderInfo.setOrderStatus(ORDER_STATUS_CANCELLED);
            orderInfo.setCancelTime(new Date());
            orderInfo.setCancelReason("订单超时未支付，系统自动取消");
            orderInfoMapper.updateById(orderInfo);
        } else if (currentStatus != ORDER_STATUS_CANCELLED) {
            log.info("timeout_cancel_skip_status_mismatch orderNo={} orderStatus={}", orderNo, currentStatus);
            return;
        }

        // 秒杀单：存在预占记录则走“释放预占 + 回补 Redis”
        com.atguigu.spzx.model.entity.order.StockReservation reservation = stockReservationMapper.getByOrderNo(orderNo);
        if (reservation != null) {
            int released = stockReservationMapper.releaseByOrderNo(orderNo, RESERVATION_STATUS_RESERVED);
            if (released > 0 && reservation.getSkuId() != null && reservation.getReserveNum() != null) {
                redisTemplate.opsForValue().increment(buildStockKey(reservation.getSkuId()), reservation.getReserveNum());
                log.info("timeout_cancel_restock_seckill orderNo={} skuId={} reserveNum={}",
                        orderNo, reservation.getSkuId(), reservation.getReserveNum());
            }
            log.info("timeout_cancel_processed_seckill orderNo={} reservationReleased={}", orderNo, released);
        } else {
            // 普通单：下单即扣 MySQL 库存，取消需要回补 MySQL（并做最小幂等，支持重试）
            List<OrderItem> orderItemList = orderItemMapper.findByOrderId(orderInfo.getId());
            for (OrderItem item : orderItemList) {
                String restockKey = buildMysqlRestockKey(orderNo, item.getSkuId());
                Boolean claimed = redisTemplate.opsForValue().setIfAbsent(restockKey, "1", 7, TimeUnit.DAYS);
                if (!Boolean.TRUE.equals(claimed)) {
                    log.info("timeout_cancel_skip_mysql_restock_duplicate orderNo={} skuId={}", orderNo, item.getSkuId());
                    continue;
                }
                Result<Boolean> result = productFeignClient.restoreStock(item.getSkuId(), item.getSkuNum());
                if (result == null || !ResultCodeEnum.SUCCESS.getCode().equals(result.getCode())
                        || !Boolean.TRUE.equals(result.getData())) {
                    redisTemplate.delete(restockKey);
                    log.error("timeout_cancel_mysql_restock_failed orderNo={} skuId={} skuNum={} result={}",
                            orderNo, item.getSkuId(), item.getSkuNum(), result);
                    throw new IllegalStateException("timeout_cancel_mysql_restock_failed");
                }
                log.info("timeout_cancel_mysql_restock_success orderNo={} skuId={} skuNum={}",
                        orderNo, item.getSkuId(), item.getSkuNum());
            }
            log.info("timeout_cancel_processed_normal orderNo={} itemCount={}", orderNo, orderItemList.size());
        }

        OrderLog orderLog = new OrderLog();
        orderLog.setOrderId(orderInfo.getId());
        orderLog.setProcessStatus(ORDER_STATUS_CANCELLED);
        orderLog.setNote("订单超时未支付，系统自动取消");
        orderLogMapper.save(orderLog);
    }
}
