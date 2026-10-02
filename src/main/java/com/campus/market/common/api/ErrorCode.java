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
    SYSTEM_ERROR(50000, "系统繁忙，请稍后再试"),

    // ==================== GOODS 商品段（M2 商品中心，追加式维护） ====================
    /** PRD GDS-01：标题/描述等文本命中敏感词，message 附命中词列表 */
    GOODS_SENSITIVE(40001, "内容包含敏感词"),
    /** PRD GDS-01：商品图片数量不满足 1~9 张约束 */
    GOODS_IMAGE_LIMIT(40002, "商品图片数量须为 1~9 张"),
    /** PRD GDS-01/02：商品字段级参数不合法（价格/成色/分类等） */
    GOODS_PARAM_INVALID(40003, "商品参数不合法"),
    /** PRD GDS-02：无权操作他人商品 */
    GOODS_FORBIDDEN(40303, "无权操作该商品"),
    /** PRD GDS-05：商品不存在或已删除 */
    GOODS_NOT_FOUND(40401, "商品不存在或已删除"),
    /** PRD §5.1：商品处于 IN_TRANSACTION 等不可操作状态 */
    GOODS_NOT_ON_SALE(40901, "商品当前不可交易"),

    // ==================== AUTH 认证段（M1 用户与校园认证，追加式维护） ====================
    /** PRD USR-01/02：用户名或密码错误（不区分二者，防撞库） */
    AUTH_CREDENTIALS_INVALID(40006, "用户名或密码错误"),
    /** PRD §5.1：校园邮箱域名不在白名单 */
    AUTH_EMAIL_DOMAIN_FORBIDDEN(40007, "仅支持校园邮箱认证"),
    /** PRD USR-03：验证码错误或已过期 */
    AUTH_CODE_INVALID(40008, "验证码错误或已过期"),
    /** PRD USR-01：注册用户名重复 */
    AUTH_USERNAME_DUP(40902, "用户名已被占用"),
    /** PRD USR-03：学号已被其他账号认证（一学号一账号） */
    AUTH_STUDENT_NO_DUP(40903, "该学号已完成认证"),
    /** PRD USR-03：校园邮箱已被其他账号认证 */
    AUTH_CAMPUS_EMAIL_DUP(40904, "该校园邮箱已被认证"),
    /** PRD USR-02：登录失败 5 次锁定 10 分钟 */
    AUTH_LOGIN_LOCKED(40102, "登录失败次数过多，请 10 分钟后再试"),
    /** PRD §4.1：发布/下单等操作要求先完成校园认证（M2 发布接口已按此名引用） */
    AUTH_NOT_CERTIFIED(40304, "请先完成校园认证"),
    /** PRD §5.1：验证码发送限频（1 分钟内不可重发 / 每日 10 次） */
    AUTH_CODE_LIMIT(42901, "验证码发送过于频繁，请稍后再试"),
    /** PRD USR-03：该账号已完成校园认证，无需重复认证 */
    AUTH_ALREADY_CERTIFIED(40905, "该账号已完成校园认证"),

    // ==================== CHAT 聊天段（M4 即时消息，追加式维护） ====================
    /** PRD CHT-01/02：会话不存在（含未创建场景） */
    CHAT_CONV_NOT_FOUND(40403, "会话不存在"),
    /** PRD CHT-02：非会话双方访问会话消息 */
    CHAT_FORBIDDEN(40306, "无权访问该会话"),
    /** PRD CHT-02/06：消息内容/类型不合法（空、超 500 字、类型未知、给自己发消息等） */
    CHAT_MESSAGE_INVALID(40010, "消息内容不合法"),

    // ==================== ORDER/CRD 订单与信用段（M3 交易与信用，追加式维护） ====================
    /** PRD ORD-05：订单不存在 */
    ORDER_NOT_FOUND(40402, "订单不存在"),
    /** PRD ORD-05/06：仅订单双方可查看或操作 */
    ORDER_FORBIDDEN(40305, "无权访问该订单"),
    /** PRD §5.2：订单当前状态不允许该操作（重复确认/重复取消/重复完成等） */
    ORDER_STATE_INVALID(40906, "订单当前状态不允许该操作"),
    /** PRD ORD-01：商品不在 ON_SALE，无法下单（含并发下单锁失败） */
    ORDER_GOODS_NOT_AVAILABLE(40907, "商品当前不可下单"),
    /** PRD ORD-06：每单每人只能评价一次（uk_order_reviewer 兜底） */
    ORDER_DUPLICATE_REVIEW(40908, "该订单已评价过，请勿重复评价"),
    /** PRD ORD-06：仅订单完成（COMPLETED）后 7 天内可评价 */
    REVIEW_WINDOW_CLOSED(40009, "已过评价期（完成后 7 天内有效）");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
