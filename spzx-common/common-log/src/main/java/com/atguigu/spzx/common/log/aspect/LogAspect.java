package com.atguigu.spzx.common.log.aspect;

import com.atguigu.spzx.common.log.annotation.Log;
import com.atguigu.spzx.common.log.service.AsyncOperLogService;
import com.atguigu.spzx.common.log.utils.LogUtil;
import com.atguigu.spzx.model.entity.system.SysOperLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LogAspect {

    @Autowired
    private AsyncOperLogService asyncOperLogService;

    @Around(value = "@annotation(sysLog)")
    public Object doAroundAdvice(ProceedingJoinPoint joinPoint, Log sysLog) {

        // 方法执行前操作
        SysOperLog sysOperLog = new SysOperLog();
        LogUtil.beforeHandleLog(sysLog, joinPoint, sysOperLog);

        Object proceed = null;
        try {
            proceed = joinPoint.proceed();

            // 方法执行后操作
            LogUtil.afterHandlLog(sysLog, proceed, sysOperLog, 0, null);
        } catch (Throwable e) {
            LogUtil.afterHandlLog(sysLog, proceed, sysOperLog, 1, e.getMessage());
            throw new RuntimeException();
        }

        // 保存日志数据
        asyncOperLogService.saveSysOperLog(sysOperLog);

        return proceed;
    }
}
