package com.journey.orchestrator.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents a condition expression that can be evaluated against a customer profile or event.
 * Supports complex boolean logic and various comparison operators.
 */
@Data
@Builder
public class Expression {
    
    private final ExpressionType type;
    private final Operator operator;
    private final String field;
    private final Object value;
    private final List<Expression> children;
    private final Map<String, Object> parameters;
    
    public enum ExpressionType {
        LITERAL,            // A constant value
        FIELD_REFERENCE,    // Reference to a profile/event field
        COMPARISON,         // Compare two values
        BOOLEAN,            // AND, OR, NOT
        FUNCTION,           // Built-in function call
        EXISTS              // Check if field exists
    }
    
    public enum Operator {
        // Comparison
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        GREATER_THAN_EQUALS,
        LESS_THAN,
        LESS_THAN_EQUALS,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        IN,
        NOT_IN,
        MATCHES_REGEX,
        
        // Boolean
        AND,
        OR,
        NOT,
        
        // Arithmetic
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        
        // Special
        IS_NULL,
        IS_NOT_NULL,
        IS_EMPTY,
        IS_NOT_EMPTY
    }
    
    /**
     * Creates an AND expression combining multiple conditions.
     */
    public static Expression and(Expression... conditions) {
        return Expression.builder()
            .type(ExpressionType.BOOLEAN)
            .operator(Operator.AND)
            .children(List.of(conditions))
            .build();
    }
    
    /**
     * Creates an OR expression combining multiple conditions.
     */
    public static Expression or(Expression... conditions) {
        return Expression.builder()
            .type(ExpressionType.BOOLEAN)
            .operator(Operator.OR)
            .children(List.of(conditions))
            .build();
    }
    
    /**
     * Creates a NOT expression.
     */
    public static Expression not(Expression condition) {
        return Expression.builder()
            .type(ExpressionType.BOOLEAN)
            .operator(Operator.NOT)
            .children(List.of(condition))
            .build();
    }
    
    /**
     * Creates an equality comparison expression.
     */
    public static Expression equals(String field, Object value) {
        return Expression.builder()
            .type(ExpressionType.COMPARISON)
            .operator(Operator.EQUALS)
            .field(field)
            .value(value)
            .build();
    }
    
    /**
     * Creates a greater than comparison expression.
     */
    public static Expression greaterThan(String field, Object value) {
        return Expression.builder()
            .type(ExpressionType.COMPARISON)
            .operator(Operator.GREATER_THAN)
            .field(field)
            .value(value)
            .build();
    }
    
    /**
     * Creates a contains expression for string/array fields.
     */
    public static Expression contains(String field, Object value) {
        return Expression.builder()
            .type(ExpressionType.COMPARISON)
            .operator(Operator.CONTAINS)
            .field(field)
            .value(value)
            .build();
    }
    
    /**
     * Creates an IN expression checking if field value is in a list.
     */
    @SafeVarargs
    public static Expression in(String field, Object... values) {
        return Expression.builder()
            .type(ExpressionType.COMPARISON)
            .operator(Operator.IN)
            .field(field)
            .value(List.of(values))
            .build();
    }
    
    /**
     * Creates an exists expression.
     */
    public static Expression exists(String field) {
        return Expression.builder()
            .type(ExpressionType.EXISTS)
            .field(field)
            .build();
    }
    
    /**
     * Creates a field reference expression.
     */
    public static Expression field(String fieldName) {
        return Expression.builder()
            .type(ExpressionType.FIELD_REFERENCE)
            .field(fieldName)
            .build();
    }
}
