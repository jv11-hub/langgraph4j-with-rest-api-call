
package com.example;

public class ApprovalRequest {

    private String decision;

    public ApprovalRequest() {
    }

    public ApprovalRequest(String decision) {
        this.decision = decision;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }
}
