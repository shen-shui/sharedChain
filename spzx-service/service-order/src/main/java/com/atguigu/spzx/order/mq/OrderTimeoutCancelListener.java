package com.atguigu.spzx.order.mq;

import com.atguigu.spzx.model.constant.MqConst;
import com.atguigu.spzx.model.entity.order.OrderInfo;
import com.atguigu.spzx.model.entity.order.OrderLog;
import com.atguigu.spzx.order.mapper.OrderInfoMapper;
import com.atguigu.spzx.order.mapper.OrderLogMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RocketMQMessageListener(
        topic = MqConst.TOPIC_ORDER_EVENT,
        consumerGroup = MqConst.CONSUMER_GROUP_TIMEOUT_CANCEL,
        selectorExpression = MqConst.TAG_ORDER_TIMEOUT
)
public class OrderTimeoutCancelListener implements RocketMQListener<String> {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

    @Override
    public void onMessage(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.getByOrderNo(orderNo);
        if (orderInfo == null) {
            return;
        }
        // 只取消仍处于待付款状态的订单（0）
        if (orderInfo.getOrderStatus() == null || orderInfo.getOrderStatus() != 0) {
            return;
        }

        orderInfo.setOrderStatus(-1);
        orderInfo.setCancelTime(new Date());
        orderInfo.setCancelReason("订单超时未支付，系统自动取消");
        orderInfoMapper.updateById(orderInfo);

        OrderLog orderLog = new OrderLog();
        orderLog.setOrderId(orderInfo.getId());
        orderLog.setProcessStatus(-1);
        orderLog.setNote("订单超时未支付，系统自动取消");
        orderLogMapper.save(orderLog);
    }
}
