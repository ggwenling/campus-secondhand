package com.campus.market.service;

import com.campus.market.common.api.PageResult;
import com.campus.market.entity.Report;
import com.campus.market.security.LoginUser;

import java.util.List;

/**
 * 举报工单处置服务（PRD ADM-04，SUPER+AUDITOR）：工单分页/详情/组合处置/驳回。
 * 处置原语复用 M7 前置的 {@link ReportService#markHandled}；处置动作组合委托
 * AdminContentService（下架）/ AdminUserService（封禁）/ CreditService（扣分）。
 */
public interface AdminReportService {

    /** 工单分页（status 可选 0/1/2） */
    PageResult<Report> page(Integer status, long pageNum, long pageSize);

    /** 工单详情 */
    Report detail(Long id);

    /**
     * 组合处置：status 0→1。actions 任选多：
     * TAKE_DOWN（委托内容下架）/ WARN（警告通知）/ DEDUCT（举报属实 -10）/ BAN（封号）。
     * result 必填；目标缺失且动作需要目标时抛 40410；重复处置 40918；双方通知。
     */
    void handle(Long id, List<String> actions, String result, LoginUser operator);

    /** 驳回：status 0→2，result 必填，通知举报人"未成立" */
    void reject(Long id, String result, LoginUser operator);
}
