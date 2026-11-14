
package com.example;

import java.util.Map;

public class ExpenseValidator {

    public static Map<String, Object> validate(
            ExpenseState state) {

        Object amountValue =
            state.value("amount").orElse(null);

        if (!(amountValue instanceof Number)) {

            throw new IllegalArgumentException(
                "Expense amount must be numeric"
            );
        }

        double amount =
            ((Number) amountValue).doubleValue();

        String description =
            state.<String>value("description")
                .orElse("");

        if (!Double.isFinite(amount) || amount <= 0) {

            throw new IllegalArgumentException(
                "Expense amount must be greater than zero"
            );
        }

        if (description.isBlank()) {

            throw new IllegalArgumentException(
                "Expense description cannot be empty"
            );
        }

        System.out.println(
            "Expense validation successful"
        );

        return Map.of(
            "validationStatus", "VALID"
        );
    }
}
