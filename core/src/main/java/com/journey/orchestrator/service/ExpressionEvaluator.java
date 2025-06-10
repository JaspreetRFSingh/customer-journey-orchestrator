package com.journey.orchestrator.service;

import com.journey.orchestrator.model.Expression;
import com.journey.orchestrator.model.Expression.ExpressionType;
import com.journey.orchestrator.model.Expression.Operator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * High-performance expression evaluator for journey conditions.
 * Evaluates boolean expressions against customer profiles and events.
 * 
 * This is a core component of the decisioning engine, similar to
 * journey orchestration platforms's real-time decisioning capabilities.
 */
public class ExpressionEvaluator {
    
    /**
     * Evaluates an expression against a data context.
     * 
     * @param expression The expression to evaluate
     * @param context The data context (profile attributes, event payload, etc.)
     * @return The evaluation result
     */
    public Object evaluate(Expression expression, Map<String, Object> context) {
        if (expression == null) {
            return true;
        }
        
        return switch (expression.getType()) {
            case LITERAL -> expression.getValue();
            case FIELD_REFERENCE -> evaluateFieldReference(expression.getField(), context);
            case COMPARISON -> evaluateComparison(expression, context);
            case BOOLEAN -> evaluateBoolean(expression, context);
            case FUNCTION -> evaluateFunction(expression, context);
            case EXISTS -> evaluateExists(expression.getField(), context);
        };
    }
    
    /**
     * Evaluates a boolean expression, returning true/false.
     */
    public boolean evaluateBoolean(Expression expression, Map<String, Object> context) {
        Object result = evaluate(expression, context);
        return result instanceof Boolean b && b;
    }
    
    /**
     * Evaluates a field reference against the context.
     * Supports nested fields using dot notation (e.g., "profile.attributes.age").
     */
    private Object evaluateFieldReference(String field, Map<String, Object> context) {
        if (field == null || context == null) {
            return null;
        }
        
        String[] parts = field.split("\\.");
        Object current = context;
        
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else if (current != null) {
                // Try to access as a property (for custom objects)
                current = getPropertyValue(current, part);
            } else {
                return null;
            }
            
            if (current == null) {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Evaluates a comparison expression.
     */
    private Object evaluateComparison(Expression expression, Map<String, Object> context) {
        Object fieldValue = evaluateFieldReference(expression.getField(), context);
        Object compareValue = expression.getValue();
        
        // Handle IN and NOT_IN operators specially
        if (expression.getOperator() == Operator.IN) {
            return isIn(fieldValue, compareValue);
        } else if (expression.getOperator() == Operator.NOT_IN) {
            return !isIn(fieldValue, compareValue);
        }
        
        return switch (expression.getOperator()) {
            case EQUALS -> equals(fieldValue, compareValue);
            case NOT_EQUALS -> !equals(fieldValue, compareValue);
            case GREATER_THAN -> compare(fieldValue, compareValue) > 0;
            case GREATER_THAN_EQUALS -> compare(fieldValue, compareValue) >= 0;
            case LESS_THAN -> compare(fieldValue, compareValue) < 0;
            case LESS_THAN_EQUALS -> compare(fieldValue, compareValue) <= 0;
            case CONTAINS -> contains(fieldValue, compareValue);
            case STARTS_WITH -> startsWith(fieldValue, compareValue);
            case ENDS_WITH -> endsWith(fieldValue, compareValue);
            case MATCHES_REGEX -> matchesRegex(fieldValue, compareValue);
            case IS_NULL -> fieldValue == null;
            case IS_NOT_NULL -> fieldValue != null;
            case IS_EMPTY -> isEmpty(fieldValue);
            case IS_NOT_EMPTY -> !isEmpty(fieldValue);
            default -> false;
        };
    }
    
    /**
     * Evaluates a boolean (AND, OR, NOT) expression.
     */
    private Object evaluateBoolean(Expression expression, Map<String, Object> context) {
        List<Expression> children = expression.getChildren();
        if (children == null || children.isEmpty()) {
            return false;
        }
        
        return switch (expression.getOperator()) {
            case AND -> children.stream().allMatch(c -> evaluateBoolean(c, context));
            case OR -> children.stream().anyMatch(c -> evaluateBoolean(c, context));
            case NOT -> {
                if (children.size() != 1) {
                    throw new IllegalArgumentException("NOT operator requires exactly one child");
                }
                yield !evaluateBoolean(children.get(0), context);
            }
            default -> false;
        };
    }
    
    /**
     * Evaluates a function expression.
     * Supports built-in functions for common operations.
     */
    private Object evaluateFunction(Expression expression, Map<String, Object> context) {
        String functionName = expression.getParameters() != null ? 
            (String) expression.getParameters().get("name") : null;
        
        if (functionName == null) {
            return false;
        }
        
        List<Expression> args = expression.getChildren();
        
        return switch (functionName) {
            case "now" -> System.currentTimeMillis();
            case "timestamp" -> args != null && !args.isEmpty() ? 
                evaluate(args.get(0), context) : System.currentTimeMillis();
            case "length" -> {
                Object value = args != null && !args.isEmpty() ? 
                    evaluate(args.get(0), context) : null;
                yield value instanceof String s ? s.length() :
                      value instanceof Collection<?> c ? c.size() : 0;
            }
            case "lower" -> {
                Object value = args != null && !args.isEmpty() ? 
                    evaluate(args.get(0), context) : null;
                yield value != null ? value.toString().toLowerCase() : null;
            }
            case "upper" -> {
                Object value = args != null && !args.isEmpty() ? 
                    evaluate(args.get(0), context) : null;
                yield value != null ? value.toString().toUpperCase() : null;
            }
            case "concat" -> {
                if (args == null) yield "";
                String result = args.stream()
                    .map(a -> String.valueOf(evaluate(a, context)))
                    .collect(Collectors.joining());
                yield result;
            }
            case "sum" -> {
                if (args == null) yield 0;
                double sum = args.stream()
                    .mapToDouble(a -> {
                        Object v = evaluate(a, context);
                        return v instanceof Number n ? n.doubleValue() : 0;
                    })
                    .sum();
                yield sum;
            }
            case "avg" -> {
                if (args == null || args.isEmpty()) yield 0;
                double sum = args.stream()
                    .mapToDouble(a -> {
                        Object v = evaluate(a, context);
                        return v instanceof Number n ? n.doubleValue() : 0;
                    })
                    .sum();
                yield sum / args.size();
            }
            case "min" -> {
                if (args == null || args.isEmpty()) yield 0;
                yield args.stream()
                    .mapToDouble(a -> {
                        Object v = evaluate(a, context);
                        return v instanceof Number n ? n.doubleValue() : 0;
                    })
                    .min().orElse(0);
            }
            case "max" -> {
                if (args == null || args.isEmpty()) yield 0;
                yield args.stream()
                    .mapToDouble(a -> {
                        Object v = evaluate(a, context);
                        return v instanceof Number n ? n.doubleValue() : 0;
                    })
                    .max().orElse(0);
            }
            default -> false;
        };
    }
    
    /**
     * Checks if a field exists in the context.
     */
    private boolean evaluateExists(String field, Map<String, Object> context) {
        return evaluateFieldReference(field, context) != null;
    }
    
    // Helper methods for comparisons
    
    private boolean equals(Object a, Object b) {
        return Objects.equals(a, b);
    }
    
    private int compare(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        
        if (a instanceof Comparable<?> compA && b instanceof Comparable<?> compB) {
            try {
                return compA.compareTo(compB);
            } catch (ClassCastException e) {
                // Try numeric comparison
                return Double.compare(toDouble(a), toDouble(b));
            }
        }
        return 0;
    }
    
    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        if (obj instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    private boolean contains(Object fieldValue, Object compareValue) {
        if (fieldValue == null || compareValue == null) return false;
        
        if (fieldValue instanceof String str) {
            return str.contains(compareValue.toString());
        }
        if (fieldValue instanceof Collection<?> col) {
            return col.contains(compareValue);
        }
        if (fieldValue.getClass().isArray()) {
            Object[] arr = (Object[]) fieldValue;
            for (Object item : arr) {
                if (Objects.equals(item, compareValue)) return true;
            }
        }
        return false;
    }
    
    private boolean startsWith(Object fieldValue, Object compareValue) {
        if (fieldValue instanceof String str && compareValue != null) {
            return str.startsWith(compareValue.toString());
        }
        return false;
    }
    
    private boolean endsWith(Object fieldValue, Object compareValue) {
        if (fieldValue instanceof String str && compareValue != null) {
            return str.endsWith(compareValue.toString());
        }
        return false;
    }
    
    private boolean matchesRegex(Object fieldValue, Object compareValue) {
        if (fieldValue instanceof String str && compareValue != null) {
            try {
                Pattern pattern = Pattern.compile(compareValue.toString());
                return pattern.matcher(str).matches();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    private boolean isIn(Object fieldValue, Object compareValue) {
        if (compareValue instanceof Collection<?> col) {
            return col.contains(fieldValue);
        }
        if (compareValue != null && compareValue.getClass().isArray()) {
            Object[] arr = (Object[]) compareValue;
            for (Object item : arr) {
                if (Objects.equals(item, fieldValue)) return true;
            }
        }
        return false;
    }
    
    private boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String str) return str.isEmpty();
        if (value instanceof Collection<?> col) return col.isEmpty();
        if (value instanceof Map<?, ?> map) return map.isEmpty();
        if (value.getClass().isArray()) return ((Object[]) value).length == 0;
        return false;
    }
    
    private Object getPropertyValue(Object obj, String propertyName) {
        // This is a simplified implementation
        // In production, you'd use reflection or a bean introspection library
        return null;
    }
}
