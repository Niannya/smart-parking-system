package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.FeeRule;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 收费规则数据访问。SQL 全部写在 resources/mapper/FeeRuleMapper.xml。
 */
@Mapper
public interface FeeRuleMapper {

    int insert(FeeRule rule);

    FeeRule selectById(@Param("id") Long id);

    /** 按创建时间倒序（created_at DESC, id DESC） */
    List<FeeRule> selectByLot(@Param("lotId") Long lotId);

    /** 当前生效规则；唯一部分索引保证每个停车场最多一条 */
    FeeRule selectActive(@Param("lotId") Long lotId);

    /** 关闭旧活动规则：is_active=false, effective_to=now */
    int deactivateActive(@Param("lotId") Long lotId, @Param("now") OffsetDateTime now);
}
