package com.cmhk.business.module.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmhk.business.module.business.entity.BusinessType;
import com.cmhk.business.module.business.mapper.BusinessTypeMapper;
import com.cmhk.business.module.business.service.BusinessTypeService;
import org.springframework.stereotype.Service;

@Service
public class BusinessTypeServiceImpl extends ServiceImpl<BusinessTypeMapper, BusinessType> implements BusinessTypeService {
}

