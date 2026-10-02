package com.campus.market.service;

import com.campus.market.common.api.PageResult;
import com.campus.market.entity.User;
import com.campus.market.security.LoginUser;

/**
 * 管理后台用户服务（PRD ADM-05 用户管理：查询/封禁/解封/信用调整，SUPER+OPERATOR）。
 * 封禁走条件 UPDATE（幂等）+ 通知；信用调整复用 CreditService（ADMIN_ADJUST，不走幂等键）。
 */
public interface AdminUserService {

    /** 用户分页（username 模糊可选） */
    PageResult<User> page(String username, long pageNum, long pageSize);

    /** 用户详情 */
    User detail(Long userId);

    /**
     * 封禁用户（PRD ADM-05）：reason 必填；durationDays 非空则限期封禁，空则永久；
     * 条件 UPDATE（已封禁 rows=0 报 40919），同事务通知用户。
     */
    void ban(Long userId, String reason, Integer durationDays, LoginUser operator);

    /** 解封用户：清除封禁字段 + 通知；未封禁幂等跳过 */
    void unban(Long userId, LoginUser operator);

    /** 手动调分（PRD §5.7 ADMIN_ADJUST）：change 正加负减，remark 必填 */
    void adjustCredit(Long userId, int change, String remark, LoginUser operator);
}
