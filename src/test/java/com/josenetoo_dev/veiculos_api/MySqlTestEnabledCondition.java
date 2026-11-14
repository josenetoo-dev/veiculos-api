package com.josenetoo_dev.veiculos_api;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Keeps database integration tests opt-in; malformed or incomplete settings never reach JDBC. */
public final class MySqlTestEnabledCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        var environment = System.getenv();
        if (MySqlTestConfiguration.isCompletelyAbsent(environment)) {
            return ConditionEvaluationResult.disabled("MySQL integration tests are opt-in; MYSQL_TEST_* is not configured");
        }
        try {
            MySqlTestConfiguration.from(environment);
            return ConditionEvaluationResult.enabled("MYSQL_TEST_URL points to a permitted local test endpoint");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid MySQL test configuration: " + exception.getMessage(), exception);
        }
    }
}
