package com.atguigu.spzx.model.constant;

/**
 * RocketMQ 常量：Topic + Tag，对应原 RabbitMQ 的 Exchange + routing key。
 */
public class MqConst {

    /** 订单域统一 Topic（部署时需在 Broker 创建或开启自动创建 Topic） */
    public static final String TOPIC_ORDER_EVENT = "ORDER_EVENT";

    /** 支付成功异步处理 */
    public static final String TAG_PAY_SUCCESS = "PAY_SUCCESS";

    /**
     * 超时未支付关单（原 RabbitMQ：TTL + 死信转发）。
     * 使用 {@link #DELAY_LEVEL_ORDER_TIMEOUT} 发送延迟消息。
     */
    public static final String TAG_ORDER_TIMEOUT = "ORDER_TIMEOUT";

    /** 秒杀异步建单（削峰） */
    public static final String TAG_SECKILL = "SECKILL";

    /**
     * RocketMQ 4.x 内置延迟级别，16 表示 30 分钟（与原 30min 未支付取消一致）。
     * 级别表：1s,5s,10s,30s,1m,2m,...,10m,20m,30m,1h,2h → 30m 为第 16 档。
     */
    public static final int DELAY_LEVEL_ORDER_TIMEOUT = 16;

    /** 消费者组（订阅同一 Topic 的不同业务须使用不同 group） */
    public static final String CONSUMER_GROUP_PAY_SUCCESS = "spzx-order-pay-success-group";
    public static final String CONSUMER_GROUP_TIMEOUT_CANCEL = "spzx-order-timeout-cancel-group";
    public static final String CONSUMER_GROUP_SECKILL = "spzx-order-seckill-group";

    /** Producer 分组（按微服务区分） */
    public static final String PRODUCER_GROUP_ORDER = "spzx-order-producer-group";
    public static final String PRODUCER_GROUP_PAY = "spzx-pay-producer-group";

    /**
     * Spring RocketMQ 发送目标：topic:tag
     */
    public static String destination(String tag) {
        return TOPIC_ORDER_EVENT + ":" + tag;
    }
}
