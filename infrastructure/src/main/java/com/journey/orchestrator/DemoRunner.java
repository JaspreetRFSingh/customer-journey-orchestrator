package com.journey.orchestrator;

import com.journey.orchestrator.model.*;
import com.journey.orchestrator.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Demo runner that showcases the journey orchestrator capabilities.
 * Creates sample journeys and simulates customer events.
 */
@Component
public class DemoRunner implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoRunner.class);

    private final RealTimeEventProcessor eventProcessor;
    private final ProfileService profileService;
    private final EventRouter eventRouter;
    private final JourneyOrchestrator journeyOrchestrator;

    public DemoRunner(RealTimeEventProcessor eventProcessor,
                      ProfileService profileService,
                      EventRouter eventRouter,
                      JourneyOrchestrator journeyOrchestrator) {
        this.eventProcessor = eventProcessor;
        this.profileService = profileService;
        this.eventRouter = eventRouter;
        this.journeyOrchestrator = journeyOrchestrator;
    }

    @Override
    public void run(String... args) throws Exception {
        LOG.info("=== Journey Orchestrator Demo Starting ===");

        // Create sample journeys
        createWelcomeJourney();
        createCartAbandonmentJourney();
        createHighValueCustomerJourney();

        // Create sample customer profiles
        createSampleProfiles();

        // Simulate customer events
        simulateCustomerEvents();

        LOG.info("=== Demo Complete ===");
        LOG.info("Check logs above for journey execution details");
        LOG.info("API available at: http://localhost:8080/api/v1");
    }

    private void createWelcomeJourney() {
        LOG.info("Creating Welcome Journey...");

        // Trigger: Profile created
        Journey.JourneyTrigger trigger = Journey.JourneyTrigger.builder()
            .type(Journey.TriggerType.EVENT_BASED)
            .eventType(Event.EventTypes.PROFILE_CREATED)
            .build();

        // Steps
        JourneyStep startStep = JourneyStep.builder()
            .stepId("start")
            .name("Start")
            .type(JourneyStep.StepType.START)
            .build();

        JourneyStep.Action welcomeEmail = JourneyStep.Action.builder()
            .actionId("send-welcome-email")
            .type(JourneyStep.ActionType.SEND_MESSAGE)
            .channel(Channel.EMAIL)
            .parameters(Map.of(
                "templateId", "welcome-email",
                "subject", "Welcome to our platform!",
                "body", "Hi {{profile.firstName}}, welcome aboard!"
            ))
            .build();

        JourneyStep welcomeStep = JourneyStep.builder()
            .stepId("send-welcome")
            .name("Send Welcome Email")
            .type(JourneyStep.StepType.ACTION)
            .actions(List.of(welcomeEmail))
            .build();

        JourneyStep waitStep = JourneyStep.builder()
            .stepId("wait-1-day")
            .name("Wait 1 Day")
            .type(JourneyStep.StepType.WAIT)
            .waitDuration(Duration.ofSeconds(5)) // Shortened for demo
            .build();

        JourneyStep.Action onboardingEmail = JourneyStep.Action.builder()
            .actionId("send-onboarding-email")
            .type(JourneyStep.ActionType.SEND_MESSAGE)
            .channel(Channel.EMAIL)
            .parameters(Map.of(
                "templateId", "onboarding-email",
                "subject", "Getting Started Guide"
            ))
            .build();

        JourneyStep onboardingStep = JourneyStep.builder()
            .stepId("send-onboarding")
            .name("Send Onboarding Email")
            .type(JourneyStep.StepType.ACTION)
            .actions(List.of(onboardingEmail))
            .build();

        JourneyStep endStep = JourneyStep.builder()
            .stepId("end")
            .name("End")
            .type(JourneyStep.StepType.END)
            .build();

        Journey journey = Journey.create(
            "Welcome Journey",
            "Onboard new customers with a welcome series",
            trigger,
            List.of(startStep, welcomeStep, waitStep, onboardingStep, endStep),
            Journey.JourneyConfig.builder()
                .maxStepsPerProfile(10)
                .journeyDurationHours(48)
                .allowReEntry(false)
                .build()
        );

        journeyOrchestrator.registerJourney(journey);
        eventRouter.registerJourney(journey);
        LOG.info("Welcome Journey created: {}", journey.getJourneyId());
    }

    private void createCartAbandonmentJourney() {
        LOG.info("Creating Cart Abandonment Journey...");

        Journey.JourneyTrigger trigger = Journey.JourneyTrigger.builder()
            .type(Journey.TriggerType.EVENT_BASED)
            .eventType(Event.EventTypes.CART_ADD)
            .build();

        JourneyStep startStep = JourneyStep.builder()
            .stepId("start")
            .name("Start")
            .type(JourneyStep.StepType.START)
            .build();

        // Wait 1 hour for purchase
        JourneyStep waitStep = JourneyStep.builder()
            .stepId("wait-1-hour")
            .name("Wait 1 Hour")
            .type(JourneyStep.StepType.WAIT)
            .waitDuration(Duration.ofSeconds(5)) // Shortened for demo
            .build();

        // Decision: Did they purchase?
        JourneyStep decisionStep = JourneyStep.builder()
            .stepId("check-purchase")
            .name("Check if Purchased")
            .type(JourneyStep.StepType.DECISION)
            .transitions(List.of(
                JourneyStep.Transition.builder()
                    .transitionId("purchased")
                    .targetStepId("end-purchased")
                    .condition(Expression.equals("hasPurchased", true))
                    .priority(1)
                    .build(),
                JourneyStep.Transition.builder()
                    .transitionId("not-purchased")
                    .targetStepId("send-reminder")
                    .condition(Expression.equals("hasPurchased", false))
                    .priority(2)
                    .build()
            ))
            .build();

        JourneyStep.Action reminderEmail = JourneyStep.Action.builder()
            .actionId("send-cart-reminder")
            .type(JourneyStep.ActionType.SEND_MESSAGE)
            .channel(Channel.EMAIL)
            .parameters(Map.of(
                "templateId", "cart-reminder",
                "subject", "Forgot something in your cart?"
            ))
            .build();

        JourneyStep reminderStep = JourneyStep.builder()
            .stepId("send-reminder")
            .name("Send Cart Reminder")
            .type(JourneyStep.StepType.ACTION)
            .actions(List.of(reminderEmail))
            .build();

        JourneyStep endPurchasedStep = JourneyStep.builder()
            .stepId("end-purchased")
            .name("End (Purchased)")
            .type(JourneyStep.StepType.END)
            .build();

        JourneyStep endStep = JourneyStep.builder()
            .stepId("end")
            .name("End")
            .type(JourneyStep.StepType.END)
            .build();

        Journey journey = Journey.create(
            "Cart Abandonment Journey",
            "Re-engage users who added items to cart but didn't purchase",
            trigger,
            List.of(startStep, waitStep, decisionStep, reminderStep, endStep, endPurchasedStep),
            Journey.JourneyConfig.builder()
                .maxStepsPerProfile(5)
                .journeyDurationHours(24)
                .build()
        );

        journeyOrchestrator.registerJourney(journey);
        eventRouter.registerJourney(journey);
        LOG.info("Cart Abandonment Journey created: {}", journey.getJourneyId());
    }

    private void createHighValueCustomerJourney() {
        LOG.info("Creating High Value Customer Journey...");

        Journey.JourneyTrigger trigger = Journey.JourneyTrigger.builder()
            .type(Journey.TriggerType.AUDIENCE_BASED)
            .eventType(Event.EventTypes.PURCHASE)
            .condition(Expression.greaterThan("profile.totalSpend", 1000))
            .build();

        JourneyStep startStep = JourneyStep.builder()
            .stepId("start")
            .name("Start")
            .type(JourneyStep.StepType.START)
            .build();

        JourneyStep.Action vipEmail = JourneyStep.Action.builder()
            .actionId("send-vip-offer")
            .type(JourneyStep.ActionType.SEND_MESSAGE)
            .channel(Channel.EMAIL)
            .parameters(Map.of(
                "templateId", "vip-exclusive",
                "subject", "Exclusive VIP Offer Just for You"
            ))
            .build();

        JourneyStep vipStep = JourneyStep.builder()
            .stepId("send-vip-offer")
            .name("Send VIP Offer")
            .type(JourneyStep.StepType.ACTION)
            .actions(List.of(vipEmail))
            .build();

        JourneyStep waitStep = JourneyStep.builder()
            .stepId("wait-2-days")
            .name("Wait 2 Days")
            .type(JourneyStep.StepType.WAIT)
            .waitDuration(Duration.ofSeconds(3))
            .build();

        JourneyStep.Action smsFollowup = JourneyStep.Action.builder()
            .actionId("send-sms-followup")
            .type(JourneyStep.ActionType.SEND_MESSAGE)
            .channel(Channel.SMS)
            .parameters(Map.of(
                "message", "Hi! Your VIP offer expires soon. Don't miss out!"
            ))
            .build();

        JourneyStep smsStep = JourneyStep.builder()
            .stepId("send-sms")
            .name("Send SMS Follow-up")
            .type(JourneyStep.StepType.ACTION)
            .actions(List.of(smsFollowup))
            .build();

        JourneyStep endStep = JourneyStep.builder()
            .stepId("end")
            .name("End")
            .type(JourneyStep.StepType.END)
            .build();

        Journey journey = Journey.create(
            "High Value Customer Journey",
            "Exclusive engagement for high-value customers",
            trigger,
            List.of(startStep, vipStep, waitStep, smsStep, endStep),
            Journey.JourneyConfig.builder()
                .priority(Journey.Priority.HIGH)
                .build()
        );

        journeyOrchestrator.registerJourney(journey);
        eventRouter.registerJourney(journey);
        LOG.info("High Value Customer Journey created: {}", journey.getJourneyId());
    }

    private void createSampleProfiles() {
        LOG.info("Creating sample customer profiles...");

        CustomerProfile profile1 = profileService.createProfile(
            "customer-001",
            CustomerProfile.Identity.builder()
                .email("alice@example.com")
                .firstName("Alice")
                .lastName("Johnson")
                .build(),
            Map.of(
                "firstName", "Alice",
                "lastName", "Johnson",
                "age", 28,
                "city", "New York"
            )
        );
        LOG.info("Created profile: {} - {}", profile1.getProfileId(), profile1.getIdentity().getEmail());

        CustomerProfile profile2 = profileService.createProfile(
            "customer-002",
            CustomerProfile.Identity.builder()
                .email("bob@example.com")
                .build(),
            Map.of(
                "firstName", "Bob",
                "lastName", "Smith",
                "age", 35,
                "city", "San Francisco"
            )
        );
        LOG.info("Created profile: {} - {}", profile2.getProfileId(), profile2.getIdentity().getEmail());
    }

    private void simulateCustomerEvents() {
        LOG.info("Simulating customer events...");

        // Profile created event (triggers Welcome Journey)
        Event profileCreated = Event.create(
            Event.EventTypes.PROFILE_CREATED,
            "profile-001",
            "customer-001",
            Map.of("source", "web-signup"),
            Event.EventSource.WEB
        );
        eventProcessor.submitEvent(profileCreated);
        LOG.info("Submitted event: {}", profileCreated.getEventType());

        // Cart add event (triggers Cart Abandonment Journey)
        Event cartAdd = Event.createCartAdd(
            "profile-001",
            "customer-001",
            "PROD-001",
            "Premium Headphones",
            299.99,
            1
        );
        eventProcessor.submitEvent(cartAdd);
        LOG.info("Submitted event: {}", cartAdd.getEventType());

        // Purchase event
        Event purchase = Event.createPurchase(
            "profile-002",
            "customer-002",
            1250.00,
            "USD",
            "ORD-12345"
        );
        eventProcessor.submitEvent(purchase);
        LOG.info("Submitted event: {}", purchase.getEventType());

        // Wait for async processing
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Print metrics
        var metrics = eventProcessor.getMetrics();
        LOG.info("=== Processing Metrics ===");
        LOG.info("Events Submitted: {}", metrics.eventsSubmitted());
        LOG.info("Events Processed: {}", metrics.eventsProcessed());
        LOG.info("Duplicates: {}", metrics.duplicates());
        LOG.info("Errors: {}", metrics.errors());
    }
}
