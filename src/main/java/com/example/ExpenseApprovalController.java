
package com.example;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseApprovalController {

    private final ExpenseApprovalService expenseApprovalService;

    public ExpenseApprovalController(
            ExpenseApprovalService expenseApprovalService) {

        this.expenseApprovalService = expenseApprovalService;
    }

    // Submit expense
    @PostMapping
    public ResponseEntity<?> submitExpense(
            @RequestBody ExpenseRequest request) {

        Map<String, Object> response =
                expenseApprovalService.submitExpense(
                        BigDecimal.valueOf(request.getAmount()),
                        request.getDescription()
                );

        return ResponseEntity.ok(response);
    }

    // Approve or reject expense
    @PostMapping("/{expenseId}/decision")
    public ResponseEntity<?> approveExpense(

            @PathVariable("expenseId") String expenseId,

            @RequestBody ApprovalRequest request) {

        Map<String, Object> response =
                expenseApprovalService.processDecision(
                        expenseId,
                        request.getDecision()
                );

        return ResponseEntity.ok(response);
    }

    // Get expense status
    @GetMapping("/{expenseId}")
    public ResponseEntity<?> getExpense(

            @PathVariable("expenseId") String expenseId) {

        return ResponseEntity.ok(
                expenseApprovalService.getExpenseStatus(expenseId)
        );
    }
}
