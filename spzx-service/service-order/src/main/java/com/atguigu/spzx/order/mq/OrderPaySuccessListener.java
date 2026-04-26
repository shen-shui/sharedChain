package com.atguigu.spzx.order.mq;

import com.atguigu.spzx.feign.product.ProductFeignClient;
import com.atguigu.spzx.model.constant.MqConst;
import com.atguigu.spzx.model.dto.product.SkuSaleDto;
import com.atguigu.spzx.model.entity.order.OrderInfo;
import com.atguigu.spzx.model.entity.order.OrderItem;
import com.atguigu.spzx.model.entity.order.OrderLog;
import com.atguigu.spzx.order.mapper.OrderInfoMapper;
import com.atguigu.spzx.order.mapper.OrderItemMapper;
import com.atguigu.spzx.order.mapper.OrderLogMapper;
import com.atguigu.spzx.order.mapper.StockReservationMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@RocketMQMessageListener(
        topic = MqConst.TOPIC_ORDER_EVENT,
        consumerGroup = MqConst.CONSUMER_GROUP_PAY_SUCCESS,
        selectorExpression = MqConst.TAG_PAY_SUCCESS
)
public class OrderPaySuccessListener implements RocketMQListener<String> {

    private static final int ORDER_STATUS_UNPAID = 0;
    private static final int ORDER_STATUS_PAID = 1;
    private static final int RESERVATION_STATUS_RESERVED = 0;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private StockReservationMapper stockReservationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.getByOrderNo(orderNo);
        if (orderInfo == null) {
            return;
        }
        // 幂等处理：如果已经是已支付状态，则不再处理
        if (orderInfo.getOrderStatus() != null && orderInfo.getOrderStatus() == ORDER_STATUS_PAID) {
            return;
        }
        if (orderInfo.getOrderStatus() == null || orderInfo.getOrderStatus() != ORDER_STATUS_UNPAID) {
            return;
        }

        // 更新订单状态
        orderInfo.setOrderStatus(ORDER_STATUS_PAID);
        // 支付方式：2 表示支付宝
        orderInfo.setPayType(2);
        orderInfo.setPaymentTime(new Date());
        orderInfoMapper.updateById(orderInfo);

        // 订单支付成功后，确认库存预占状态
        stockReservationMapper.confirmByOrderNo(orderNo, RESERVATION_STATUS_RESERVED);

        // 记录订单日志
        OrderLog orderLog = new OrderLog();
        orderLog.setOrderId(orderInfo.getId());
        orderLog.setProcessStatus(ORDER_STATUS_PAID);
        orderLog.setNote("支付宝支付成功（MQ）");
        orderLogMapper.save(orderLog);

        // 异步更新商品销量
        List<OrderItem> orderItemList = orderItemMapper.findByOrderId(orderInfo.getId());
        List<SkuSaleDto> skuSaleDtoList = new ArrayList<>();
        for (OrderItem item : orderItemList) {
            SkuSaleDto skuSaleDto = new SkuSaleDto();
            skuSaleDto.setSkuId(item.getSkuId());
            skuSaleDto.setNum(item.getSkuNum());
            skuSaleDtoList.add(skuSaleDto);
        }
        if (!skuSaleDtoList.isEmpty()) {
            productFeignClient.updateSkuSaleNum(skuSaleDtoList);
        }
    }
}
