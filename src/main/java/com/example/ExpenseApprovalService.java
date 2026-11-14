
package com.example;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.GraphResult;
import org.bsc.langgraph4j.RunnableConfig;

@Service
public class ExpenseApprovalService {

    private final CompiledGraph<ExpenseState> expenseGraph;

    private final Map<String, Map<String, Object>> expenses =
        new ConcurrentHashMap<>();

    public ExpenseApprovalService(
            CompiledGraph<ExpenseState> expenseGraph) {

        this.expenseGraph = expenseGraph;
    }

    // =====================================
    // 1. Submit expense
    // =====================================

    public Map<String, Object> submitExpense(
            BigDecimal amount,
            String description) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                "Expense amount must be greater than zero"
            );
        }

        if (description == null || description.isBlank()) {

            throw new IllegalArgumentException(
                "Expense description is required"
            );
        }

        String expenseId =
            UUID.randomUUID().toString();

        // Unique LangGraph thread for this expense
        RunnableConfig config =
            RunnableConfig.builder()
                .threadId(expenseId)
                .build();

        // Initial workflow input
        Map<String, Object> input = Map.of(
            "amount", amount.doubleValue(),
            "description", description
        );

        // Start workflow
        GraphResult result = expenseGraph.invokeSync(
            GraphInput.args(input),
            config
        );

        Map<String, Object> expense =
            new ConcurrentHashMap<>();

        expense.put("expenseId", expenseId);
        expense.put("amount", amount);
        expense.put("description", description);

        // Check whether workflow is interrupted
        if (result.isInterruptionMetadata()) {

            expense.put(
                "status",
                "PENDING_APPROVAL"
            );

            System.out.println(
                "Workflow interrupted. Waiting for human approval."
            );

        } else {

            if (!result.isStateDataOrCheckpointSaverTag()) {
                throw new IllegalStateException(
                    "Unexpected workflow result"
                );
            }

            Map<String, Object> finalState =
                result.asStateDataOrLastCheckpointStateData();

            expense.put(
                "status",
                finalState.get("approvalStatus")
            );

            expense.put(
                "approvedBy",
                finalState.get("approvedBy")
            );

            expense.put(
                "finalResult",
                finalState.get("finalResult")
            );
        }

        expenses.put(expenseId, expense);

        return expense;
    }

    // =====================================
    // 2. Human approval or rejection
    // =====================================

    public Map<String, Object> processDecision(
            String expenseId,
            String decision) {

        Map<String, Object> expense =
            expenses.get(expenseId);

        if (expense == null) {

            throw new IllegalArgumentException(
                "Expense not found: " + expenseId
            );
        }

        synchronized (expense) {

            if (!"PENDING_APPROVAL".equals(
                    expense.get("status"))) {

                throw new IllegalStateException(
                    "Expense is not awaiting approval"
                );
            }

            if (decision == null) {

                throw new IllegalArgumentException(
                    "Decision is required"
                );
            }

            decision =
                decision.trim().toUpperCase(Locale.ROOT);

            if (!decision.equals("APPROVED")
                    && !decision.equals("REJECTED")) {

                throw new IllegalArgumentException(
                    "Decision must be APPROVED or REJECTED"
                );
            }

            // Use the same thread ID
            RunnableConfig config =
                RunnableConfig.builder()
                    .threadId(expenseId)
                    .build();

            // Resume interrupted workflow
            GraphResult result = expenseGraph.invokeSync(

                GraphInput.resume(
                    Map.of(
                        "humanDecision",
                        decision
                    )
                ),

                config
            );

            if (result.isInterruptionMetadata()) {

                throw new IllegalStateException(
                    "Workflow was interrupted unexpectedly"
                );
            }

            if (!result.isStateDataOrCheckpointSaverTag()) {

                throw new IllegalStateException(
                    "Unexpected workflow result after resume"
                );
            }

            Map<String, Object> finalState =
                result.asStateDataOrLastCheckpointStateData();

            String finalStatus =
                (String) finalState.get("approvalStatus");

            if (!decision.equals(finalStatus)) {

                throw new IllegalStateException(
                    "Workflow final decision does not match human decision"
                );
            }

            // Update expense status
            expense.put("status", finalStatus);

            expense.put(
                "approvedBy",
                finalState.get("approvedBy")
            );

            expense.put(
                "finalResult",
                finalState.get("finalResult")
            );

            return expense;
        }
    }

    // =====================================
    // 3. Get expense status
    // =====================================

    public Map<String, Object> getExpenseStatus(
            String expenseId) {

        Map<String, Object> expense =
            expenses.get(expenseId);

        if (expense == null) {

            throw new IllegalArgumentException(
                "Expense not found: " + expenseId
            );
        }

        return expense;
    }
}
