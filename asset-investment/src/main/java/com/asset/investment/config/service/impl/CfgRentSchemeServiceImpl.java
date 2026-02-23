package com.asset.investment.config.service.impl;

import com.asset.investment.config.entity.CfgRentScheme;
import com.asset.investment.config.mapper.CfgRentSchemeMapper;
import com.asset.investment.config.service.CfgRentSchemeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/** 计租方案 Service 实现 */
@Service
public class CfgRentSchemeServiceImpl
        extends ServiceImpl<CfgRentSchemeMapper, CfgRentScheme>
        implements CfgRentSchemeService {
}
