package com.journey.orchestrator.service;

import com.journey.orchestrator.model.Expression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ExpressionEvaluator.
 * Demonstrates understanding of the decisioning engine.
 */
class ExpressionEvaluatorTest {

    private ExpressionEvaluator evaluator;
    private Map<String, Object> context;

    @BeforeEach
    void setUp() {
        evaluator = new ExpressionEvaluator();
        context = Map.of(
            "profile", Map.of(
                "firstName", "John",
                "lastName", "Doe",
                "age", 30,
                "email", "john.doe@example.com",
                "totalSpend", 1500.00,
                "purchaseCount", 5,
                "segment", "HIGH_VALUE",
                "tags", List.of("vip", "early-adopter")
            ),
            "event", Map.of(
                "type", "purchase",
                "amount", 250.00,
                "currency", "USD"
            ),
            "timestamp", System.currentTimeMillis()
        );
    }

    @Test
    @DisplayName("Should evaluate equality expressions")
    void testEqualityExpression() {
        Expression expr = Expression.equals("profile.segment", "HIGH_VALUE");
        assertTrue(evaluator.evaluateBoolean(expr, context));

        Expression expr2 = Expression.equals("profile.age", 30);
        assertTrue(evaluator.evaluateBoolean(expr2, context));

        Expression expr3 = Expression.equals("profile.segment", "LOW_VALUE");
        assertFalse(evaluator.evaluateBoolean(expr3, context));
    }

    @Test
    @DisplayName("Should evaluate comparison expressions")
    void testComparisonExpressions() {
        Expression expr = Expression.greaterThan("profile.totalSpend", 1000);
        assertTrue(evaluator.evaluateBoolean(expr, context));

        Expression expr2 = Expression.builder()
            .type(Expression.ExpressionType.COMPARISON)
            .operator(Expression.Operator.LESS_THAN)
            .field("profile.age")
            .value(25)
            .build();
        assertFalse(evaluator.evaluateBoolean(expr2, context));
    }

    @Test
    @DisplayName("Should evaluate boolean AND expressions")
    void testAndExpression() {
        Expression expr = Expression.and(
            Expression.equals("profile.segment", "HIGH_VALUE"),
            Expression.greaterThan("profile.totalSpend", 1000)
        );
        assertTrue(evaluator.evaluateBoolean(expr, context));

        Expression expr2 = Expression.and(
            Expression.equals("profile.segment", "HIGH_VALUE"),
            Expression.equals("profile.age", 25)
        );
        assertFalse(evaluator.evaluateBoolean(expr2, context));
    }

    @Test
    @DisplayName("Should evaluate boolean OR expressions")
    void testOrExpression() {
        Expression expr = Expression.or(
            Expression.equals("profile.segment", "HIGH_VALUE"),
            Expression.equals("profile.age", 25)
        );
        assertTrue(evaluator.evaluateBoolean(expr, context));

        Expression expr2 = Expression.or(
            Expression.equals("profile.segment", "LOW_VALUE"),
            Expression.equals("profile.age", 25)
        );
        assertFalse(evaluator.evaluateBoolean(expr2, context));
    }

    @Test
    @DisplayName("Should evaluate NOT expressions")
    void testNotExpression() {
        Expression expr = Expression.not(Expression.equals("profile.segment", "LOW_VALUE"));
        assertTrue(evaluator.evaluateBoolean(expr, context));

        Expression expr2 = Expression.not(Expression.equals("profile.segment", "HIGH_VALUE"));
        assertFalse(evaluator.evaluateBoolean(expr2, context));
    }

    @Test
    @DisplayName("Should evaluate IN expressions")
    void testInExpression() {
        Expression expr = Expression.in("profile.segment", "HIGH_VALUE", "VIP", "PREMIUM");
        assertTrue(evaluator.evaluateBoolean(expr, context));

        Expression expr2 = Expression.in("profile.segment", "LOW_VALUE", "BASIC");
        assertFalse(evaluator.evaluateBoolean(expr2, context));
    }

    @Test
    @DisplayName("Should evaluate contains expressions")
    void testContainsExpression() {
        Expression expr = Expression.contains("profile.email", "@example.com");
        assertTrue(evaluator.evaluateBoolean(expr, context));

        Expression expr2 = Expression.contains("profile.tags", "vip");
        assertTrue(evaluator.evaluateBoolean(expr2, context));
    }

    @Test
    @DisplayName("Should evaluate exists expressions")
    void testExistsExpression() {
        Expression expr = Expression.exists("profile.firstName");
        assertTrue(evaluator.evaluateBoolean(expr, context));

        Expression expr2 = Expression.exists("profile.nonExistentField");
        assertFalse(evaluator.evaluateBoolean(expr2, context));
    }

    @Test
    @DisplayName("Should handle nested field references")
    void testNestedFieldReferences() {
        Expression expr = Expression.equals("profile.firstName", "John");
        assertTrue(evaluator.evaluateBoolean(expr, context));

        Expression expr2 = Expression.greaterThan("event.amount", 100);
        assertTrue(evaluator.evaluateBoolean(expr2, context));
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void testNullHandling() {
        Map<String, Object> nullContext = Map.of(
            "profile", Map.of(
                "name", null,
                "age", 30
            )
        );

        Expression expr = Expression.equals("profile.name", null);
        assertTrue(evaluator.evaluateBoolean(expr, nullContext));

        Expression expr2 = Expression.exists("profile.nonExistent");
        assertFalse(evaluator.evaluateBoolean(expr2, nullContext));
    }

    @Test
    @DisplayName("Should evaluate complex nested expressions")
    void testComplexNestedExpressions() {
        Expression expr = Expression.and(
            Expression.or(
                Expression.equals("profile.segment", "HIGH_VALUE"),
                Expression.greaterThan("profile.totalSpend", 2000)
            ),
            Expression.not(
                Expression.equals("profile.age", 25)
            ),
            Expression.greaterThan("event.amount", 50)
        );
        assertTrue(evaluator.evaluateBoolean(expr, context));
    }
}
