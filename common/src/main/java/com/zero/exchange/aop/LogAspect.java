package com.zero.exchange.aop;

import com.zero.exchange.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class LogAspect {

    private final Logger log = LoggerFactory.getLogger(LogAspect.class);

    @Before("execution(public * com.zero.exchange.*.controller.*.*(..))")
    public void doInBefore(JoinPoint joinPoint) {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String url = request.getRequestURI();
        String method = request.getMethod();
        Object[] params = joinPoint.getArgs();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String functionName = methodSignature.getName();
        String[] paramNames = methodSignature.getParameterNames();
        StringBuilder paramBuilder = new StringBuilder();
        for (int i = 0; i < paramNames.length; i++) {
            if (i >= params.length) {
                break;
            }
            Object param = params[i];
            String name = paramNames[i];
            if ("request".equals(name) || "response".equals(name)) {
                continue;
            }
            paramBuilder.append(name).append("=").append(param.toString()).append(", ");
        }
        if (paramBuilder.length() > 0) {
            paramBuilder.replace(paramBuilder.lastIndexOf(", "), paramBuilder.length(), "");
        }
        String logStart = functionName + ": " + method + " ----> ";
        System.out.println();
        log.info(logStart + "url: " + url);
        String padding = new String(new char[logStart.length()]).replace('\0', ' ');
        log.info(padding + "params: {" + paramBuilder + "}");
    }

    @Around("execution(public * com.zero.exchange.*.controller.*.*(..))")
    public Object doInAfter(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String method = request.getMethod();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String functionName = methodSignature.getName();
        Object result = joinPoint.proceed();
        String logStart = functionName + ": " + method + " <---- ";
        log.info(logStart + "response: " + JsonUtil.writeJson(result));
        return result;
    }
}
