package com.payment_service.payment_service.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory Pattern — resolves the correct PaymentStrategy at runtime based on the payment method name.
 * Spring auto-injects all PaymentStrategy beans, so adding a new strategy requires zero factory code changes.
 */
@Component
@RequiredArgsConstructor
public class PaymentStrategyFactory {

    private final Map<String, PaymentStrategy> strategyMap;

    public PaymentStrategyFactory(List<PaymentStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        s -> s.getMethodName().toUpperCase(),
                        Function.identity()
                ));
    }

    /**
     * Returns the correct payment strategy for the given method name.
     *
     * @param paymentMethod e.g. "UPI", "CARD", "COD"
     * @return the matching {@link PaymentStrategy}
     * @throws IllegalArgumentException if the method is not supported
     */
    public PaymentStrategy getStrategy(String paymentMethod) {
        PaymentStrategy strategy = strategyMap.get(paymentMethod.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + paymentMethod
                    + ". Supported methods: " + strategyMap.keySet());
        }
        return strategy;
    }
}
