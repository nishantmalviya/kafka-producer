# Deployment Guide for Spring Boot Kafka Producer

## Table of Contents

1. [Local Development](#local-development)
2. [Docker Deployment](#docker-deployment)
3. [Kubernetes Deployment](#kubernetes-deployment)
4. [Production Deployment](#production-deployment)
5. [Performance Tuning](#performance-tuning)
6. [Monitoring and Logging](#monitoring-and-logging)

---

## Local Development

### Prerequisites

- Java 11 or higher
- Maven 3.6+
- Kafka broker running locally

### Setup Steps

1. **Clone/Download the project**
   ```bash
   cd /Users/nishant/work/Kafka
   ```

2. **Configure application.properties**
   ```bash
   cp application.properties.example src/main/resources/application.properties
   # Edit the file with your Kafka details
   ```

3. **Build the application**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

5. **Test the API**
   ```bash
   curl -X POST http://localhost:8080/api/v1/kafka/publish \
     -H "Content-Type: application/json" \
     -d '{"message":"Hello Kafka","key":"test-key"}'
   ```

### Local Kafka Setup with Docker

```bash
# Start Kafka with Docker Compose
docker-compose up -d

# Verify Kafka is running
docker ps

# Check Kafka logs
docker logs kafka

# Create a topic (if needed)
docker exec kafka kafka-topics --create \
  --topic my-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

---

## Docker Deployment

### Building Docker Image

Create `Dockerfile`:

```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the built JAR from target directory
COPY target/kafka-producer-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/kafka/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Building and Running

```bash
# Build Docker image
docker build -t kafka-producer:1.0.0 .

# Run the container
docker run -d \
  --name kafka-producer \
  -p 8080:8080 \
  -e KAFKA_JAAS_CONFIG='org.apache.kafka.common.security.plain.PlainLoginModule required username="producer" password="producer-secret";' \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9093 \
  -e SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL=SASL_PLAINTEXT \
  -e SPRING_KAFKA_PROPERTIES_SASL_MECHANISM=PLAIN \
  --network kafka-network \
  kafka-producer:1.0.0
```

### Docker Compose with Kafka Producer

```yaml
version: '3.8'

services:
  # ... existing kafka and zookeeper services ...

  kafka-producer:
    build: .
    container_name: kafka-producer
    ports:
      - "8080:8080"
    depends_on:
      - kafka
    environment:
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9093
      SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL: SASL_PLAINTEXT
      SPRING_KAFKA_PROPERTIES_SASL_MECHANISM: PLAIN
      SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG: 'org.apache.kafka.common.security.plain.PlainLoginModule required username="producer" password="producer-secret";'
      KAFKA_TOPIC_NAME: my-topic
      LOGGING_LEVEL_COM_EXAMPLE: DEBUG
    networks:
      - kafka-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/v1/kafka/health"]
      interval: 10s
      timeout: 5s
      retries: 5
```

---

## Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (1.16+)
- kubectl configured
- Docker registry access

### Step 1: Build and Push Image

```bash
# Build Docker image
docker build -t your-registry/kafka-producer:1.0.0 .

# Push to registry
docker push your-registry/kafka-producer:1.0.0
```

### Step 2: Create Kubernetes Namespace

```bash
kubectl create namespace kafka-producer
```

### Step 3: Create ConfigMap for application.properties

```bash
kubectl create configmap kafka-producer-config \
  --from-file=src/main/resources/application.properties \
  -n kafka-producer
```

### Step 4: Create Secret for Credentials

```bash
kubectl create secret generic kafka-credentials \
  --from-literal=username=producer \
  --from-literal=password=producer-secret \
  --from-literal=jaas-config='org.apache.kafka.common.security.plain.PlainLoginModule required username="producer" password="producer-secret";' \
  -n kafka-producer
```

### Step 5: Create Deployment YAML

Create `kafka-producer-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-producer
  namespace: kafka-producer
  labels:
    app: kafka-producer
spec:
  replicas: 3
  selector:
    matchLabels:
      app: kafka-producer
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: kafka-producer
    spec:
      containers:
      - name: kafka-producer
        image: your-registry/kafka-producer:1.0.0
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8080
          name: http
          protocol: TCP
        
        env:
        - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
          value: "kafka-broker.kafka:9093"
        - name: SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL
          value: "SASL_SSL"
        - name: SPRING_KAFKA_PROPERTIES_SASL_MECHANISM
          value: "PLAIN"
        - name: SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG
          valueFrom:
            secretKeyRef:
              name: kafka-credentials
              key: jaas-config
        - name: SPRING_KAFKA_PROPERTIES_SSL_TRUSTSTORE_LOCATION
          value: "file:/etc/kafka/secrets/truststore.jks"
        - name: SPRING_KAFKA_PROPERTIES_SSL_TRUSTSTORE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ssl-certificates
              key: truststore-password
        - name: KAFKA_TOPIC_NAME
          value: "production-topic"
        - name: LOGGING_LEVEL_COM_EXAMPLE
          value: "DEBUG"
        
        # Liveness probe
        livenessProbe:
          httpGet:
            path: /api/v1/kafka/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        # Readiness probe
        readinessProbe:
          httpGet:
            path: /api/v1/kafka/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        # Resource requests and limits
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        
        # Volume mounts for certificates
        volumeMounts:
        - name: kafka-certs
          mountPath: /etc/kafka/secrets
          readOnly: true
      
      volumes:
      - name: kafka-certs
        secret:
          secretName: kafka-ssl-certs
      
      # Pod disruption budget
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - kafka-producer
              topologyKey: kubernetes.io/hostname

---
apiVersion: v1
kind: Service
metadata:
  name: kafka-producer-service
  namespace: kafka-producer
  labels:
    app: kafka-producer
spec:
  type: LoadBalancer
  selector:
    app: kafka-producer
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
    name: http
```

### Step 6: Deploy

```bash
# Create SSL certificates secret
kubectl create secret generic kafka-ssl-certs \
  --from-file=truststore.jks=/path/to/truststore.jks \
  -n kafka-producer

# Create password secret
kubectl create secret generic ssl-certificates \
  --from-literal=truststore-password=your_password \
  -n kafka-producer

# Deploy the application
kubectl apply -f kafka-producer-deployment.yaml

# Check deployment status
kubectl get deployment -n kafka-producer
kubectl get pods -n kafka-producer
kubectl logs -f deployment/kafka-producer -n kafka-producer
```

---

## Production Deployment

### Checklist

- [ ] SSL/TLS certificates are valid and not expired
- [ ] JAAS credentials are stored in secrets management system
- [ ] Application is configured for high availability (multiple replicas)
- [ ] Monitoring and alerting are in place
- [ ] Backup strategy is defined
- [ ] Disaster recovery plan is documented
- [ ] Load testing has been completed
- [ ] Security audit has been passed

### Production Configuration Best Practices

```properties
# High Availability
spring.kafka.producer.acks=all
spring.kafka.producer.retries=5
spring.kafka.producer.properties.min.insync.replicas=2
spring.kafka.producer.properties.delivery.timeout.ms=180000

# Performance
spring.kafka.producer.properties.linger.ms=10
spring.kafka.producer.properties.batch.size=32768
spring.kafka.producer.compression-type=snappy

# Security
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=SCRAM-SHA-512
spring.kafka.properties.ssl.enabled.protocols=TLSv1.2,TLSv1.3

# Timeouts
spring.kafka.producer.properties.request.timeout.ms=30000
spring.kafka.properties.connections.max.idle.ms=540000
```

### Deployment Commands

```bash
# Build with optimizations
mvn clean install -DskipTests -Dorg.slf4j.simpleLogger.defaultLogLevel=warn

# Build Docker image with optimizations
docker build -t kafka-producer:1.0.0 --build-arg JAR_FILE=target/*.jar .

# Push to production registry
docker push your-prod-registry/kafka-producer:1.0.0

# Deploy to production
kubectl set image deployment/kafka-producer \
  kafka-producer=your-prod-registry/kafka-producer:1.0.0 \
  -n kafka-producer

# Verify rollout
kubectl rollout status deployment/kafka-producer -n kafka-producer
```

---

## Performance Tuning

### Producer Configuration

```properties
# For High Throughput
spring.kafka.producer.acks=1
spring.kafka.producer.properties.linger.ms=50
spring.kafka.producer.properties.batch.size=65536
spring.kafka.producer.compression-type=lz4

# For Low Latency
spring.kafka.producer.acks=1
spring.kafka.producer.properties.linger.ms=0
spring.kafka.producer.properties.batch.size=1024
spring.kafka.producer.compression-type=none

# For Maximum Durability
spring.kafka.producer.acks=all
spring.kafka.producer.retries=5
spring.kafka.producer.properties.min.insync.replicas=2
spring.kafka.producer.properties.delivery.timeout.ms=300000
```

### JVM Tuning

```bash
# In Dockerfile or deployment
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

---

## Monitoring and Logging

### Prometheus Metrics

Add Micrometer dependency to `pom.xml`:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Configure in `application.properties`:

```properties
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

### ELK Stack Integration

```yaml
# Logback configuration for ELK
logging.pattern.console=%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ} [%thread] %-5level %logger{36} - %msg%n
logging.level.com.example.kafka=DEBUG
```

### Sample Queries

```bash
# Check message throughput
curl http://localhost:8080/actuator/metrics/kafka.producer.records.sent

# View all metrics
curl http://localhost:8080/actuator/prometheus
```

---

## References

- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Kubernetes Deployment Guide](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
- [Apache Kafka Deployment Best Practices](https://kafka.apache.org/documentation/#bestpractices)
