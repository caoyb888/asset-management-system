package com.asset.investment.config.service.impl;

import com.asset.investment.config.entity.CfgFeeItem;
import com.asset.investment.config.mapper.CfgFeeItemMapper;
import com.asset.investment.config.service.CfgFeeItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/** 收款项目 Service 实现 */
@Service
public class CfgFeeItemServiceImpl
        extends ServiceImpl<CfgFeeItemMapper, CfgFeeItem>
        implements CfgFeeItemService {
}
