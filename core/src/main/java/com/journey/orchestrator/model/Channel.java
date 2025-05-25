package com.journey.orchestrator.model;

/**
 * Communication channels for customer engagement.
 */
public enum Channel {
    EMAIL("Email", ChannelCategory.DIGITAL),
    SMS("SMS", ChannelCategory.DIGITAL),
    PUSH_NOTIFICATION("Push Notification", ChannelCategory.MOBILE),
    IN_APP_MESSAGE("In-App Message", ChannelCategory.MOBILE),
    WEB_PUSH("Web Push", ChannelCategory.DIGITAL),
    WHATSAPP("WhatsApp", ChannelCategory.MESSAGING),
    DIRECT_MAIL("Direct Mail", ChannelCategory.PHYSICAL),
    PHONE_CALL("Phone Call", ChannelCategory.PHYSICAL),
    WEB_PERSONALIZATION("Web Personalization", ChannelCategory.DIGITAL),
    MOBILE_PERSONALIZATION("Mobile Personalization", ChannelCategory.MOBILE),
    SOCIAL("Social Media", ChannelCategory.SOCIAL),
    DISPLAY_AD("Display Advertising", ChannelCategory.ADVERTISING),
    SEARCH_AD("Search Advertising", ChannelCategory.ADVERTISING),
    API("API Integration", ChannelCategory.INTEGRATION);
    
    private final String displayName;
    private final ChannelCategory category;
    
    Channel(String displayName, ChannelCategory category) {
        this.displayName = displayName;
        this.category = category;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public ChannelCategory getCategory() {
        return category;
    }
    
    public boolean isDigital() {
        return category == ChannelCategory.DIGITAL || 
               category == ChannelCategory.MOBILE ||
               category == ChannelCategory.MESSAGING;
    }
    
    public boolean isInstant() {
        return this == EMAIL || this == SMS || this == PUSH_NOTIFICATION || 
               this == IN_APP_MESSAGE || this == WHATSAPP;
    }
}
