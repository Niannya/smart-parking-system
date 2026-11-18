package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.dto.feerule.FeeRule;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 收费规则 Mapper（成员 3）。
 */
@Mapper
public interface FeeRuleMapper {

    int insert(FeeRule rule);

    FeeRule selectById(@Param("id") Long id);

    /** 按创建时间倒序（created_at DESC, id DESC） */
    List<FeeRule> selectByLot(@Param("lotId") Long lotId);

    FeeRule selectActive(@Param("lotId") Long lotId);

    /** 关闭旧活动规则：is_active=false, effective_to=now */
    int deactivateActive(@Param("lotId") Long lotId, @Param("now") OffsetDateTime now);
}
