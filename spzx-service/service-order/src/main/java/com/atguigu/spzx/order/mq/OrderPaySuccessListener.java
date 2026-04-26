package com.atguigu.spzx.order.mq;

import com.atguigu.spzx.feign.product.ProductFeignClient;
import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.model.constant.MqConst;
import com.atguigu.spzx.model.dto.product.SkuSaleDto;
import com.atguigu.spzx.model.entity.order.OrderInfo;
import com.atguigu.spzx.model.entity.order.OrderItem;
import com.atguigu.spzx.model.entity.order.OrderLog;
import com.atguigu.spzx.model.entity.order.StockReservation;
import com.atguigu.spzx.order.mapper.OrderInfoMapper;
import com.atguigu.spzx.order.mapper.OrderItemMapper;
import com.atguigu.spzx.order.mapper.OrderLogMapper;
import com.atguigu.spzx.order.mapper.StockReservationMapper;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OrderPaySuccessListener implements RocketMQListener<String> {

    private static final int ORDER_STATUS_UNPAID = 0;
    private static final int ORDER_STATUS_PAID = 1;
    private static final int RESERVATION_STATUS_RESERVED = 0;
    private static final int RESERVATION_STATUS_CONFIRMED = 1;
    private static final int RESERVATION_STATUS_RELEASED = 2;

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
            log.warn("pay_success_skip_missing_order orderNo={}", orderNo);
            return;
        }
        // 幂等处理：如果已经是已支付状态，则不再处理
        if (orderInfo.getOrderStatus() != null && orderInfo.getOrderStatus() == ORDER_STATUS_PAID) {
            log.info("pay_success_skip_already_paid orderNo={}", orderNo);
            return;
        }
        if (orderInfo.getOrderStatus() == null || orderInfo.getOrderStatus() != ORDER_STATUS_UNPAID) {
            log.info("pay_success_skip_status_mismatch orderNo={} orderStatus={}", orderNo, orderInfo.getOrderStatus());
            return;
        }

        StockReservation reservation = stockReservationMapper.getByOrderNo(orderNo);
        if (reservation == null) {
            log.warn("pay_success_skip_missing_reservation orderNo={}", orderNo);
            return;
        }
        if (reservation.getReservationStatus() != null && reservation.getReservationStatus() == RESERVATION_STATUS_RELEASED) {
            log.warn("pay_success_skip_released_reservation orderNo={}", orderNo);
            return;
        }

        int confirmed = 0;
        if (reservation.getReservationStatus() != null && reservation.getReservationStatus() == RESERVATION_STATUS_RESERVED) {
            confirmed = stockReservationMapper.confirmByOrderNo(orderNo, RESERVATION_STATUS_RESERVED);
        } else if (reservation.getReservationStatus() != null && reservation.getReservationStatus() == RESERVATION_STATUS_CONFIRMED) {
            confirmed = 1;
        }
        log.info("pay_success_reservation_confirm orderNo={} confirmed={}", orderNo, confirmed);
        if (confirmed <= 0) {
            return;
        }

        // 已支付订单才写入 MySQL 最终库存，补齐秒杀场景最终账本闭环
        List<OrderItem> orderItemList = orderItemMapper.findByOrderId(orderInfo.getId());
        for (OrderItem item : orderItemList) {
            Result<Boolean> result = productFeignClient.deductStock(item.getSkuId(), item.getSkuNum());
            if (result == null || !ResultCodeEnum.SUCCESS.getCode().equals(result.getCode())
                    || !Boolean.TRUE.equals(result.getData())) {
                log.error("pay_success_mysql_deduct_failed orderNo={} skuId={} skuNum={} result={}",
                        orderNo, item.getSkuId(), item.getSkuNum(), result);
                throw new IllegalStateException("pay_success_mysql_deduct_failed");
            }
        }

        // 更新订单状态
        orderInfo.setOrderStatus(ORDER_STATUS_PAID);
        // 支付方式：2 表示支付宝
        orderInfo.setPayType(2);
        orderInfo.setPaymentTime(new Date());
        orderInfoMapper.updateById(orderInfo);

        // 记录订单日志
        OrderLog orderLog = new OrderLog();
        orderLog.setOrderId(orderInfo.getId());
        orderLog.setProcessStatus(ORDER_STATUS_PAID);
        orderLog.setNote("支付宝支付成功（MQ）");
        orderLogMapper.save(orderLog);

        // 异步更新商品销量
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
        log.info("pay_success_processed orderNo={} itemCount={}", orderNo, skuSaleDtoList.size());
    }
}
