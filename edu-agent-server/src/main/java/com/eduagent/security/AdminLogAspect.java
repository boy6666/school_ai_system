package com.eduagent.security;

import com.eduagent.entity.AdminLog;
import com.eduagent.repository.AdminLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class AdminLogAspect {

    private final AdminLogRepository adminLogRepository;

    public AdminLogAspect(AdminLogRepository adminLogRepository) {
        this.adminLogRepository = adminLogRepository;
    }

    @Around("@annotation(com.eduagent.security.AdminOperation)")
    public Object logAdminOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AdminOperation annotation = method.getAnnotation(AdminOperation.class);

        Object result = joinPoint.proceed();

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long adminId = auth != null ? (Long) auth.getPrincipal() : null;

            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String ip = "";
            String ua = "";
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ip = request.getRemoteAddr();
                ua = request.getHeader("User-Agent");
            }

            AdminLog log = new AdminLog();
            log.setAdminId(adminId);
            log.setAction(annotation.value());
            log.setTargetType(annotation.targetType());
            log.setDescription(annotation.value());
            log.setIpAddress(ip);
            log.setUserAgent(ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua);
            adminLogRepository.save(log);
        } catch (Exception ignored) {
            // 日志记录失败不影响业务
        }

        return result;
    }
}
