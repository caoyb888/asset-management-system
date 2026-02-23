package com.asset.investment.contract.service.impl;

import com.asset.investment.contract.entity.InvLeaseContractFee;
import com.asset.investment.contract.mapper.InvLeaseContractFeeMapper;
import com.asset.investment.contract.service.InvLeaseContractFeeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class InvLeaseContractFeeServiceImpl extends ServiceImpl<InvLeaseContractFeeMapper, InvLeaseContractFee> implements InvLeaseContractFeeService {}
