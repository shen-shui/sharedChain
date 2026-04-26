package com.atguigu.spzx.manager.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.atguigu.spzx.common.service.exception.GuiguException;
import com.atguigu.spzx.manager.mapper.SysRoleUserMapper;
import com.atguigu.spzx.manager.mapper.SysUserMapper;
import com.atguigu.spzx.manager.service.SysUserService;
import com.atguigu.spzx.model.dto.system.AssginRoleDto;
import com.atguigu.spzx.model.dto.system.LoginDto;
import com.atguigu.spzx.model.dto.system.SysUserDto;
import com.atguigu.spzx.model.entity.system.SysUser;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.model.vo.system.LoginVo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.core.util.Json;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysRoleUserMapper sysRoleUserMapper;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 用户登录
    @Override
    public LoginVo login(LoginDto loginDto) {
        String captcha = loginDto.getCaptcha();
        String codeKey = loginDto.getCodeKey();

        String value = redisTemplate.opsForValue().get("user:validate" + codeKey);

        if (StrUtil.isEmpty(value) || !StrUtil.equalsIgnoreCase(value, captcha)) {
            throw new GuiguException(ResultCodeEnum.VALIDATECODE_ERROR);
        }

        redisTemplate.delete("user:validate" + codeKey);

        // 1 获取提交用户名
        String userName = loginDto.getUserName();

        // 2 根据用户名查询数据库sys_user表
        SysUser sysUser = sysUserMapper.selectUserInfoByUserName(userName);

        // 3 如果根据用户名查不到信息 用户不存在 报错
        if (sysUser == null) {
//            throw new RuntimeException("用户名不存在");
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 4 查询到用户信息 获取输入的密码和数据库的密码进行比较
        String database_password = sysUser.getPassword();
        String input_password = DigestUtils.md5DigestAsHex(loginDto.getPassword().getBytes());
        if (!input_password.equals(database_password)) {
//            throw new RuntimeException("密码不正确");
            throw new GuiguException(ResultCodeEnum.LOGIN_ERROR);
        }

        // 5 如果密码相同 登录成功 并生成token
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        // 6 把登录成功的信息放到 redis 中
        redisTemplate.opsForValue()
                .set("user:login:"+token,
                        JSON.toJSONString(sysUser),
                        7,
                        TimeUnit.DAYS);

        // 7 返回loginVo对象
        LoginVo loginVo = new LoginVo();
        loginVo.setToken(token);
        return loginVo;
    }

    @Override
    public SysUser getUserInfo(String token) {
        String userJson = redisTemplate.opsForValue().get("user:login:" + token);
        return JSON.parseObject(userJson, SysUser.class);
    }

    @Override
    public void logout(String token) {
        redisTemplate.delete("user:login:"+token);
    }

    @Override
    public PageInfo<SysUser> findByPage(Integer current, Integer limit, SysUserDto sysUserDto) {
        PageHelper.startPage(current, limit);
        List<SysUser> list = sysUserMapper.findByPage(sysUserDto);
        PageInfo<SysUser> pageInfo = new PageInfo<>(list);
        return pageInfo;
    }

    @Override
    public void saveSysUser(SysUser sysUser) {
        // 1 用户名不能重复
        String userName = sysUser.getUserName();
        SysUser dbSysUser = sysUserMapper.selectUserInfoByUserName(userName);
        if (dbSysUser != null) {
            throw new GuiguException(ResultCodeEnum.USER_NAME_IS_EXISTS);
        }

        // 2 密码要进行加密
        String password = DigestUtils.md5DigestAsHex(sysUser.getPassword().getBytes());
        sysUser.setPassword(password);

        sysUser.setStatus(1);

        sysUserMapper.saveSysUser(sysUser);
    }

    @Override
    public void updateSysUser(SysUser sysUser) {
        sysUserMapper.updateSysUser(sysUser);
    }

    @Override
    public void deleteById(Long userId) {
        sysUserMapper.deleteById(userId);
    }

    @Override
    public void doAssign(AssginRoleDto assignRoleDto) {
        // 删除该用户的所有角色数据
        sysRoleUserMapper.deleteByUserId(assignRoleDto.getUserId());

        // 添加新角色数据
        sysRoleUserMapper.doAssignBatch(assignRoleDto);
    }
}
