package com.journey.orchestrator.service;

import com.journey.orchestrator.model.Channel;
import com.journey.orchestrator.model.JourneyStep.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Push notification channel executor.
 */
public class PushNotificationExecutor implements ChannelExecutorService.ChannelExecutor {
    
    private static final Logger LOG = LoggerFactory.getLogger(PushNotificationExecutor.class);
    
    @Override
    public ChannelExecutionResult execute(Action action, String profileId, Map<String, Object> context) {
        try {
            Map<String, Object> params = action.getParameters();
            String title = (String) params.get("title");
            String body = (String) params.get("body");
            String deepLink = (String) params.get("deepLink");
            
            LOG.info("Sending push notification | Title: {} | Body: {}", title, body);
            
            return ChannelExecutionResult.success(Map.of(
                "messageId", "push-" + System.currentTimeMillis(),
                "status", "sent",
                "channel", "push"
            ));
            
        } catch (Exception e) {
            return ChannelExecutionResult.failure("Failed to send push", e);
        }
    }
    
    @Override
    public Channel getSupportedChannel() {
        return Channel.PUSH_NOTIFICATION;
    }
}
