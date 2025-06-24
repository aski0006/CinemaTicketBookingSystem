package com.asaki0019.cinematicketbookingsystem.aop;

import com.asaki0019.cinematicketbookingsystem.utils.LogSystem;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect
@Component
public class LogAspect {
    // 切入点：controller和service包下所有public方法
    @Pointcut("execution(public * com.asaki0019.cinematicketbookingsystem.controller..*(..)) || " +
            "execution(public * com.asaki0019.cinematicketbookingsystem.services..*(..))")
    public void logPointcut() {
    }

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        Object[] args = joinPoint.getArgs();

        // 记录方法调用
        LogSystem.log(String.format("调用方法: %s.%s, 参数: %s", className, methodName, Arrays.toString(args)));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;

            // 记录方法返回
            LogSystem.log(String.format("方法返回: %s.%s, 返回值: %s, 耗时: %dms", className, methodName, result, cost));
            return result;
        } catch (Throwable ex) {
            // 记录方法异常
            LogSystem.logLevel(LogSystem.LogLevel.ERROR,
                    String.format("方法异常: %s.%s, 异常: %s", className, methodName, ex.getMessage()));
            throw ex;
        }
    }
}