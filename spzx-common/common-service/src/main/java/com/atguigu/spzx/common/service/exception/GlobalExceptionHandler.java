package com.atguigu.spzx.common.service.exception;

import com.atguigu.spzx.model.vo.common.Result;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result error(Exception e){
        e.printStackTrace();
        return Result.build(null, ResultCodeEnum.SYSTEM_ERROR);
    }

    @ExceptionHandler(GuiguException.class)
    @ResponseBody
    public Result error(GuiguException e){
        return Result.build(null, e.getResultCodeEnum());
    }

}
