package com.atguigu.spzx.user.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.atguigu.spzx.common.service.exception.GuiguException;
import com.atguigu.spzx.model.dto.h5.UserLoginDto;
import com.atguigu.spzx.model.dto.h5.UserRegisterDto;
import com.atguigu.spzx.model.entity.user.UserInfo;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.model.vo.h5.UserInfoVo;
import com.atguigu.spzx.user.mapper.UserInfoMapper;
import com.atguigu.spzx.user.service.UserInfoService;
import com.atguigu.spzx.util.AuthContextUtil;
import io.minio.Digest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public void register(UserRegisterDto userRegisterDto) {
        // 获取用户信息
        String username = userRegisterDto.getUsername();
        String password = userRegisterDto.getPassword();
        String nickName = userRegisterDto.getNickName();
        String code = userRegisterDto.getCode();

        // 校验参数
        if(StringUtils.isEmpty(username) ||
                StringUtils.isEmpty(password) ||
                StringUtils.isEmpty(nickName) ||
                StringUtils.isEmpty(code)) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 校验验证码
        String codeRedis = redisTemplate.opsForValue().get(username);
        if (!code.equals(codeRedis)) {
            throw new GuiguException(ResultCodeEnum.VALIDATECODE_ERROR);
        }

        // 校验用户名是否重复
        UserInfo userInfo = userInfoMapper.getByUsername(username);
        if(null != userInfo) {
            throw new GuiguException(ResultCodeEnum.USER_NAME_IS_EXISTS);
        }

        // 添加数据到userInfo
        userInfo = new UserInfo();
        userInfo.setUsername(username);
        userInfo.setNickName(nickName);
        userInfo.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        userInfo.setPhone(username);
        userInfo.setStatus(1);
        userInfo.setSex(0);
        userInfo.setAvatar("http://thirdwx.qlogo.cn/mmopen/vi_32/DYAIOgq83eoj0hHXhgJNOTSOFsS4uZs8x1ConecaVOB8eIl115xmJZcT4oCicvia7wMEufibKtTLqiaJeanU2Lpg3w/132");
        userInfoMapper.save(userInfo);

        // 删除Redis中的数据
        redisTemplate.delete(username) ;
    }

    @Override
    public Object login(UserLoginDto userLoginDto) {
        // 从dto中获取用户信息
        String username = userLoginDto.getUsername();
        String password = userLoginDto.getPassword();

        // 从数据库中获取该用户的password信息
        UserInfo userInfo = userInfoMapper.getByUsername(username);
        if (userInfo == null) {
            throw new GuiguException(ResultCodeEnum.LOGIN_ERROR);
        }

        // 判断密码是否相同
        if (!userInfo.getPassword().equals(DigestUtils.md5DigestAsHex(password.getBytes()))) {
            throw new GuiguException(ResultCodeEnum.LOGIN_ERROR);
        }

        // 判断账号是否已停用
        if (userInfo.getStatus() == 0) {
            throw new GuiguException(ResultCodeEnum.ACCOUNT_STOP);
        }

        String token = UUID.randomUUID().toString().replaceAll("-", "");
        redisTemplate.opsForValue().set("user:spzx:" + token, JSON.toJSONString(userInfo), 30, TimeUnit.DAYS);
        return token;
    }

    @Override
    public UserInfoVo getCurrentUserInfo(String token) {
//        String userInfoJSON = redisTemplate.opsForValue().get("user:spzx:" + token);
//        if(StringUtils.isEmpty(userInfoJSON)) {
//            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH) ;
//        }
//        UserInfo userInfo = JSON.parseObject(userInfoJSON, UserInfo.class);
        UserInfo userInfo = AuthContextUtil.getUserInfo();
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo, userInfoVo);
        return userInfoVo;
    }
}
