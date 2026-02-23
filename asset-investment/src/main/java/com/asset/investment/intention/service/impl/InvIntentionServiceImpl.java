package com.asset.investment.intention.service.impl;

import com.asset.investment.intention.entity.InvIntention;
import com.asset.investment.intention.mapper.InvIntentionMapper;
import com.asset.investment.intention.service.InvIntentionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/** 意向协议 Service 实现 */
@Service
public class InvIntentionServiceImpl extends ServiceImpl<InvIntentionMapper, InvIntention>
        implements InvIntentionService {
}
