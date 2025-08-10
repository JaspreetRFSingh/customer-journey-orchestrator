package com.journey.orchestrator.service;

import com.journey.orchestrator.model.Channel;
import com.journey.orchestrator.model.JourneyStep.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Email channel executor - simulates sending emails.
 * In production, this would integrate with an email service provider
 * like SendGrid, Amazon SES, or commercial email services.
 */
public class EmailChannelExecutor implements ChannelExecutorService.ChannelExecutor {
    
    private static final Logger LOG = LoggerFactory.getLogger(EmailChannelExecutor.class);
    
    @Override
    public ChannelExecutionResult execute(Action action, String profileId, Map<String, Object> context) {
        try {
            Map<String, Object> params = action.getParameters();
            
            String to = (String) params.get("to");
            String subject = (String) params.get("subject");
            String body = (String) params.get("body");
            String templateId = (String) params.get("templateId");
            
            // Simulate email sending
            LOG.info("Sending email to: {} | Subject: {} | Template: {}", 
                to, subject, templateId);
            
            // Simulate processing time
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Return simulated response
            return ChannelExecutionResult.success(Map.of(
                "messageId", "email-" + System.currentTimeMillis(),
                "status", "sent",
                "channel", "email"
            ));
            
        } catch (Exception e) {
            LOG.error("Error sending email", e);
            return ChannelExecutionResult.failure("Failed to send email", e);
        }
    }
    
    @Override
    public Channel getSupportedChannel() {
        return Channel.EMAIL;
    }
}
