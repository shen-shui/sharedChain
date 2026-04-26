package com.atguigu.spzx.order.mq;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秒杀下单消息载体
 */
public class SeckillOrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private Long userId;

    private String nickName;

    private Long skuId;

    private Integer skuNum;

    private BigDecimal seckillPrice;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getSkuNum() {
        return skuNum;
    }

    public void setSkuNum(Integer skuNum) {
        this.skuNum = skuNum;
    }

    public BigDecimal getSeckillPrice() {
        return seckillPrice;
    }

    public void setSeckillPrice(BigDecimal seckillPrice) {
        this.seckillPrice = seckillPrice;
    }
}

