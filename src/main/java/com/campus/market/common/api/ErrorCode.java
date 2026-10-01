package com.campus.market.common.api;

import lombok.Getter;

/**
 * 全局业务状态码。分段约定：0 成功；400xx 参数；401xx 登录态；403xx 权限；
 * 404xx 不存在；409xx 冲突；429xx 限流；500xx 系统。各模块细分错误码在此追加。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    PARAM_ERROR(40000, "参数错误"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    TOKEN_INVALID(40101, "登录凭证无效，请重新登录"),
    FORBIDDEN(40300, "无权限执行该操作"),
    ACCOUNT_BANNED(40301, "账号已被封禁"),
    ACCOUNT_RESTRICTED(40302, "信用分受限，暂无法执行该操作"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "数据冲突，请刷新后重试"),
    TOO_MANY_REQUESTS(42900, "操作过于频繁，请稍后再试"),
    SYSTEM_ERROR(50000, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
