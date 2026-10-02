package com.campus.market.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 提取（PRD ADM-09 操作日志 / §9.2 限流）：优先反向代理头，兼容 IPv6（≤45 字符）。
 */
public final class IpUtils {

    private IpUtils() {
    }

    public static String clientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For 可能是链式：client, proxy1, proxy2 → 取第一个
                int comma = value.indexOf(',');
                return (comma > 0 ? value.substring(0, comma) : value).trim();
            }
        }
        return request.getRemoteAddr();
    }
}
