package com.asset.investment.contract.service.impl;

import com.asset.investment.contract.entity.InvLeaseContract;
import com.asset.investment.contract.mapper.InvLeaseContractMapper;
import com.asset.investment.contract.service.InvLeaseContractService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class InvLeaseContractServiceImpl extends ServiceImpl<InvLeaseContractMapper, InvLeaseContract> implements InvLeaseContractService {}
