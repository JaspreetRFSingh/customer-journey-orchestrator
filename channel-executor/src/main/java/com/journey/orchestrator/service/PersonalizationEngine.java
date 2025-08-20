package com.journey.orchestrator.service;

import com.journey.orchestrator.model.JourneyStep.PersonalizationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Engine for applying personalization to content and actions.
 * 
 * Features:
 * - Dynamic content selection based on profile attributes
 * - A/B test variant selection
 * - Fallback value handling
 * - Expression-based variant selection
 * 
 * This mirrors journey orchestration platforms's personalization capabilities
 * for delivering relevant content to each customer.
 */
public class PersonalizationEngine {
    
    private static final Logger LOG = LoggerFactory.getLogger(PersonalizationEngine.class);
    
    private final ExpressionEvaluator expressionEvaluator;
    
    public PersonalizationEngine() {
        this.expressionEvaluator = new ExpressionEvaluator();
    }
    
    /**
     * Applies personalization to action parameters.
     */
    public Map<String, Object> applyPersonalization(Map<String, Object> parameters,
                                                     PersonalizationConfig config,
                                                     Map<String, Object> context) {
        if (config == null || parameters == null) {
            return parameters;
        }
        
        Map<String, Object> personalized = new HashMap<>(parameters);
        
        // Apply content variants
        if (config.getContentVariants() != null && !config.getContentVariants().isEmpty()) {
            String selectedVariant = selectVariant(config, context);
            
            // Find the parameter that should use variants
            for (Map.Entry<String, String> variant : config.getContentVariants().entrySet()) {
                if (variant.getValue().equals(selectedVariant)) {
                    personalized.put(variant.getKey(), variant.getValue());
                    break;
                }
            }
        }
        
        // Apply variant selector expression
        if (config.getVariantSelector() != null) {
            Object selectedValue = expressionEvaluator.evaluate(config.getVariantSelector(), context);
            personalized.put("selectedVariant", selectedValue);
        }
        
        // Apply fallback values for missing parameters
        if (config.getFallbackValues() != null) {
            for (Map.Entry<String, Object> fallback : config.getFallbackValues().entrySet()) {
                if (!personalized.containsKey(fallback.getKey()) || 
                    personalized.get(fallback.getKey()) == null) {
                    personalized.put(fallback.getKey(), fallback.getValue());
                    LOG.debug("Applied fallback value for: {}", fallback.getKey());
                }
            }
        }
        
        return personalized;
    }
    
    /**
     * Selects a content variant based on configuration and context.
     */
    private String selectVariant(PersonalizationConfig config, Map<String, Object> context) {
        // If there's a variant selector expression, use it
        if (config.getVariantSelector() != null) {
            Object result = expressionEvaluator.evaluate(config.getVariantSelector(), context);
            if (result != null) {
                return result.toString();
            }
        }
        
        // Default: select first variant (in production, this would do A/B testing logic)
        if (config.getContentVariants() != null && !config.getContentVariants().isEmpty()) {
            return config.getContentVariants().values().iterator().next();
        }
        
        return null;
    }
    
    /**
     * Personalizes a message template by replacing placeholders.
     */
    public String personalizeMessage(String template, Map<String, Object> context) {
        if (template == null || context == null) {
            return template;
        }
        
        String result = template;
        
        // Replace {{field}} placeholders with values from context
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (entry.getValue() != null) {
                result = result.replace(placeholder, entry.getValue().toString());
            }
        }
        
        // Handle nested fields (e.g., {{profile.firstName}})
        result = replaceNestedFields(result, context);
        
        return result;
    }
    
    /**
     * Replaces nested field placeholders.
     */
    private String replaceNestedFields(String template, Map<String, Object> context) {
        // Simple implementation - in production, use proper template engine
        return template.replaceAll("\\{\\{([^}]+)\\}\\}", matcher -> {
            String fieldPath = matcher.group(1);
            Object value = resolveFieldPath(fieldPath, context);
            return value != null ? value.toString() : matcher.group();
        });
    }
    
    /**
     * Resolves a field path against the context.
     */
    private Object resolveFieldPath(String path, Map<String, Object> context) {
        String[] parts = path.split("\\.");
        Object current = context;
        
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        
        return current;
    }
}
