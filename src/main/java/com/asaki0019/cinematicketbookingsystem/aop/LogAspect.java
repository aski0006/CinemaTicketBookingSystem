package com.asaki0019.cinematicketbookingsystem.aop;

import com.asaki0019.cinematicketbookingsystem.utils.LogSystem;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import com.asaki0019.cinematicketbookingsystem.services.OrderServiceImpl;
import com.asaki0019.cinematicketbookingsystem.entities.Order;
import com.asaki0019.cinematicketbookingsystem.entities.PaymentCallbackRequest;
import org.aspectj.lang.annotation.AfterReturning;
import org.springframework.beans.factory.annotation.Autowired;

@Aspect
@Component
public class LogAspect {
    private static final Map<String, Long> sessionLogTimeMap = new ConcurrentHashMap<>();
    private static final long LOG_INTERVAL_MS = 120_000; // 120秒

    @Autowired
    private OrderServiceImpl orderService;

    // 切入点：controller和service包下所有public方法
    @Pointcut("execution(public * com.asaki0019.cinematicketbookingsystem.controller..*(..)) || " +
            "execution(public * com.asaki0019.cinematicketbookingsystem.services..*(..))")
    public void logPointcut() {
    }

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method.isAnnotationPresent(NotLogInAOP.class)) {
            return joinPoint.proceed();
        }
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getName();
        Object[] args = joinPoint.getArgs();

        boolean shouldLog = true;
        // 只对PaymentController做节流，并剔除前端订单状态轮询接口日志
        if (className.endsWith("PaymentController")) {
            String sessionId = null;
            String requestUri = null;
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest req) {
                    sessionId = req.getSession().getId();
                    requestUri = req.getRequestURI();
                    break;
                }
            }
            // 剔除订单状态轮询接口日志
            if (requestUri != null && requestUri.matches(".*/api/payments/query/.*")) {
                shouldLog = false;
            } else if (sessionId != null) {
                long now = System.currentTimeMillis();
                Long last = sessionLogTimeMap.get(sessionId);
                if (last == null || now - last > LOG_INTERVAL_MS) {
                    sessionLogTimeMap.put(sessionId, now);
                    shouldLog = true;
                } else {
                    shouldLog = false;
                }
            }
        }
        if (shouldLog) {
            LogSystem.log(String.format("调用方法: %s.%s, 参数: %s", className, methodName, Arrays.toString(args)));
        }

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            if (shouldLog) {
                LogSystem.log(String.format("方法返回: %s.%s, 返回值: %s, 耗时: %dms", className, methodName, result, cost));
            }
            return result;
        } catch (Throwable ex) {
            if (shouldLog) {
                LogSystem.logLevel(LogSystem.LogLevel.ERROR,
                        String.format("方法异常: %s.%s, 异常: %s", className, methodName, ex.getMessage()));
            }
            throw ex;
        }
    }

    @Around("execution(* com.asaki0019.cinematicketbookingsystem..*.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 修正注解名和类型
        if (method.isAnnotationPresent(NotLogInAOP.class)) {
            System.out.println(method.getName() + " 被跳过AOP日志记录");
            return joinPoint.proceed();
        }
        // 原有日志逻辑
        Object result;
        long start = System.currentTimeMillis();
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long end = System.currentTimeMillis();
            // 这里可写入日志实现
            System.out.println("[AOP LOG] " + method.getName() + " 执行耗时: " + (end - start) + "ms");
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NotLogInAOP {
    }

    // 新增：支付回调后自动更新OrderSeat状态
    @AfterReturning(pointcut = "execution(* com.asaki0019.cinematicketbookingsystem.services.OrderServiceImpl.processPaymentCallback(..)) && args(callbackRequest)", argNames = "callbackRequest")
    public void afterProcessPaymentCallback(PaymentCallbackRequest callbackRequest) {
        try {
            Order order = orderService.getOrderRepository().findByOrderNo(callbackRequest.getOrderNo());
            if (order != null) {
                orderService.updateOrderSeatStatusAndLog(order, callbackRequest.getPaymentStatus());
            }
        } catch (Exception e) {
            LogSystem.error("[AOP afterProcessPaymentCallback] 自动更新OrderSeat状态异常: " + e.getMessage());
        }
    }
}