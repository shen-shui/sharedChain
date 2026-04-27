package com.atguigu.spzx.order.controller;

import com.atguigu.spzx.feign.product.ProductFeignClient;
import com.atguigu.spzx.model.constant.MqConst;
import com.atguigu.spzx.model.entity.product.ProductSku;
import com.atguigu.spzx.model.entity.user.UserInfo;
import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.order.mq.SeckillOrderMessage;
import com.atguigu.spzx.order.properties.SeckillProperties;
import com.atguigu.spzx.util.AuthContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 简化版秒杀接口：演示 Redis 预减库存 + MQ 削峰
 */
@RestController
@RequestMapping("/api/order/seckill")
@Slf4j
public class SeckillController {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private SeckillProperties seckillProperties;

    private String buildStockKey(Long skuId) {
        return "seckill:stock:" + skuId;
    }

    private String buildUserKey(Long userId, Long skuId) {
        return "seckill:order:" + seckillProperties.getActivityId() + ":" + userId + ":" + skuId;
    }

    private Long executePreDeductLua(String stockKey) {
        // Lua 原子化：库存不存在/不足直接失败，避免 check-then-decrement 并发窗口
        String scriptText = """
                local stock = redis.call('GET', KEYS[1])
                if (not stock) then
                    return -2
                end
                if (tonumber(stock) <= 0) then
                    return -1
                end
                return redis.call('DECR', KEYS[1])
                """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(scriptText);
        script.setResultType(Long.class);
        return redisTemplate.execute(script, Collections.singletonList(stockKey));
    }

    /**
     * 秒杀下单入口（单商品、单数量的示例）
     */
    @PostMapping("/doSeckill/{skuId}")
    public Result<String> doSeckill(@PathVariable Long skuId) {
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        if (userInfo == null) {
            return Result.<String>build(null, ResultCodeEnum.LOGIN_AUTH);
        }
        Long userId = userInfo.getId();

        // 防止同一用户对同一商品重复秒杀
        String userKey = buildUserKey(userId, skuId);
        Boolean firstSeckill = redisTemplate.opsForValue().setIfAbsent(
                userKey,
                "1",
                seckillProperties.getUserLimitTtlMinutes(),
                TimeUnit.MINUTES
        );
        if (!Boolean.TRUE.equals(firstSeckill)) {
            log.info("seckill_rejected_repeat userId={} skuId={} userKey={}", userId, skuId, userKey);
            // 已经参与过本次秒杀
            return Result.<String>build("请勿重复秒杀", ResultCodeEnum.DATA_ERROR);
        }

        // 严格预热驱动：库存 key 必须在活动预热阶段提前写入 Redis，运行期不再回源 MySQL 初始化
        String stockKey = buildStockKey(skuId);
        if (Boolean.FALSE.equals(redisTemplate.hasKey(stockKey))) {
            log.info("seckill_rejected_stock_not_preheated userId={} skuId={} stockKey={}", userId, skuId, stockKey);
            return Result.<String>build(null, ResultCodeEnum.STOCK_LESS);
        }

        Long left = executePreDeductLua(stockKey);
        if (left == null || left < 0) {
            log.info("seckill_rejected_deduct_failed userId={} skuId={} left={}", userId, skuId, left);
            return Result.<String>build(null, ResultCodeEnum.STOCK_LESS);
        }

        // 组装秒杀订单消息
        SeckillOrderMessage message = new SeckillOrderMessage();
        String orderNo = System.currentTimeMillis() + "_" + userId + "_" + skuId;
        message.setOrderNo(orderNo);
        message.setUserId(userId);
        message.setNickName(userInfo.getNickName());
        message.setSkuId(skuId);
        message.setSkuNum(1);
        // 示例中直接使用商品售价作为秒杀价
        ProductSku productSku = productFeignClient.getBySkuId(skuId);
        if (productSku != null) {
            message.setSeckillPrice(productSku.getSalePrice());
        }

        // 发送到秒杀队列，由后台异步创建订单（削峰填谷）
        rocketMQTemplate.syncSend(MqConst.destination(MqConst.TAG_SECKILL), message);
        log.info("seckill_accepted orderNo={} userId={} skuId={} leftStock={}", orderNo, userId, skuId, left);

        return Result.<String>build("秒杀请求已受理，正在排队中", ResultCodeEnum.SUCCESS);
    }
}

