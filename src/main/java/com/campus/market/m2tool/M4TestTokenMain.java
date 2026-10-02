package com.campus.market.m2tool;

import com.campus.market.config.JwtProperties;
import com.campus.market.security.JwtUtil;
import com.campus.market.security.LoginUser;

/**
 * 【临时工具，验证完成后删除】M4 模块联调用：绕过登录接口直接为指定用户签发 access token。
 * 用法：java -cp target/classes;... com.campus.market.m2tool.M4TestTokenMain <userId> <username>
 * （Windows 分隔符为分号；dev 密钥与 application-dev.yml 一致，仅供本机联调）
 */
public final class M4TestTokenMain {

    private M4TestTokenMain() {
    }

    public static void main(String[] args) {
        long userId = Long.parseLong(args[0]);
        String username = args.length > 1 ? args[1] : ("__m4test__" + userId);

        JwtProperties properties = new JwtProperties();
        // 与 application-dev.yml 默认演示密钥一致（dev-only）
        properties.setSecret("dev-only-secret-key-please-override-0123456789");

        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setUserType(LoginUser.UserType.USER);
        user.setTokenType(JwtUtil.TYPE_ACCESS);

        System.out.println(new JwtUtil(properties).createAccessToken(user));
    }
}
