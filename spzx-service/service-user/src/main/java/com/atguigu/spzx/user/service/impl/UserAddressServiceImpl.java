package com.atguigu.spzx.user.service.impl;

import com.atguigu.spzx.model.entity.user.UserAddress;
import com.atguigu.spzx.user.mapper.UserAddressMapper;
import com.atguigu.spzx.user.service.UserAddressService;
import com.atguigu.spzx.util.AuthContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAddressServiceImpl implements UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddress> findUserAddressList() {
        Long id = AuthContextUtil.getUserInfo().getId();

        return userAddressMapper.findByUserId(id);
    }

    @Override
    public UserAddress getById(Long id) {
        return userAddressMapper.getById(id);
    }
}
