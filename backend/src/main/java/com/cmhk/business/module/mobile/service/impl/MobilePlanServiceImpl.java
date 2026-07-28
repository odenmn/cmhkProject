package com.cmhk.business.module.mobile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cmhk.business.module.mobile.entity.MobilePlan;
import com.cmhk.business.module.mobile.mapper.MobilePlanMapper;
import com.cmhk.business.module.mobile.service.MobilePlanService;
import org.springframework.stereotype.Service;

@Service
public class MobilePlanServiceImpl extends ServiceImpl<MobilePlanMapper, MobilePlan> implements MobilePlanService {
}

