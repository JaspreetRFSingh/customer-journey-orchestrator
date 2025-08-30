package com.journey.orchestrator;

import com.journey.orchestrator.service.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Journey Orchestrator Application
 * 
 * A real-time customer journey orchestration engine demonstrating:
 * - Event-driven architecture
 * - Real-time profile management
 * - Multi-channel execution
 * - Cloud-native deployment
 * 
 * Built to showcase skills aligned with journey orchestration platforms team.
 */
@SpringBootApplication
@EnableScheduling
public class JourneyOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(JourneyOrchestratorApplication.class, args);
    }

    @Bean
    public ChannelExecutorService channelExecutorService() {
        return new ChannelExecutorService();
    }

    @Bean
    public JourneyOrchestrator journeyOrchestrator(ChannelExecutorService channelExecutorService) {
        return new JourneyOrchestrator(channelExecutorService);
    }

    @Bean
    public JourneyExecutionService journeyExecutionService(JourneyOrchestrator journeyOrchestrator) {
        return new JourneyExecutionService(journeyOrchestrator);
    }

    @Bean
    public EventRouter eventRouter(JourneyExecutionService journeyExecutionService) {
        return new EventRouter(journeyExecutionService);
    }

    @Bean
    public ProfileService profileService() {
        return new ProfileService();
    }

    @Bean
    public RealTimeEventProcessor eventProcessor(EventRouter eventRouter, ProfileService profileService) {
        return new RealTimeEventProcessor.Builder()
            .eventRouter(eventRouter)
            .profileService(profileService)
            .build();
    }
}
