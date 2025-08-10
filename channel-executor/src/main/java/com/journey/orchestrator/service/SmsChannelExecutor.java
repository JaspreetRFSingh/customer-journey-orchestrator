package com.journey.orchestrator.service;

import com.journey.orchestrator.model.Channel;
import com.journey.orchestrator.model.JourneyStep.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * SMS channel executor - simulates sending SMS messages.
 */
public class SmsChannelExecutor implements ChannelExecutorService.ChannelExecutor {
    
    private static final Logger LOG = LoggerFactory.getLogger(SmsChannelExecutor.class);
    
    @Override
    public ChannelExecutionResult execute(Action action, String profileId, Map<String, Object> context) {
        try {
            Map<String, Object> params = action.getParameters();
            String to = (String) params.get("to");
            String message = (String) params.get("message");
            
            LOG.info("Sending SMS to: {} | Message: {}", to, message);
            
            return ChannelExecutionResult.success(Map.of(
                "messageId", "sms-" + System.currentTimeMillis(),
                "status", "sent",
                "channel", "sms"
            ));
            
        } catch (Exception e) {
            return ChannelExecutionResult.failure("Failed to send SMS", e);
        }
    }
    
    @Override
    public Channel getSupportedChannel() {
        return Channel.SMS;
    }
}
