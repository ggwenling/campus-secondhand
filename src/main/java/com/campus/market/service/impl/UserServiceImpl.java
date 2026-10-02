package com.campus.market.service.impl;

import com.campus.market.common.api.ErrorCode;
import com.campus.market.common.exception.BusinessException;
import com.campus.market.config.CreditProperties;
import com.campus.market.dto.UserEditDTO;
import com.campus.market.entity.User;
import com.campus.market.entity.UserAuth;
import com.campus.market.mapper.UserAuthMapper;
import com.campus.market.mapper.UserMapper;
import com.campus.market.security.LoginUser;
import com.campus.market.service.UserService;
import com.campus.market.vo.UserDetailVO;
import com.campus.market.vo.UserOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;

/**
 * 用户服务实现（PRD USR-04/05/06、§4.1 封禁、§5.7 信用等级）。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;
    private final CreditProperties creditProperties;

    @Override
    public UserDetailVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setCollege(user.getCollege());
        vo.setBio(user.getBio());
        vo.setCreditScore(user.getCreditScore());
        vo.setCreditLevel(creditLevel(user.getCreditScore()));
        vo.setAuthStatus(user.getAuthStatus());
        vo.setOnSaleCount(userMapper.countOnSaleGoods(userId));
        vo.setCreatedAt(user.getCreatedAt());
        UserAuth auth = userAuthMapper.selectByUserId(userId);
        if (auth != null) {
            vo.setStudentNoMasked(mask(auth.getStudentNo(), 3, 2));
            vo.setCampusEmailMasked(maskEmail(auth.getCampusEmail()));
        }
        return vo;
    }

    @Override
    public void updateMe(Long userId, UserEditDTO dto) {
        User patch = new User();
        patch.setId(userId);
        patch.setNickname(dto.getNickname());
        patch.setAvatar(dto.getAvatar());
        patch.setCollege(dto.getCollege());
        // bio 为用户自由文本，入库前转义防 XSS（PRD §9.2）
        patch.setBio(StringUtils.hasText(dto.getBio()) ? HtmlUtils.htmlEscape(dto.getBio()) : dto.getBio());
        userMapper.updateById(patch);
    }

    @Override
    public UserOverviewVO overview(Long userId) {
        UserOverviewVO vo = new UserOverviewVO();
        vo.setOnSaleCount(userMapper.countOnSaleGoods(userId));
        vo.setSoldCount(userMapper.countSoldGoods(userId));
        vo.setFavoriteCount(userMapper.countFavorites(userId));
        return vo;
    }

    @Override
    public void applyFreshState(LoginUser loginUser) {
        User user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            return;
        }
        // 封禁拦截 + 到期自动解封（PRD §4.1 / §5.7 封禁期限）
        if (user.getStatus() != null && user.getStatus() == User.STATUS_BANNED) {
            if (user.getBannedUntil() != null && user.getBannedUntil().isBefore(LocalDateTime.now())) {
                User patch = new User();
                patch.setId(user.getId());
                patch.setStatus(User.STATUS_NORMAL);
                userMapper.updateById(patch);
            } else {
                throw new BusinessException(ErrorCode.ACCOUNT_BANNED);
            }
        }
        loginUser.setAuthStatus(user.getAuthStatus());
        loginUser.setCreditScore(user.getCreditScore());
    }

    /** 信用四档等级（PRD §5.7）：优秀≥120 / 良好 80~119 / 一般 60~79 / 受限&lt;60 */
    private String creditLevel(Integer score) {
        int s = score == null ? 0 : score;
        if (s >= creditProperties.getExcellentThreshold()) {
            return "优秀";
        }
        if (s >= creditProperties.getGoodThreshold()) {
            return "良好";
        }
        if (s >= creditProperties.getRestrictedThreshold()) {
            return "一般";
        }
        return "受限";
    }

    /** 中段打码：保留前 keepHead 后 keepTail（PRD §9.2 脱敏） */
    private String mask(String value, int keepHead, int keepTail) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.length() <= keepHead + keepTail) {
            return "****";
        }
        return value.substring(0, keepHead) + "****" + value.substring(value.length() - keepTail);
    }

    /** 邮箱脱敏：本地部分保留前 2 位，域名完整保留 */
    private String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "****";
        }
        String local = email.substring(0, at);
        String head = local.length() <= 2 ? local : local.substring(0, 2);
        return head + "****" + email.substring(at);
    }
}
