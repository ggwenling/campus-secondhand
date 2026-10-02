package com.campus.market.controller;

import com.campus.market.common.api.ErrorCode;
import com.campus.market.common.api.PageResult;
import com.campus.market.common.api.Result;
import com.campus.market.common.exception.BusinessException;
import com.campus.market.entity.User;
import com.campus.market.security.LoginUser;
import com.campus.market.security.UserContext;
import com.campus.market.security.annotation.OperationLog;
import com.campus.market.security.annotation.RequireRole;
import com.campus.market.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理（PRD ADM-05，SUPER+OPERATOR）：分页/详情/封禁/解封/信用调整。
 * 审计由方法内显式记录（含理由摘要，比 AOP 参数捕获更精确）。
 */
@Tag(name = "管理后台-用户管理（SUPER/OPERATOR）")
@Validated
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@RequireRole({"super", "operator"})
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户分页（username 模糊可选）")
    @GetMapping
    public Result<PageResult<User>> page(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return Result.ok(adminUserService.page(username, pageNum, pageSize));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public Result<User> detail(@PathVariable Long id) {
        return Result.ok(adminUserService.detail(id));
    }

    @Operation(summary = "封禁用户（reason 必填；durationDays 空为永久）")
    @PostMapping("/{id}/ban")
    public Result<Void> ban(@PathVariable Long id, @RequestBody BanBody body) {
        adminUserService.ban(id, body.getReason(), body.getDurationDays(), requireAdmin());
        return Result.ok();
    }

    @Operation(summary = "解封用户")
    @PostMapping("/{id}/unban")
    public Result<Void> unban(@PathVariable Long id) {
        adminUserService.unban(id, requireAdmin());
        return Result.ok();
    }

    @Operation(summary = "手动调整信用分（ADMIN_ADJUST，change 正加负减，remark 必填）")
    @PostMapping("/{id}/credit")
    public Result<Void> adjustCredit(@PathVariable Long id, @RequestBody CreditBody body) {
        adminUserService.adjustCredit(id, body.getChange(), body.getRemark(), requireAdmin());
        return Result.ok();
    }

    private LoginUser requireAdmin() {
        LoginUser user = UserContext.requireLogin();
        if (user.getUserType() != LoginUser.UserType.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return user;
    }

    /** 封禁请求体 */
    @Data
    public static class BanBody {
        private String reason;
        private Integer durationDays;
    }

    /** 调分请求体 */
    @Data
    public static class CreditBody {
        private Integer change;
        private String remark;
    }
}
