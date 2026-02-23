package com.asset.investment.intention.service.impl;

import com.asset.investment.intention.entity.InvIntentionBilling;
import com.asset.investment.intention.mapper.InvIntentionBillingMapper;
import com.asset.investment.intention.service.InvIntentionBillingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/** 意向协议-账期 Service 实现 */
@Service
public class InvIntentionBillingServiceImpl extends ServiceImpl<InvIntentionBillingMapper, InvIntentionBilling>
        implements InvIntentionBillingService {
}
