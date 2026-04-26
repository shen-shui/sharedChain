package com.atguigu.spzx.common.service.anno;

import com.atguigu.spzx.common.service.config.UserWebMvcConfiguration;
import com.atguigu.spzx.common.service.interceptor.UserLoginAuthInterceptor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@Import(value = { UserLoginAuthInterceptor.class , UserWebMvcConfiguration.class})
public @interface EnableUserWebMvcConfiguration {
}
