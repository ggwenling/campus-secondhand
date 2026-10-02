package com.campus.market.controller;

import com.campus.market.common.api.PageResult;
import com.campus.market.common.api.Result;
import com.campus.market.entity.OperationLog;
import com.campus.market.security.annotation.RequireRole;
import com.campus.market.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询（PRD ADM-09，SUPER 专属查看；记录由 AOP 自动完成）。
 */
@Tag(name = "管理后台-操作日志（SUPER）")
@Validated
@RestController
@RequestMapping("/api/admin/operation-logs")
@RequiredArgsConstructor
@RequireRole("super")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "操作日志分页（adminId/action 可选过滤）")
    @GetMapping
    public Result<PageResult<OperationLog>> page(
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return Result.ok(operationLogService.page(adminId, action, pageNum, pageSize));
    }
}
