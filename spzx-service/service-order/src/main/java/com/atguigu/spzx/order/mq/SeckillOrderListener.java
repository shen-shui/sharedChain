package com.atguigu.spzx.order.mq;

import com.atguigu.spzx.feign.product.ProductFeignClient;
import com.atguigu.spzx.model.constant.MqConst;
import com.atguigu.spzx.model.entity.order.OrderInfo;
import com.atguigu.spzx.model.entity.order.OrderItem;
import com.atguigu.spzx.model.entity.order.OrderLog;
import com.atguigu.spzx.model.entity.order.StockReservation;
import com.atguigu.spzx.model.entity.product.ProductSku;
import com.atguigu.spzx.order.mapper.OrderInfoMapper;
import com.atguigu.spzx.order.mapper.OrderItemMapper;
import com.atguigu.spzx.order.mapper.OrderLogMapper;
import com.atguigu.spzx.order.mapper.StockReservationMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 秒杀订单异步创建消费者
 */
@Component
@RocketMQMessageListener(
        topic = MqConst.TOPIC_ORDER_EVENT,
        consumerGroup = MqConst.CONSUMER_GROUP_SECKILL,
        selectorExpression = MqConst.TAG_SECKILL
)
public class SeckillOrderListener implements RocketMQListener<SeckillOrderMessage> {

    private static final int RESERVATION_STATUS_RESERVED = 0;
    private static final long RESERVATION_TIMEOUT_MILLIS = 30L * 60 * 1000;

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
    public void onMessage(SeckillOrderMessage message) {
        // 幂等：如果已存在同一订单号，直接返回
        OrderInfo exist = orderInfoMapper.getByOrderNo(message.getOrderNo());
        if (exist != null) {
            return;
        }

        // 查询商品信息（用于价格、名称、图片）
        ProductSku productSku = productFeignClient.getBySkuId(message.getSkuId());
        if (productSku == null) {
            return;
        }

        BigDecimal price = message.getSeckillPrice() != null
                ? message.getSeckillPrice()
                : productSku.getSalePrice();

        // 构建订单
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(message.getOrderNo());
        orderInfo.setUserId(message.getUserId());
        orderInfo.setNickName(message.getNickName());
        orderInfo.setTotalAmount(price.multiply(new BigDecimal(message.getSkuNum())));
        orderInfo.setCouponAmount(BigDecimal.ZERO);
        orderInfo.setOriginalTotalAmount(orderInfo.getTotalAmount());
        orderInfo.setFeightFee(BigDecimal.ZERO);
        orderInfo.setPayType(2);       // 默认支付宝
        orderInfo.setOrderStatus(0);   // 待付款
        orderInfoMapper.save(orderInfo);

        // 构建订单明细（单件商品）
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(orderInfo.getId());
        orderItem.setSkuId(message.getSkuId());
        orderItem.setSkuNum(message.getSkuNum());
        orderItem.setSkuName(productSku.getSkuName());
        orderItem.setSkuPrice(price);
        orderItem.setThumbImg(productSku.getThumbImg());
        orderItemMapper.save(orderItem);

        // 记录订单日志
        OrderLog orderLog = new OrderLog();
        orderLog.setOrderId(orderInfo.getId());
        orderLog.setProcessStatus(0);
        orderLog.setNote("秒杀下单创建订单");
        orderLog.setCreateTime(new Date());
        orderLogMapper.save(orderLog);

        // 建单成功后写入库存预占记录，后续用于支付确认或超时回补
        StockReservation stockReservation = new StockReservation();
        stockReservation.setOrderNo(orderInfo.getOrderNo());
        stockReservation.setUserId(orderInfo.getUserId());
        stockReservation.setSkuId(message.getSkuId());
        stockReservation.setReserveNum(message.getSkuNum());
        stockReservation.setReservationStatus(RESERVATION_STATUS_RESERVED);
        stockReservation.setExpireTime(new Date(System.currentTimeMillis() + RESERVATION_TIMEOUT_MILLIS));
        stockReservationMapper.save(stockReservation);
    }
}
