package com.juzi.aipassagecreator.aop;

import com.juzi.aipassagecreator.annotation.AuthCheck;
import com.juzi.aipassagecreator.enums.UserRoleEnum;
import com.juzi.aipassagecreator.exception.BusinessException;
import com.juzi.aipassagecreator.exception.ErrorCode;
import com.juzi.aipassagecreator.model.entity.User;
import com.juzi.aipassagecreator.model.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        /**
         * 获取当前线程绑定的请求上下文
         *     ↓
         * 按 Servlet 请求上下文处理
         *     ↓
         * 取出 HttpServletRequest
         */
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 已确认登录，不要求特定角色时直接放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        // 必须有这个权限才能通过
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 要求必须有管理员权限，但当前用户没有
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}
