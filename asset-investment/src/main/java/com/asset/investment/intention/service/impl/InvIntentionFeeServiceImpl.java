package com.asset.investment.intention.service.impl;

import com.asset.investment.intention.entity.InvIntentionFee;
import com.asset.investment.intention.mapper.InvIntentionFeeMapper;
import com.asset.investment.intention.service.InvIntentionFeeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/** 意向协议-费项明细 Service 实现 */
@Service
public class InvIntentionFeeServiceImpl extends ServiceImpl<InvIntentionFeeMapper, InvIntentionFee>
        implements InvIntentionFeeService {
}
