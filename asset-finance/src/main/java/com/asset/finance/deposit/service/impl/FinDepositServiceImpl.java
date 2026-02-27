package com.asset.finance.deposit.service.impl;
import com.asset.finance.deposit.entity.FinDeposit;
import com.asset.finance.deposit.mapper.FinDepositMapper;
import com.asset.finance.deposit.service.FinDepositService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
@Service public class FinDepositServiceImpl extends ServiceImpl<FinDepositMapper, FinDeposit> implements FinDepositService {}
