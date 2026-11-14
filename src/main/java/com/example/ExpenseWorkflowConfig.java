
package com.example;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.checkpoint.MemorySaver;

import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.AsyncEdgeAction;

import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.GraphDefinition.END;

@Configuration
public class ExpenseWorkflowConfig {

    @Bean
    public CompiledGraph<ExpenseState> expenseGraph()
            throws Exception {

        // Node 1: Validate expense
        AsyncNodeAction<ExpenseState> validateExpense =
            AsyncNodeAction.node_async(
                (ExpenseState state) ->
                    ExpenseValidator.validate(state)
            );

        // Node 2: Risk assessment
        AsyncNodeAction<ExpenseState> riskAssessment =
            AsyncNodeAction.node_async(
                (ExpenseState state) ->
                    RiskAssessment.assess(state)
            );

        // Node 3: Automatic approval
        AsyncNodeAction<ExpenseState> autoApprove =
            AsyncNodeAction.node_async(
                (ExpenseState state) -> {

                    System.out.println(
                        "Automatically approving expense"
                    );

                    return Map.of(
                        "approvalStatus", "APPROVED",
                        "approvedBy", "SYSTEM"
                    );
                }
            );

        // Node 4: Human review
        AsyncNodeAction<ExpenseState> humanReview =
            AsyncNodeAction.node_async(
                (ExpenseState state) ->
                    HumanApprovalService.processDecision(state)
            );

        // Node 5: Finalize expense
        AsyncNodeAction<ExpenseState> finalizeExpense =
            AsyncNodeAction.node_async(
                (ExpenseState state) -> {

                    String status =
                        state.<String>value("approvalStatus")
                            .orElse("UNKNOWN");

                    String approvedBy =
                        state.<String>value("approvedBy")
                            .orElse("UNKNOWN");

                    String result =
                        "Expense " + status +
                        " by " + approvedBy;

                    System.out.println(result);

                    return Map.of(
                        "finalResult", result
                    );
                }
            );

        // Conditional routing
        AsyncEdgeAction<ExpenseState> riskRouter =
            AsyncEdgeAction.edge_async(
                (ExpenseState state) -> {

                    String risk =
                        state.<String>value("riskLevel")
                            .orElse("HIGH");

                    return risk.equals("LOW")
                        ? "AUTO"
                        : "HUMAN";
                }
            );

        // Build graph with explicitly declared state type
        StateGraph<ExpenseState> workflow =
            new StateGraph<ExpenseState>(
                ExpenseState::new
            )

            .addNode("validate_expense", validateExpense)

            .addNode("risk_assessment", riskAssessment)

            .addNode("auto_approve", autoApprove)

            .addNode("human_review", humanReview)

            .addNode("finalize_expense", finalizeExpense)

            .addEdge(START, "validate_expense")

            .addEdge(
                "validate_expense",
                "risk_assessment"
            )

            .addConditionalEdges(
                "risk_assessment",
                riskRouter,
                Map.of(
                    "AUTO", "auto_approve",
                    "HUMAN", "human_review"
                )
            )

            .addEdge(
                "auto_approve",
                "finalize_expense"
            )

            .addEdge(
                "human_review",
                "finalize_expense"
            )

            .addEdge(
                "finalize_expense",
                END
            );

        // Checkpoint configuration
        CompileConfig compileConfig =
            CompileConfig.builder()

                .checkpointSaver(
                    new MemorySaver()
                )

                .interruptBefore(
                    "human_review"
                )

                .build();

        return workflow.compile(compileConfig);
    }
}
