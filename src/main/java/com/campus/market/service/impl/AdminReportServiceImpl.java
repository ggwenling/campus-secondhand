package com.campus.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.api.ErrorCode;
import com.campus.market.common.api.PageResult;
import com.campus.market.common.exception.BusinessException;
import com.campus.market.entity.CreditLog;
import com.campus.market.entity.Goods;
import com.campus.market.entity.Notification;
import com.campus.market.entity.OperationLog;
import com.campus.market.entity.Report;
import com.campus.market.entity.SwapPost;
import com.campus.market.entity.WantPost;
import com.campus.market.mapper.GoodsMapper;
import com.campus.market.mapper.NotificationMapper;
import com.campus.market.mapper.ReportMapper;
import com.campus.market.mapper.SwapPostMapper;
import com.campus.market.mapper.WantPostMapper;
import com.campus.market.security.LoginUser;
import com.campus.market.service.AdminContentService;
import com.campus.market.service.AdminReportService;
import com.campus.market.service.AdminUserService;
import com.campus.market.service.CreditService;
import com.campus.market.service.OperationLogService;
import com.campus.market.service.ReportService;
import com.campus.market.vo.AdminContentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 举报工单处置实现（PRD ADM-04 / §6.2 / 数据库设计文档 §3.21）。
 * 一致性要点：
 * - 状态机 0待处理→1已处置/2已驳回，markHandled 内部条件更新防重复（40918）；
 * - 处置组合一次调用按序执行：下架→扣分→封号→警告，任何一步失败整体回滚（事务）；
 * - 扣分为标准原因 REPORT_VALID（-violationReportPenalty），幂等键 ref=REPORT/reportId；
 * - 双方通知：举报人获处置结果，被举报人获警告/封禁告知（扣分/封禁动作自身已带通知的不再重复）；
 * - 工单处置本身写 operation_log（ACTION_REPORT_HANDLE），委托动作的日志由各自服务记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private static final Set<String> VALID_ACTIONS =
            Set.of("TAKE_DOWN", "WARN", "DEDUCT", "BAN");

    private final ReportMapper reportMapper;
    private final GoodsMapper goodsMapper;
    private final WantPostMapper wantPostMapper;
    private final SwapPostMapper swapPostMapper;
    private final NotificationMapper notificationMapper;
    private final ReportService reportService;
    private final AdminContentService adminContentService;
    private final AdminUserService adminUserService;
    private final CreditService creditService;
    private final OperationLogService operationLogService;

    @Override
    public PageResult<Report> page(Integer status, long pageNum, long pageSize) {
        if (status != null && status != Report.STATUS_PENDING
                && status != Report.STATUS_HANDLED && status != Report.STATUS_REJECTED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "status 仅支持 0/1/2");
        }
        pageSize = Math.min(Math.max(pageSize, 1), 100);
        pageNum = Math.max(pageNum, 1);
        IPage<Report> result = reportMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Report>()
                        .eq(status != null, Report::getStatus, status)
                        .orderByAsc(Report::getStatus)         // 待处理优先
                        .orderByDesc(Report::getCreatedAt));
        List<Report> records = result.getRecords();
        PageResult<Report> page = new PageResult<>();
        page.setList(records);
        page.setTotal(result.getTotal());
        page.setPageNum(result.getCurrent());
        page.setPageSize(result.getSize());
        return page;
    }

    @Override
    public Report detail(Long id) {
        return reportService.requireById(id);
    }

    @Override
    @Transactional
    public void handle(Long id, List<String> actions, String result, LoginUser operator) {
        Report report = requirePending(id);
        if (!StringUtils.hasText(result)) {
            throw new BusinessException(ErrorCode.ADM_PARAM_INVALID, "处置结果说明必填");
        }
        if (actions != null && !actions.isEmpty()) {
            for (String action : actions) {
                if (!VALID_ACTIONS.contains(action)) {
                    throw new BusinessException(ErrorCode.ADM_PARAM_INVALID, "未知处置动作：" + action);
                }
            }
            Long ownerId = resolveTargetOwner(report);
            for (String action : actions) {
                applyAction(report, action, ownerId, result, operator);
            }
        }
        reportService.markHandled(id, operator.getUserId(), result.trim(), Report.STATUS_HANDLED);

        operationLogService.record(operator.getUserId(), operator.getUsername(),
                OperationLog.ACTION_REPORT_HANDLE, "REPORT", id,
                "处置举报：" + (actions == null ? "无动作" : String.join("+", actions)) + "；" + result, null);
        // 举报人通知（处置结果）
        notify(report.getReporterId(), String.format(
                "您的举报（%s #%d）已处置：%s", report.getTargetType(), report.getTargetId(), result.trim()));
        log.info("举报处置完成：reportId={}, actions={}, by={}", id, actions, operator.getUsername());
    }

    @Override
    @Transactional
    public void reject(Long id, String result, LoginUser operator) {
        Report report = requirePending(id);
        if (!StringUtils.hasText(result)) {
            throw new BusinessException(ErrorCode.ADM_PARAM_INVALID, "驳回理由必填");
        }
        reportService.markHandled(id, operator.getUserId(), result.trim(), Report.STATUS_REJECTED);
        operationLogService.record(operator.getUserId(), operator.getUsername(),
                OperationLog.ACTION_REPORT_HANDLE, "REPORT", id, "驳回举报：" + result, null);
        notify(report.getReporterId(), String.format(
                "您的举报（%s #%d）经核查未成立：%s", report.getTargetType(), report.getTargetId(), result.trim()));
    }

    // ==================== 私有 ====================

    private Report requirePending(Long id) {
        Report report = reportService.requireById(id);
        if (report.getStatus() == null || report.getStatus() != Report.STATUS_PENDING) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_HANDLED);
        }
        return report;
    }

    /** 目标归属用户（软删目标仍可回溯归属）；目标不存在抛 40410 */
    private Long resolveTargetOwner(Report report) {
        String type = report.getTargetType();
        if (AdminContentVO.TYPE_GOODS.equals(type)) {
            Goods goods = goodsMapper.selectById(report.getTargetId());
            if (goods == null) {
                throw new BusinessException(ErrorCode.REPORT_TARGET_MISSING);
            }
            return goods.getUserId();
        }
        if (AdminContentVO.TYPE_WANT.equals(type)) {
            WantPost post = wantPostMapper.selectById(report.getTargetId());
            if (post == null) {
                throw new BusinessException(ErrorCode.REPORT_TARGET_MISSING);
            }
            return post.getUserId();
        }
        if (AdminContentVO.TYPE_SWAP.equals(type)) {
            SwapPost post = swapPostMapper.selectById(report.getTargetId());
            if (post == null) {
                throw new BusinessException(ErrorCode.REPORT_TARGET_MISSING);
            }
            return post.getUserId();
        }
        throw new BusinessException(ErrorCode.REPORT_TARGET_MISSING);
    }

    private void applyAction(Report report, String action, Long ownerId, String result, LoginUser operator) {
        switch (action) {
            case "TAKE_DOWN" -> adminContentService.takeDown(report.getTargetType(),
                    report.getTargetId(), result, operator);
            case "DEDUCT" -> creditService.addCredit(ownerId, CreditLog.REASON_REPORT_VALID,
                    CreditLog.REF_TYPE_REPORT, report.getId());
            case "BAN" -> adminUserService.ban(ownerId, "举报属实：" + result, null, operator);
            case "WARN" -> notify(ownerId, String.format(
                    "您发布的内容（%s #%d）被举报且经核查属实，已给予警告：%s",
                    report.getTargetType(), report.getTargetId(), result));
            default -> throw new BusinessException(ErrorCode.ADM_PARAM_INVALID, "未知处置动作");
        }
    }

    private void notify(Long userId, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(Notification.TYPE_REPORT);
        n.setTitle("举报工单通知");
        n.setContent(content);
        n.setIsRead(0);
        notificationMapper.insert(n);
    }
}
