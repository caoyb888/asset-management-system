package com.asset.investment.contract.service.impl;

import com.asset.investment.contract.entity.InvLeaseContractBilling;
import com.asset.investment.contract.mapper.InvLeaseContractBillingMapper;
import com.asset.investment.contract.service.InvLeaseContractBillingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class InvLeaseContractBillingServiceImpl extends ServiceImpl<InvLeaseContractBillingMapper, InvLeaseContractBilling> implements InvLeaseContractBillingService {}
