# Journey Orchestrator

A real-time customer journey orchestration engine demonstrating enterprise-grade architecture and cloud-native design patterns.

## 🎯 Overview

This project implements a simplified journey orchestration system for designing and executing cross-channel customer experiences, featuring:

- **Real-time Customer Profiles** - Unified customer data with computed traits and segmentation
- **Event-Driven Architecture** - High-throughput event processing with deduplication
- **Journey Orchestration** - Visual journey design with decision splits, waits, and goals
- **Multi-Channel Execution** - Email, SMS, Push, In-App, WhatsApp, Web Personalization
- **Cloud-Native Deployment** - Docker, Kubernetes, Terraform, CI/CD pipelines

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Journey Orchestrator                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────────────┐   │
│  │   REST API   │    │   Event API  │    │   Management API         │   │
│  │  (Journeys)  │    │  (Ingestion) │    │   (Profiles, Metrics)    │   │
│  └──────┬───────┘    └──────┬───────┘    └────────────┬─────────────┘   │
│         │                   │                         │                  │
│         ▼                   ▼                         ▼                  │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    Event Router                                   │   │
│  │         (Routes events to matching journeys)                      │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│         │                                                                │
│         ▼                                                                │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │              Real-Time Event Processor                            │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │   │
│  │  │Deduplicator │  │  Priority   │  │    Profile Service      │   │   │
│  │  │             │  │   Queues    │  │  (Caffeine Cache)       │   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│         │                                                                │
│         ▼                                                                │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │              Journey Orchestrator Engine                          │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │   │
│  │  │  Expression │  │   Step      │  │    Execution State      │   │   │
│  │  │  Evaluator  │  │  Executor   │  │    Manager              │   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│         │                                                                │
│         ▼                                                                │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │              Channel Executor Service                             │   │
│  │  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────────┐ ┌──────────┐ ┌──────────┐  │   │
│  │  │Email│ │ SMS │ │Push │ │In-App   │ │ WhatsApp │ │Web Pers. │  │   │
│  │  └─────┘ └─────┘ └─────┘ └─────────┘ └──────────┘ └──────────┘  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## 📦 Project Structure

```
journey-orchestrator/
├── core/                    # Domain models and core services
│   ├── model/               # CustomerProfile, Journey, Event, Expression
│   └── service/             # ExpressionEvaluator
├── event-processor/         # Real-time event processing
│   ├── RealTimeEventProcessor
│   ├── EventRouter
│   ├── ProfileService
│   └── MetricsCollector
├── orchestration-engine/    # Journey execution engine
│   └── JourneyOrchestrator
├── channel-executor/        # Multi-channel execution
│   ├── ChannelExecutorService
│   ├── PersonalizationEngine
│   └── Channel executors (Email, SMS, Push, etc.)
├── infrastructure/          # Application and APIs
│   ├── JourneyOrchestratorApplication
│   └── REST Controllers
├── docker/                  # Docker configuration
├── k8s/                     # Kubernetes manifests
├── terraform/               # Infrastructure as Code
└── .github/workflows/       # CI/CD pipelines
```

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker (optional)
- Kubernetes cluster (optional)

### Build and Run

```bash
# Clone the repository
git clone <repository-url>
cd journey-orchestrator

# Build the project
mvn clean install

# Run the application
cd infrastructure
mvn spring-boot:run

# Or run the JAR
java -jar infrastructure/target/infrastructure-1.0.0-SNAPSHOT.jar
```

### Using Docker

```bash
# Build Docker image
docker build -f docker/Dockerfile -t journey-orchestrator .

# Run with Docker Compose
cd docker
docker-compose up -d
```

### Access the Application

- **REST API**: http://localhost:8080/api/v1
- **Actuator Health**: http://localhost:8080/actuator/health
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus
- **Grafana Dashboard**: http://localhost:3000 (admin/admin)

## 📡 API Reference

### Journeys API

```bash
# Create a journey
POST /api/v1/journeys
Content-Type: application/json

{
  "journeyId": "welcome-journey",
  "name": "Welcome Journey",
  "status": "ACTIVE",
  "trigger": {
    "type": "EVENT_BASED",
    "eventType": "profileCreated"
  },
  "steps": [...]
}

# List all journeys
GET /api/v1/journeys

# Get journey by ID
GET /api/v1/journeys/{journeyId}
```

### Events API

```bash
# Ingest an event
POST /api/v1/events
Content-Type: application/json

{
  "eventType": "purchase",
  "profileId": "user-123",
  "customerId": "cust-456",
  "payload": {
    "amount": 99.99,
    "currency": "USD",
    "orderId": "ORD-789"
  },
  "source": "WEB"
}

# Batch ingest events
POST /api/v1/events/batch
```

### Profiles API

```bash
# Create a profile
POST /api/v1/profiles
Content-Type: application/json

{
  "customerId": "cust-456",
  "identity": {
    "email": "user@example.com",
    "phone": "+1234567890"
  },
  "attributes": {
    "firstName": "John",
    "lastName": "Doe"
  }
}

# Get profile
GET /api/v1/profiles/{profileId}

# Update attribute
PATCH /api/v1/profiles/{profileId}/attributes
```

## 🧪 Example Journey

Here's an example of a cart abandonment journey:

```java
// Create trigger
Journey.JourneyTrigger trigger = Journey.JourneyTrigger.builder()
    .type(Journey.TriggerType.EVENT_BASED)
    .eventType(Event.EventTypes.CART_ADD)
    .build();

// Create steps
JourneyStep startStep = JourneyStep.builder()
    .stepId("start")
    .type(JourneyStep.StepType.START)
    .build();

JourneyStep waitStep = JourneyStep.builder()
    .stepId("wait-1hour")
    .type(JourneyStep.StepType.WAIT)
    .waitDuration(Duration.ofHours(1))
    .build();

JourneyStep decisionStep = JourneyStep.builder()
    .stepId("check-purchase")
    .type(JourneyStep.StepType.DECISION)
    .entryCondition(Expression.equals("hasPurchased", true))
    .build();

JourneyStep.Action emailAction = JourneyStep.Action.builder()
    .actionId("send-abandon-email")
    .type(JourneyStep.ActionType.SEND_MESSAGE)
    .channel(Channel.EMAIL)
    .parameters(Map.of(
        "templateId", "cart-abandonment",
        "subject", "Forgot something?"
    ))
    .build();

JourneyStep emailStep = JourneyStep.builder()
    .stepId("send-email")
    .type(JourneyStep.StepType.ACTION)
    .actions(List.of(emailAction))
    .build();

JourneyStep endStep = JourneyStep.builder()
    .stepId("end")
    .type(JourneyStep.StepType.END)
    .build();

// Create journey
Journey journey = Journey.create(
    "Cart Abandonment Journey",
    "Re-engage users who added items to cart",
    trigger,
    List.of(startStep, waitStep, decisionStep, emailStep, endStep),
    Journey.JourneyConfig.builder()
        .maxStepsPerProfile(10)
        .journeyDurationHours(24)
        .build()
);
```

## ☁️ Cloud Deployment

### Kubernetes

```bash
# Apply namespace
kubectl apply -f k8s/namespace.yaml

# Apply deployment
kubectl apply -f k8s/deployment.yaml

# Check status
kubectl get pods -n journey-orchestrator
```

### Terraform (AWS)

```bash
# Initialize Terraform
cd terraform
terraform init

# Plan deployment
terraform plan -var="environment=prod"

# Apply infrastructure
terraform apply -var="environment=prod"
```

## 📊 Observability

### Metrics

The application exposes Prometheus metrics at `/actuator/prometheus`:

- `journey_events_processed_total` - Total events processed
- `journey_executions_active` - Active journey executions
- `journey_actions_executed_total` - Actions executed by channel
- `profile_cache_hits_total` - Profile cache hits
- `event_processing_duration_seconds` - Event processing latency

### Health Checks

- `/actuator/health` - Overall health
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify -Pintegration-tests

# Generate coverage report
mvn clean test jacoco:report

# View coverage report
open infrastructure/target/site/jacoco/index.html
```

## 🔒 Security Features

- Non-root container user
- Read-only root filesystem
- Resource limits and requests
- Network policies
- Secrets management via Kubernetes
- Dependency vulnerability scanning (OWASP, Trivy)

## 📈 Performance Characteristics

| Metric | Target |
|--------|--------|
| Event throughput | 10,000 events/sec |
| Profile lookup latency | < 1ms (cached) |
| Journey execution latency | < 100ms per step |
| Event deduplication window | 5 minutes |
| Profile cache size | 100,000 profiles |

## 🛠️ Technologies Used

| Category | Technology |
|----------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Build | Maven |
| Caching | Caffeine |
| Container | Docker |
| Orchestration | Kubernetes |
| IaC | Terraform |
| CI/CD | GitHub Actions |
| Monitoring | Prometheus, Grafana |

## 🎓 Key Concepts Demonstrated

1. **Domain-Driven Design** - Rich domain models with business logic
2. **Event Sourcing Patterns** - Event-driven state transitions
3. **CQRS** - Separate read/write models for profiles
4. **Strategy Pattern** - Pluggable channel executors
5. **Chain of Responsibility** - Journey step execution
6. **Circuit Breaker** - Fault-tolerant channel execution
7. **Rate Limiting** - Token bucket algorithm
8. **Expression Evaluation** - DSL for journey conditions

## 📝 License

MIT License - See LICENSE file for details.

## 👤 Author

Built as a portfolio project demonstrating enterprise-grade journey orchestration capabilities.
# Performance Benchmarks

## Security Best Practices
- Non-root container user
- Read-only root filesystem
- Resource limits

## Troubleshooting

### Common Issues
- Memory limits: Adjust JAVA_OPTS MaxRAMPercentage
- Connection timeouts: Check Kubernetes network policies

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.

## Performance Benchmarks

| Metric | Target |
|--------|--------|
| Event throughput | 10,000 events/sec |
| Profile lookup latency | < 1ms (cached) |
| Journey execution latency | < 100ms per step |

## Security Best Practices
- Non-root container user
- Read-only root filesystem
- Resource limits and requests
- Dependency vulnerability scanning

## Troubleshooting

### Common Issues
- Memory limits: Adjust JAVA_OPTS MaxRAMPercentage
- Connection timeouts: Check Kubernetes network policies

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.

---

*Last updated: April 2026*
