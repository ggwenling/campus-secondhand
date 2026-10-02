package com.campus.market.security;

import com.campus.market.common.api.ErrorCode;
import com.campus.market.common.exception.BusinessException;
import com.campus.market.security.annotation.RequireAuth;
import com.campus.market.security.annotation.RequireRole;
import com.campus.market.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * 鉴权拦截器（PRD §9.2 接口层校验）：
 * 1. 请求头携带合法 token 时解析出 LoginUser 放入 UserContext，供"登录可选"接口（如商品详情）读取；
 * 2. 命中 @RequireAuth 强制要求登录，且 refresh token 不能访问业务接口；
 * 3. 命中 @RequireRole 校验后台管理员角色（RBAC）。
 * 未标注注解的接口不做强制校验，因此无需维护放行路径清单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        LoginUser user = resolveUser(request);
        // 前台用户主体：刷新封禁/认证/信用状态（封禁拦截 + 到期自动解封，PRD §4.1/§5.7）
        if (user != null && user.getUserType() == LoginUser.UserType.USER) {
            userService.applyFreshState(user);
        }

        RequireAuth requireAuth = findAnnotation(handlerMethod, RequireAuth.class);
        if (requireAuth != null) {
            if (user == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            if (JwtUtil.TYPE_REFRESH.equals(user.getTokenType())) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }
        }

        RequireRole requireRole = findAnnotation(handlerMethod, RequireRole.class);
        if (requireRole != null) {
            if (user == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            if (user.getUserType() != LoginUser.UserType.ADMIN
                    || !Arrays.asList(requireRole.value()).contains(user.getAdminRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }

        if (user != null) {
            UserContext.set(user);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    /** 解析 Authorization: Bearer xxx；无 token 或解析失败均返回 null（是否拦截由注解决定） */
    private LoginUser resolveUser(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        try {
            return jwtUtil.parse(authorization.substring(BEARER_PREFIX.length()));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("token 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /** 先查方法上的注解，再回退到类上 */
    private <A extends Annotation> A findAnnotation(HandlerMethod handlerMethod, Class<A> type) {
        A annotation = handlerMethod.getMethodAnnotation(type);
        return annotation != null
                ? annotation
                : AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), type);
    }
}
