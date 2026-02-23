package com.asset.investment.intention.service.impl;

import com.asset.investment.intention.entity.InvIntentionShop;
import com.asset.investment.intention.mapper.InvIntentionShopMapper;
import com.asset.investment.intention.service.InvIntentionShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/** 意向协议-商铺关联 Service 实现 */
@Service
public class InvIntentionShopServiceImpl extends ServiceImpl<InvIntentionShopMapper, InvIntentionShop>
        implements InvIntentionShopService {
}
