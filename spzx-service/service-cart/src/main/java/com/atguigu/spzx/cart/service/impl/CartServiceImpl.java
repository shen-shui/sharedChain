package com.atguigu.spzx.cart.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.spzx.cart.service.CartService;
import com.atguigu.spzx.feign.product.ProductFeignClient;
import com.atguigu.spzx.model.entity.h5.CartInfo;
import com.atguigu.spzx.model.entity.product.ProductSku;
import com.atguigu.spzx.util.AuthContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    private String getCartKey(Long userId) {
        //定义key user:cart:userId
        return "user:cart:" + userId;
    }

    @Override
    public void addToCart(Long skuId, Integer skuNum) {
        // 获取当前登陆用户id
        Long userId = AuthContextUtil.getUserInfo().getId();
        String cartKey = this.getCartKey(userId);

        // 从redis中获取购物车信息
        Object cartInfoObj = redisTemplate.opsForHash().get(cartKey, String.valueOf(skuId));
        CartInfo cartInfo = null;
        // 如果购物车有该商品 就添加商品数量
        if (null != cartInfoObj) {
            cartInfo = JSON.parseObject(cartInfoObj.toString(), CartInfo.class);
            cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
            cartInfo.setIsChecked(1);
            cartInfo.setUpdateTime(new Date());
        } else {
            // 如果购物车里没有商品 添加商品
            ProductSku productSku = productFeignClient.getBySkuId(skuId);

            cartInfo = new CartInfo();
            cartInfo.setCartPrice(productSku.getSalePrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setImgUrl(productSku.getThumbImg());
            cartInfo.setSkuName(productSku.getSkuName());
            cartInfo.setIsChecked(1);
            cartInfo.setCreateTime(new Date());
            cartInfo.setUpdateTime(new Date());
        }

        // 将数据添加到redis中
        redisTemplate.opsForHash().put(cartKey, String.valueOf(skuId), JSON.toJSONString(cartInfo));
    }

    @Override
    public List<CartInfo> getCartList() {

        // 获取当前登录的用户信息
        Long userId = AuthContextUtil.getUserInfo().getId();
        String cartKey = this.getCartKey(userId);

        // 获取数据
        List<Object> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            return cartInfoList.stream()
                    .map(cartInfoJSON -> JSON.parseObject(cartInfoJSON.toString(), CartInfo.class))
                    .sorted((o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime()))
                    .toList();
        }

        return new ArrayList<>();
    }

    @Override
    public void deleteCart(Long skuId) {
        // 获取当前登录的用户信息
        Long userId = AuthContextUtil.getUserInfo().getId();
        String cartKey = this.getCartKey(userId);

        redisTemplate.opsForHash().delete(cartKey, String.valueOf(skuId));
    }

    @Override
    public void checkCart(Long skuId, Integer isChecked) {
        // 获取当前登录的用户信息
        Long userId = AuthContextUtil.getUserInfo().getId();
        String cartKey = this.getCartKey(userId);

        Boolean hasKey = redisTemplate.opsForHash().hasKey(cartKey, String.valueOf(skuId));
        if (hasKey) {
            String cartInfoJSON = redisTemplate.opsForHash().get(cartKey, String.valueOf(skuId)).toString();
            CartInfo cartInfo = JSON.parseObject(cartInfoJSON, CartInfo.class);
            cartInfo.setIsChecked(isChecked);
            redisTemplate.opsForHash().put(cartKey, String.valueOf(skuId), JSON.toJSONString(cartInfo));
        }
    }

    @Override
    public void allCheckCart(Integer isChecked) {
        // 获取当前登录的用户信息
        Long userId = AuthContextUtil.getUserInfo().getId();
        String cartKey = this.getCartKey(userId);

        List<Object> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            cartInfoList.stream().map(cartInfoJSON -> {
                CartInfo cartInfo = JSON.parseObject(cartInfoJSON.toString(), CartInfo.class);
                cartInfo.setIsChecked(isChecked);
                return cartInfo;
            }).forEach(cartInfo -> redisTemplate.opsForHash().put(cartKey, String.valueOf(cartInfo.getSkuId()), JSON.toJSONString(cartInfo)));
        }
    }

    @Override
    public void clearCart() {
        // 获取当前登录的用户信息
        Long userId = AuthContextUtil.getUserInfo().getId();
        String cartKey = this.getCartKey(userId);

        redisTemplate.delete(cartKey);
    }

    @Override
    public List<CartInfo> getAllChecked() {
        // 获取当前登录的用户信息
        Long userId = AuthContextUtil.getUserInfo().getId();
        String cartKey = this.getCartKey(userId);

        List<Object> objectList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(objectList)) {
            return objectList.stream().map(cartInfoJSON -> JSON.parseObject(cartInfoJSON.toString(), CartInfo.class))
                    .filter(cartInfo -> cartInfo.getIsChecked() == 1)
                    .toList();
        }

        return new ArrayList<>();
    }

    @Override
    public void deleteChecked() {
        // 获取当前登录的用户信息
        Long userId = AuthContextUtil.getUserInfo().getId();
        String cartKey = this.getCartKey(userId);

        List<Object> objectList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(objectList)) {
            objectList.stream().map(cartInfoJSON -> JSON.parseObject(cartInfoJSON.toString(), CartInfo.class))
                    .filter(cartInfo -> cartInfo.getIsChecked() == 1)
                    .forEach(cartInfo -> redisTemplate.opsForHash().delete(cartKey, String.valueOf(cartInfo.getSkuId())));
        }
    }
}
