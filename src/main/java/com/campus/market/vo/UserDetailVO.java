package com.campus.market.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户主页/个人资料响应（PRD USR-04）。
 * 学号与校园邮箱脱敏返回（PRD §9.2），未认证时为 null。
 */
@Getter
@Setter
public class UserDetailVO {

    private Long id;

    private String nickname;

    private String avatar;

    private String college;

    private String bio;

    private Integer creditScore;

    /** 优秀 / 良好 / 一般 / 受限（PRD §5.7 四档） */
    private String creditLevel;

    /** 0=未认证 1=已认证 */
    private Integer authStatus;

    /** 在售商品数 */
    private Long onSaleCount;

    /** 脱敏学号，未认证为 null */
    private String studentNoMasked;

    /** 脱敏校园邮箱，未认证为 null */
    private String campusEmailMasked;

    private LocalDateTime createdAt;
}
