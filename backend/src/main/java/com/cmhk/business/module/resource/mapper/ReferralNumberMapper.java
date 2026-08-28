package com.cmhk.business.module.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmhk.business.module.resource.entity.ReferralNumber;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 推荐号码池数据访问。 */
@Mapper
public interface ReferralNumberMapper extends BaseMapper<ReferralNumber> {
    @Select("SELECT * FROM referral_number_pool WHERE id = #{id} FOR UPDATE")
    ReferralNumber selectByIdForUpdate(Long id);
}
