
package com.example;

import java.util.Map;
import java.util.Locale;

public class HumanApprovalService {

    public static Map<String, Object> processDecision(
            ExpenseState state) {

        String decision =
            state.<String>value("humanDecision")
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Human decision is missing"
                    )
                );

        decision =
            decision.trim().toUpperCase(Locale.ROOT);

        if (!decision.equals("APPROVED")
                && !decision.equals("REJECTED")) {

            throw new IllegalArgumentException(
                "Invalid human decision: " + decision
            );
        }

        System.out.println(
            "Human approval decision: " + decision
        );

        return Map.of(
            "approvalStatus", decision,
            "approvedBy", "HUMAN"
        );
    }
}
