package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户行为 Mapper（PRD GDS-05 浏览埋点 / 数据库设计文档 §3.26）。
 * VIEW 同人同商品同日的去重由 uk_behavior_view_day 数据库唯一约束保证（T9）。
 */
@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {
}
