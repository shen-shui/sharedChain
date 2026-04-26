package com.atguigu.spzx.order.service.impl;

import com.atguigu.spzx.common.service.exception.GuiguException;
import com.atguigu.spzx.feign.cart.CartFeignClient;
import com.atguigu.spzx.feign.product.ProductFeignClient;
import com.atguigu.spzx.feign.user.UserFeignClient;
import com.atguigu.spzx.model.constant.MqConst;
import com.atguigu.spzx.model.dto.h5.OrderInfoDto;
import com.atguigu.spzx.model.entity.h5.CartInfo;
import com.atguigu.spzx.model.entity.order.OrderInfo;
import com.atguigu.spzx.model.entity.order.OrderItem;
import com.atguigu.spzx.model.entity.order.OrderLog;
import com.atguigu.spzx.model.entity.product.ProductSku;
import com.atguigu.spzx.model.entity.user.UserAddress;
import com.atguigu.spzx.model.entity.user.UserInfo;
import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.model.vo.h5.TradeVo;
import com.atguigu.spzx.order.mapper.OrderInfoMapper;
import com.atguigu.spzx.order.mapper.OrderItemMapper;
import com.atguigu.spzx.order.mapper.OrderLogMapper;
import com.atguigu.spzx.order.service.OrderInfoService;
import com.atguigu.spzx.util.AuthContextUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OrderInfoServiceImpl implements OrderInfoService {

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public TradeVo getTrade() {
        List<CartInfo> cartInfoList = cartFeignClient.getAllChecked();
        List<OrderItem> orderItemList = new ArrayList<>();
        for (CartInfo cartInfo : cartInfoList) {
            OrderItem orderItem = new OrderItem();
            orderItem.setSkuId(cartInfo.getSkuId());
            orderItem.setSkuName(cartInfo.getSkuName());
            orderItem.setSkuNum(cartInfo.getSkuNum());
            orderItem.setSkuPrice(cartInfo.getCartPrice());
            orderItem.setThumbImg(cartInfo.getImgUrl());
            orderItemList.add(orderItem);
        }

        BigDecimal totalAmount = new BigDecimal(0);
        for (OrderItem orderItem : orderItemList) {
            totalAmount = totalAmount.add(orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuNum())));
        }

        TradeVo tradeVo = new TradeVo();
        tradeVo.setOrderItemList(orderItemList);
        tradeVo.setTotalAmount(totalAmount);
        return tradeVo;
    }

    @Override
    public Long submitOrder(OrderInfoDto orderInfoDto) {
        // 数据校验
        List<OrderItem> orderItemList = orderInfoDto.getOrderItemList();
        // 如果数据为空 说明购物车里没有选中的数据
        if (CollectionUtils.isEmpty(orderItemList)) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 获取当前用户
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        if (userInfo == null) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }
        Long userId = userInfo.getId();

        // 幂等：同一用户 + 同一购物车内容 在 5 秒内只能提交一次，防止连续点击重复下单
        String cartFingerprint = orderItemList.stream()
                .sorted(Comparator.comparing(OrderItem::getSkuId))
                .map(o -> o.getSkuId() + ":" + o.getSkuNum())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        String cartHash = DigestUtils.md5DigestAsHex(cartFingerprint.getBytes(StandardCharsets.UTF_8));
        String idempotencyKey = "order:submit:" + userId + ":" + cartHash;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new GuiguException(ResultCodeEnum.SUBMIT_ORDER_REPEAT);
        }

        try {
            return doSubmitOrder(orderInfoDto, orderItemList, userInfo);
        } catch (Exception e) {
            redisTemplate.delete(idempotencyKey);
            throw e;
        }
    }

    private Long doSubmitOrder(OrderInfoDto orderInfoDto, List<OrderItem> orderItemList, UserInfo userInfo) {
        // 校验库存
        for (OrderItem orderItem : orderItemList) {
            // 根据skuId获取sku剩余库存
            ProductSku productSku = productFeignClient.getBySkuId(orderItem.getSkuId());
            if (null == productSku) {
                throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            }
            if (orderItem.getSkuNum() > productSku.getStockNum()) {
                throw new GuiguException(ResultCodeEnum.STOCK_LESS);
            }
        }

        // 分布式锁 + 原子扣减库存，防止超卖
        orderItemList.sort((a, b) -> a.getSkuId().compareTo(b.getSkuId()));
        List<RLock> lockList = new ArrayList<>();
        try {
            // 加锁
            for (OrderItem orderItem : orderItemList) {
                String lockKey = "lock:stock:" + orderItem.getSkuId();
                RLock lock = redissonClient.getLock(lockKey);
                if (!lock.tryLock(3, 5, java.util.concurrent.TimeUnit.SECONDS)) {
                    throw new GuiguException(ResultCodeEnum.STOCK_LESS);
                }
                lockList.add(lock);
            }
            // 扣减库存（数据库原子操作）
            for (OrderItem orderItem : orderItemList) {
                Result<Boolean> result = productFeignClient.deductStock(orderItem.getSkuId(), orderItem.getSkuNum());
                if (result == null || !ResultCodeEnum.SUCCESS.getCode().equals(result.getCode())
                        || !Boolean.TRUE.equals(result.getData())) {
                    throw new GuiguException(ResultCodeEnum.STOCK_LESS);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            for (RLock lock : lockList) {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        // 构建订单数据
        OrderInfo orderInfo = new OrderInfo();
        // 订单编号
        orderInfo.setOrderNo(String.valueOf(System.currentTimeMillis()));
        // 用户id
        orderInfo.setUserId(userInfo.getId());
        // 用户昵称
        orderInfo.setNickName(userInfo.getNickName());
        // 用户收货地址
        UserAddress userAddress = userFeignClient.getUserAddress(orderInfoDto.getUserAddressId());
        orderInfo.setReceiverName(userAddress.getName());
        orderInfo.setReceiverPhone(userAddress.getPhone());
        orderInfo.setReceiverTagName(userAddress.getTagName());
        orderInfo.setReceiverProvince(userAddress.getProvinceCode());
        orderInfo.setReceiverCity(userAddress.getCityCode());
        orderInfo.setReceiverDistrict(userAddress.getDistrictCode());
        orderInfo.setReceiverAddress(userAddress.getFullAddress());
        // 订单金额
        BigDecimal totalAmount = new BigDecimal(0);
        for (OrderItem orderItem : orderItemList) {
            totalAmount = totalAmount.add(orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuNum())));
        }
        orderInfo.setTotalAmount(totalAmount);
        orderInfo.setCouponAmount(new BigDecimal(0));
        orderInfo.setOriginalTotalAmount(totalAmount);
        orderInfo.setFeightFee(orderInfoDto.getFeightFee());
        orderInfo.setPayType(2);
        orderInfo.setOrderStatus(0);
        orderInfoMapper.save(orderInfo);
        // 保存订单明细
        for (OrderItem orderItem : orderItemList) {
            orderItem.setOrderId(orderInfo.getId());
            orderItemMapper.save(orderItem);
        }
        // 记录日志
        OrderLog orderLog = new OrderLog();
        orderLog.setOrderId(orderInfo.getId());
        orderLog.setProcessStatus(0);
        orderLog.setNote("提交订单");
        orderLogMapper.save(orderLog);
        // 远程调用service-cart微服务接口清空购物车数据
        cartFeignClient.deleteChecked();

        // 发送延迟消息：30 分钟未支付自动取消（RocketMQ 延迟级别 16 = 30分钟）
        rocketMQTemplate.syncSend(
                MqConst.destination(MqConst.TAG_ORDER_TIMEOUT),
                MessageBuilder.withPayload(orderInfo.getOrderNo()).build(),
                3000,
                MqConst.DELAY_LEVEL_ORDER_TIMEOUT
        );

        return orderInfo.getId();
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        return orderInfoMapper.getById(orderId);
    }

    @Override
    public TradeVo buy(Long skuId) {
        TradeVo tradeVo = new TradeVo();
        List<OrderItem> orderItemList = new ArrayList<>();
        ProductSku productSku = productFeignClient.getBySkuId(skuId);
        OrderItem orderItem = new OrderItem();
        orderItem.setSkuId(skuId);
        orderItem.setSkuName(productSku.getSkuName());
        orderItem.setThumbImg(productSku.getThumbImg());
        orderItem.setSkuPrice(productSku.getSalePrice());
        orderItem.setSkuNum(1);
        orderItemList.add(orderItem);

        tradeVo.setOrderItemList(orderItemList);
        tradeVo.setTotalAmount(productSku.getSalePrice());
        return tradeVo;
    }

    @Override
    public PageInfo<OrderInfo> findUserPage(Integer page, Integer limit, Integer orderStatus) {
        PageHelper.startPage(page, limit);
        Long userId = AuthContextUtil.getUserInfo().getId();
        List<OrderInfo> orderInfoList = orderInfoMapper.findUserPage(userId, orderStatus);

        orderInfoList.forEach(orderInfo -> {
            List<OrderItem> orderItemList = orderItemMapper.findByOrderId(orderInfo.getId());
            orderInfo.setOrderItemList(orderItemList);
        });

        return new PageInfo<>(orderInfoList);
    }

    @Override
    public OrderInfo getByOrderNo(String orderNo) {
        OrderInfo orderInfo = orderInfoMapper.getByOrderNo(orderNo);
        List<OrderItem> orderItemList = orderItemMapper.findByOrderId(orderInfo.getId());
        orderInfo.setOrderItemList(orderItemList);
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderNo, Integer orderStatus) {
        // 更新订单状态
        OrderInfo orderInfo = orderInfoMapper.getByOrderNo(orderNo);
        orderInfo.setOrderStatus(1);
        orderInfo.setPayType(orderStatus);
        orderInfo.setPaymentTime(new Date());
        orderInfoMapper.updateById(orderInfo);

        // 记录日志
        OrderLog orderLog = new OrderLog();
        orderLog.setOrderId(orderInfo.getId());
        orderLog.setProcessStatus(1);
        orderLog.setNote("支付宝支付成功");
        orderLogMapper.save(orderLog);
    }
}
