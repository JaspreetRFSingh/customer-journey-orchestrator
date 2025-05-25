package com.journey.orchestrator.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a customer event that can trigger journey entry or progression.
 * Events are the primary input for real-time journey orchestration.
 */
@Data
@Builder
public class Event {
    
    private final String eventId;
    private final String eventType;
    private final String profileId;
    private final String customerId;
    private final Instant timestamp;
    private final Map<String, Object> payload;
    private final EventSource source;
    private final Map<String, String> context;
    private final EventPriority priority;
    
    public enum EventSource {
        WEB,
        MOBILE,
        API,
        BATCH,
        STREAM,
        THIRD_PARTY,
        INTERNAL
    }
    
    public enum EventPriority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
    
    /**
     * Common event types for customer journey tracking.
     */
    public static class EventTypes {
        public static final String PROFILE_CREATED = "profileCreated";
        public static final String PAGE_VIEW = "pageView";
        public static final String PRODUCT_VIEW = "productView";
        public static final String CART_ADD = "cartAdd";
        public static final String CART_REMOVE = "cartRemove";
        public static final String CHECKOUT_START = "checkoutStart";
        public static final String PURCHASE = "purchase";
        public static final String EMAIL_OPEN = "emailOpen";
        public static final String EMAIL_CLICK = "emailClick";
        public static final String EMAIL_BOUNCE = "emailBounce";
        public static final String FORM_SUBMIT = "formSubmit";
        public static final String SEARCH = "search";
        public static final String VIDEO_PLAY = "videoPlay";
        public static final String VIDEO_COMPLETE = "videoComplete";
        public static final String SUBSCRIPTION_START = "subscriptionStart";
        public static final String SUBSCRIPTION_CANCEL = "subscriptionCancel";
        public static final String SUPPORT_TICKET = "supportTicket";
        public static final String LOGIN = "login";
        public static final String LOGOUT = "logout";
    }
    
    /**
     * Factory method to create a new event.
     */
    public static Event create(String eventType, String profileId, String customerId, 
                               Map<String, Object> payload, EventSource source) {
        return Event.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(eventType)
            .profileId(profileId)
            .customerId(customerId)
            .timestamp(Instant.now())
            .payload(payload)
            .source(source)
            .context(Map.of())
            .priority(EventPriority.NORMAL)
            .build();
    }
    
    /**
     * Creates a purchase event with standard fields.
     */
    public static Event createPurchase(String profileId, String customerId, 
                                        double amount, String currency, String orderId) {
        return Event.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventTypes.PURCHASE)
            .profileId(profileId)
            .customerId(customerId)
            .timestamp(Instant.now())
            .payload(Map.of(
                "orderId", orderId,
                "amount", amount,
                "currency", currency,
                "timestamp", Instant.now().toEpochMilli()
            ))
            .source(EventSource.WEB)
            .context(Map.of())
            .priority(EventPriority.HIGH)
            .build();
    }
    
    /**
     * Creates a cart add event.
     */
    public static Event createCartAdd(String profileId, String customerId, 
                                       String productId, String productName, 
                                       double price, int quantity) {
        return Event.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(EventTypes.CART_ADD)
            .profileId(profileId)
            .customerId(customerId)
            .timestamp(Instant.now())
            .payload(Map.of(
                "productId", productId,
                "productName", productName,
                "price", price,
                "quantity", quantity,
                "cartValue", price * quantity
            ))
            .source(EventSource.WEB)
            .context(Map.of())
            .priority(EventPriority.NORMAL)
            .build();
    }
}
