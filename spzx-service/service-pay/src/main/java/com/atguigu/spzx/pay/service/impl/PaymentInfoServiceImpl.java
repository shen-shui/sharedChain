package com.atguigu.spzx.pay.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.spzx.feign.order.OrderFeignClient;
import com.atguigu.spzx.model.constant.MqConst;
import com.atguigu.spzx.model.entity.order.OrderInfo;
import com.atguigu.spzx.model.entity.order.OrderItem;
import com.atguigu.spzx.model.entity.pay.PaymentInfo;
import com.atguigu.spzx.pay.mapper.PaymentInfoMapper;
import com.atguigu.spzx.pay.service.PaymentInfoService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class PaymentInfoServiceImpl implements PaymentInfoService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public PaymentInfo savePaymentInfo(String orderNo) {
        PaymentInfo paymentInfo = paymentInfoMapper.getByOrderNo(orderNo);
        if (paymentInfo == null) {
            paymentInfo = new PaymentInfo();

            OrderInfo orderInfo = orderFeignClient.getOrderInfoByOrderNo(orderNo).getData();

            paymentInfo.setUserId(orderInfo.getUserId());
            paymentInfo.setPayType(orderInfo.getPayType());
            StringBuilder content = new StringBuilder();
            for (OrderItem item : orderInfo.getOrderItemList()) {
                content.append(item.getSkuName()).append(" ");
            }
            paymentInfo.setContent(content.toString());
            paymentInfo.setAmount(orderInfo.getTotalAmount());
            paymentInfo.setOrderNo(orderNo);
            paymentInfo.setPaymentStatus(0);
            paymentInfoMapper.save(paymentInfo);
        }

        return paymentInfo;
    }

    @Override
    public void updatePaymentStatus(Map<String, String> map, Integer payType) {
        // 查询PaymentInfo
        PaymentInfo paymentInfo = paymentInfoMapper.getByOrderNo(map.get("out_trade_no"));
        if (paymentInfo.getPaymentStatus() == 1) {
            return;
        }

        //更新支付信息
        paymentInfo.setPaymentStatus(1);
        paymentInfo.setOutTradeNo(map.get("trade_no"));
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(JSON.toJSONString(map));
        paymentInfoMapper.updateById(paymentInfo);

        // 支付成功后发送订单支付成功事件，由订单服务异步更新订单状态等
        rocketMQTemplate.syncSend(
                MqConst.destination(MqConst.TAG_PAY_SUCCESS),
                paymentInfo.getOrderNo()
        );
    }

}
