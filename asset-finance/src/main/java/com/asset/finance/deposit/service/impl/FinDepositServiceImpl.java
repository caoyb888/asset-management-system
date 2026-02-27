package com.asset.finance.deposit.service.impl;

import com.asset.finance.deposit.entity.FinDepositAccount;
import com.asset.finance.deposit.mapper.FinDepositAccountMapper;
import com.asset.finance.deposit.service.FinDepositService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FinDepositServiceImpl extends ServiceImpl<FinDepositAccountMapper, FinDepositAccount>
        implements FinDepositService {}
