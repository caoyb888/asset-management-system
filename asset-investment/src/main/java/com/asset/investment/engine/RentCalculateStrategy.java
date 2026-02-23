package com.asset.investment.engine;
import java.math.BigDecimal;
/**
 * 租金计算策略接口（Strategy模式）
 * 5种实现类对应5种收费方式，第三阶段任务3.1完成具体实现
 */
public interface RentCalculateStrategy {
    BigDecimal calculate(RentCalculateContext context);
}
