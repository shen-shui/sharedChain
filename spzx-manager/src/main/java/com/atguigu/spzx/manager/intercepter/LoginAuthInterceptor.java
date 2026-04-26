package com.atguigu.spzx.manager.intercepter;


import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.atguigu.spzx.manager.properties.UserProperties;
import com.atguigu.spzx.model.entity.system.SysUser;
import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.util.AuthContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

@Component
public class LoginAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求方式
        String method = request.getMethod();
        // 如果是预检请求 直接放行
        if ("OPTIONS".equals(method)) {
            return true;
        }
        // 从request中获取token
        String token = request.getHeader("token");
        // 如果token为空 报错 不放行
        if (StrUtil.isEmpty(token)) {
            responseNoLoginInfo(response);
            return false;
        }

        // token不为空 检验token的合法性
        String sysInfoJson = redisTemplate.opsForValue().get("user:login:" + token);
        if (StrUtil.isEmpty(sysInfoJson)) {
            responseNoLoginInfo(response);
            return false;
        }

        // 将用户信息存储到threadLocal中
        SysUser sysUser = JSON.parseObject(sysInfoJson, SysUser.class);
        AuthContextUtil.set(sysUser);

        // 重置redis中的有效时间
        redisTemplate.expire("user:login:" + token, 30, TimeUnit.MINUTES);

        // 放行
        return true;
    }

    //响应208状态码给前端
    private void responseNoLoginInfo(HttpServletResponse response) {
        Result<Object> result = Result.build(null, ResultCodeEnum.LOGIN_AUTH);
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        try {
            writer = response.getWriter();
            writer.print(JSON.toJSONString(result));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) writer.close();
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        AuthContextUtil.remove();
    }
}
