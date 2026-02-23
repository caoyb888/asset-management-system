package com.asset.investment.engine;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 租金计算策略路由器
 * 根据 cfg_rent_scheme.strategy_bean_name 路由到对应策略实现
 */
@Component
@RequiredArgsConstructor
public class RentCalculateStrategyRouter {
    private final ApplicationContext applicationContext;

    public RentCalculateStrategy route(String strategyBeanName) {
        return (RentCalculateStrategy) applicationContext.getBean(strategyBeanName);
    }
}
