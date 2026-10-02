package com.campus.market.security;

import com.campus.market.common.LoginUserTestFactory;
import com.campus.market.common.api.ErrorCode;
import com.campus.market.common.exception.BusinessException;
import com.campus.market.security.annotation.RequireAuth;
import com.campus.market.security.annotation.RequireRole;
import com.campus.market.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * AuthInterceptor 深测（PRD §9.2 / §4.2 RBAC 权限矩阵、token 类型口径、UserContext 注入）。
 * M6 管理后台复用本拦截器：@RequireRole 命中/未命中即后台权限矩阵语义。
 */
@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock JwtUtil jwtUtil;
    @Mock UserService userService;

    private AuthInterceptor interceptor;
    private MockHttpServletRequest request;
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        interceptor = new AuthInterceptor(jwtUtil, userService);
        request = new MockHttpServletRequest();
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    // ==================== 注解缺失场景 ====================

    @Test
    void nonHandlerMethod_passesThrough() {
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void noAnnotation_anonymousPasses_andDoesNotTouchUserService() throws Exception {
        assertThat(interceptor.preHandle(request, response, handler("noAnnotation"))).isTrue();
        verify(userService, never()).applyFreshState(any());
        assertThat(UserContext.get()).isNull();
    }

    // ==================== @RequireAuth ====================

    @Test
    void requireAuth_withoutToken_unauthorized() throws Exception {
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("requireAuth")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void requireAuth_refreshToken_rejected() throws Exception {
        stubToken(LoginUserTestFactory.user(1L, 1, 100), JwtUtil.TYPE_REFRESH);
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("requireAuth")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    @Test
    void requireAuth_validAccessToken_setsContext_andRefreshesState() throws Exception {
        LoginUser user = LoginUserTestFactory.user(1L, 1, 100);
        stubToken(user, JwtUtil.TYPE_ACCESS);

        assertThat(interceptor.preHandle(request, response, handler("requireAuth"))).isTrue();
        assertThat(UserContext.get()).isSameAs(user);
        verify(userService).applyFreshState(user);   // 前台主体刷新封禁/认证状态（PRD §4.1）
    }

    // ==================== @RequireRole（RBAC，M6 权限矩阵基础） ====================

    @Test
    void requireRole_anonymous_unauthorized() throws Exception {
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("requireSuper")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void requireRole_frontUser_forbidden() throws Exception {
        stubToken(LoginUserTestFactory.user(1L, 1, 100), JwtUtil.TYPE_ACCESS);
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("requireSuper")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void requireRole_adminRoleMismatch_forbidden() throws Exception {
        stubToken(LoginUserTestFactory.admin("operator"), JwtUtil.TYPE_ACCESS);
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("requireSuper")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void requireRole_adminRoleMatch_passes_withoutUserStateRefresh() throws Exception {
        LoginUser admin = LoginUserTestFactory.admin("super");
        stubToken(admin, JwtUtil.TYPE_ACCESS);

        assertThatCode(() -> assertThat(interceptor
                .preHandle(request, response, handler("requireSuper"))).isTrue())
                .doesNotThrowAnyException();
        assertThat(UserContext.get()).isSameAs(admin);
        // ADMIN 主体不触发前台 applyFreshState
        verify(userService, never()).applyFreshState(any());
    }

    @Test
    void requireRole_multiRoles_anyMatchPasses() throws Exception {
        stubToken(LoginUserTestFactory.admin("auditor"), JwtUtil.TYPE_ACCESS);
        assertThat(interceptor.preHandle(request, response, handler("requireContent"))).isTrue();
    }

    @Test
    void invalidToken_treatedAsAnonymous() throws Exception {
        request.addHeader("Authorization", "Bearer garbage.token.here");
        lenient().when(jwtUtil.parse(any())).thenThrow(new io.jsonwebtoken.JwtException("bad"));
        // 公开接口：解析失败按游客放行
        assertThat(interceptor.preHandle(request, response, handler("noAnnotation"))).isTrue();
        // 受保护接口：401
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("requireAuth")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    // ==================== 辅助 ====================

    private void stubToken(LoginUser user, String tokenType) {
        user.setTokenType(tokenType);
        request.addHeader("Authorization", "Bearer valid-token");
        lenient().when(jwtUtil.parse("valid-token")).thenReturn(user);
    }

    private HandlerMethod handler(String methodName) throws Exception {
        Method method = SampleController.class.getMethod(methodName);
        return new HandlerMethod(new SampleController(), method);
    }

    /** 注解样例控制器（拦截器只读取注解，不真正调用） */
    static class SampleController {
        public void noAnnotation() { }

        @RequireAuth
        public void requireAuth() { }

        @RequireRole("super")
        public void requireSuper() { }

        @RequireRole({"super", "auditor"})
        public void requireContent() { }
    }
}
