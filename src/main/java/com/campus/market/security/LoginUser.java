package com.campus.market.security;

import lombok.Getter;
import lombok.Setter;

/**
 * 当前登录主体（前台用户或后台管理员），由 JWT 解析而来
 */
@Getter
@Setter
public class LoginUser {

    public enum UserType { USER, ADMIN }

    private Long userId;
    private String username;
    private UserType userType;
    /** 管理员角色：super / auditor / operator，仅后台主体有值（PRD §4.2） */
    private String adminRole;
    /** token 类型：access / refresh，refresh 不能用于访问业务接口 */
    private String tokenType;
}
