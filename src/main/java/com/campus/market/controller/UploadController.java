package com.campus.market.controller;

import com.campus.market.common.api.Result;
import com.campus.market.security.LoginUser;
import com.campus.market.security.UserContext;
import com.campus.market.common.api.ErrorCode;
import com.campus.market.common.exception.BusinessException;
import com.campus.market.service.UploadService;
import com.campus.market.vo.UploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传接口（PRD §9.1 / GDS-01）：登录后可上传，jpg/png/webp ≤5MB，返回 {url, thumbUrl}
 */
@Tag(name = "商品-图片上传")
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @Operation(summary = "上传单张商品图片（multipart 字段名 file）")
    @PostMapping
    public Result<UploadVO> upload(@RequestPart("file") MultipartFile file) {
        requireFrontUser();
        return Result.ok(uploadService.saveImage(file));
    }

    /** 上传仅限前台用户主体（admin 与 user 主键空间独立，防止越权） */
    private void requireFrontUser() {
        LoginUser user = UserContext.get();
        if (user == null || user.getUserType() != LoginUser.UserType.USER) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
