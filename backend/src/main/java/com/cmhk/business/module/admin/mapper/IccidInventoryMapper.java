package com.cmhk.business.module.admin.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cmhk.business.module.admin.entity.IccidInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** ICCID 卡池数据访问。 */
@Mapper
public interface IccidInventoryMapper extends BaseMapper<IccidInventory> {
    @Select("SELECT * FROM iccid_inventory WHERE id = #{id} FOR UPDATE")
    IccidInventory selectByIdForUpdate(Long id);
}
