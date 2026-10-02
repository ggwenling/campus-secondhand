package com.campus.market.m2tool;

import com.campus.market.config.JwtProperties;
import com.campus.market.security.JwtUtil;
import com.campus.market.security.LoginUser;

/**
 * 【临时工具，验证完成后删除】M2 模块联调用：绕过登录接口直接为指定用户签发 access token。
 * 仅开发/演示使用：M1 登录接口尚未落地时的测试通路（M2 验证要求）。
 * 用法：java -cp target/classes... com.campus.market.m2tool.M2TestTokenMain <userId> <username>
 */
public final class M2TestTokenMain {

    private M2TestTokenMain() {
    }

    public static void main(String[] args) {
        long userId = Long.parseLong(args[0]);
        String username = args.length > 1 ? args[1] : ("__m2test__" + userId);

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
