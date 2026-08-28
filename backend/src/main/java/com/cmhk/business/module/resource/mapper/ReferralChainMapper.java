package com.cmhk.business.module.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmhk.business.module.resource.entity.ReferralChain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 推荐号码接龙数据访问。 */
@Mapper
public interface ReferralChainMapper extends BaseMapper<ReferralChain> {
    @Select("SELECT * FROM referral_chain WHERE id = #{id} FOR UPDATE")
    ReferralChain selectByIdForUpdate(Long id);
}
