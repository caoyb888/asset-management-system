package com.asset.finance.prepayment.service.impl;
import com.asset.finance.prepayment.entity.FinPrepayment;
import com.asset.finance.prepayment.mapper.FinPrepaymentMapper;
import com.asset.finance.prepayment.service.FinPrepaymentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
@Service public class FinPrepaymentServiceImpl extends ServiceImpl<FinPrepaymentMapper, FinPrepayment> implements FinPrepaymentService {}
