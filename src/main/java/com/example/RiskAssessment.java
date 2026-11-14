
package com.example;

import java.util.Map;

public class RiskAssessment {

    public static final double APPROVAL_LIMIT = 10000.0;

    public static boolean requiresHumanReview(
            double amount) {

        return amount > APPROVAL_LIMIT;
    }

    public static Map<String, Object> assess(
            ExpenseState state) {

        Object amountValue =
            state.value("amount").orElse(null);

        if (!(amountValue instanceof Number)) {

            throw new IllegalArgumentException(
                "Invalid expense amount"
            );
        }

        double amount =
            ((Number) amountValue).doubleValue();

        String riskLevel =
            requiresHumanReview(amount)
                ? "HIGH"
                : "LOW";

        System.out.println(
            "Risk assessment: " + riskLevel
        );

        return Map.of(
            "riskLevel", riskLevel
        );
    }
}
