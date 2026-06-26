package com.sub2.monitor.config.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Order(-1)
@Component
public class DataSourceAspect {

    @Around("@annotation(com.sub2.monitor.config.datasource.UseDataSource) || @within(com.sub2.monitor.config.datasource.UseDataSource)")
    public Object switchDataSource(ProceedingJoinPoint joinPoint) throws Throwable {
        UseDataSource useDataSource = findUseDataSource(joinPoint);
        if (useDataSource == null) {
            return joinPoint.proceed();
        }

        DataSourceContextHolder.use(useDataSource.value());
        try {
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    private UseDataSource findUseDataSource(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        UseDataSource methodAnnotation = AnnotationUtils.findAnnotation(method, UseDataSource.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), UseDataSource.class);
    }
}
