package com.atguigu.spzx.order.mq;

import com.atguigu.spzx.model.constant.MqConst;
import com.atguigu.spzx.model.entity.order.OrderInfo;
import com.atguigu.spzx.model.entity.order.OrderLog;
import com.atguigu.spzx.order.mapper.OrderInfoMapper;
import com.atguigu.spzx.order.mapper.OrderLogMapper;
import com.atguigu.spzx.order.mapper.StockReservationMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
@RocketMQMessageListener(
        topic = MqConst.TOPIC_ORDER_EVENT,
        consumerGroup = MqConst.CONSUMER_GROUP_TIMEOUT_CANCEL,
        selectorExpression = MqConst.TAG_ORDER_TIMEOUT
)
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
    private RedisTemplate<String, String> redisTemplate;

    private String buildStockKey(Long skuId) {
        return "seckill:stock:" + skuId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.getByOrderNo(orderNo);
        if (orderInfo == null) {
            return;
        }
        // 只取消仍处于待付款状态的订单（0）
        if (orderInfo.getOrderStatus() == null || orderInfo.getOrderStatus() != ORDER_STATUS_UNPAID) {
            return;
        }

        orderInfo.setOrderStatus(ORDER_STATUS_CANCELLED);
        orderInfo.setCancelTime(new Date());
        orderInfo.setCancelReason("订单超时未支付，系统自动取消");
        orderInfoMapper.updateById(orderInfo);

        // 只有从 RESERVED 释放成功时才回补 Redis，避免重复回补
        int released = stockReservationMapper.releaseByOrderNo(orderNo, RESERVATION_STATUS_RESERVED);
        if (released > 0) {
            com.atguigu.spzx.model.entity.order.StockReservation reservation = stockReservationMapper.getByOrderNo(orderNo);
            if (reservation != null && reservation.getSkuId() != null && reservation.getReserveNum() != null) {
                redisTemplate.opsForValue().increment(buildStockKey(reservation.getSkuId()), reservation.getReserveNum());
            }
        }

        OrderLog orderLog = new OrderLog();
        orderLog.setOrderId(orderInfo.getId());
        orderLog.setProcessStatus(ORDER_STATUS_CANCELLED);
        orderLog.setNote("订单超时未支付，系统自动取消");
        orderLogMapper.save(orderLog);
    }
}
