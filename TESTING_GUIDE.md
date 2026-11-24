# Complete Testing Guide for Kafka Producer

## Overview

This guide provides comprehensive instructions for testing the Spring Boot Kafka Producer application with various scenarios.

## Prerequisites

- Java 17+ or Docker
- Kafka broker running
- curl or Postman for API testing
- Python 3 (optional, for advanced testing)

## Table of Contents

1. [Quick Local Testing](#quick-local-testing)
2. [API Testing with cURL](#api-testing-with-curl)
3. [Docker Testing](#docker-testing)
4. [Performance Testing](#performance-testing)
5. [Advanced Testing Scenarios](#advanced-testing-scenarios)
6. [Troubleshooting Tests](#troubleshooting-tests)

---

## Quick Local Testing

### Step 1: Start Kafka (Using Docker Compose)

```bash
# Navigate to project directory
cd /Users/nishant/work/Kafka

# Start Kafka and Zookeeper
docker-compose up -d

# Wait for services to be ready (30-60 seconds)
docker-compose ps

# Check logs
docker-compose logs kafka
```

### Step 2: Configure Application

```bash
# Edit application.properties with your Kafka details
nano src/main/resources/application.properties

# Minimal configuration for local testing:
spring.kafka.bootstrap-servers=localhost:9092
kafka.topic.name=my-topic
spring.kafka.producer.acks=all
```

### Step 3: Build and Run the Application

```bash
# Build
mvn clean install -DskipTests

# Run
java -jar target/kafka-producer-1.0.0.jar

# Or using Maven
mvn spring-boot:run

# Application should start on http://localhost:8080
```

### Step 4: Verify Service is Running

```bash
# Check health endpoint
curl http://localhost:8080/api/v1/kafka/health

# Expected response:
# Kafka Producer Service is running
```

---

## API Testing with cURL

### Test 1: Publish Single Message

**Simple message:**
```bash
curl -X POST http://localhost:8080/api/v1/kafka/publish \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello, Kafka!"}'
```

**Message with key:**
```bash
curl -X POST http://localhost:8080/api/v1/kafka/publish \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello with key","key":"my-key-123"}'
```

**Expected Response:**
```json
{
  "status": "SUCCESS",
  "message": "Message sent successfully",
  "timeTakenMs": 45,
  "timestamp": 1700000000000,
  "offset": 0,
  "partition": 0,
  "topic": "my-topic"
}
```

### Test 2: Publish Batch Messages

```bash
curl -X POST http://localhost:8080/api/v1/kafka/publish-batch \
  -H "Content-Type: application/json" \
  -d '[
    {"message":"Batch message 1","key":"batch-1"},
    {"message":"Batch message 2","key":"batch-2"},
    {"message":"Batch message 3","key":"batch-3"}
  ]'
```

**Expected Response:**
```json
{
  "totalMessages": 3,
  "totalTimeTakenMs": 120,
  "responses": [
    {
      "status": "SUCCESS",
      "message": "Message sent successfully",
      "timeTakenMs": 40,
      ...
    },
    ...
  ]
}
```

### Test 3: Error Cases

**Empty message (should fail):**
```bash
curl -X POST http://localhost:8080/api/v1/kafka/publish \
  -H "Content-Type: application/json" \
  -d '{"message":""}'
```

**Expected Response (400):**
```json
{
  "status": "INVALID_REQUEST",
  "message": "Message cannot be empty",
  "timeTakenMs": 0
}
```

**Null message:**
```bash
curl -X POST http://localhost:8080/api/v1/kafka/publish \
  -H "Content-Type: application/json" \
  -d '{"key":"test"}'
```

### Test 4: Health Check

```bash
curl -v http://localhost:8080/api/v1/kafka/health
```

---

## Docker Testing

### Start Everything with Docker

```bash
# Build the application Docker image
docker build -t kafka-producer:1.0.0 .

# Start Kafka and application together
docker-compose up -d

# Wait for services to start
sleep 10

# Check if application is running
docker ps

# View application logs
docker logs kafka-producer

# Test the API
curl http://localhost:8080/api/v1/kafka/health
```

### Test from Docker Container

```bash
# Access application container
docker exec -it kafka-producer bash

# From inside container, test Kafka connectivity
kafka-console-consumer --bootstrap-server kafka:9092 --topic my-topic --from-beginning
```

### Stop Docker Services

```bash
# Stop all services
docker-compose down

# Remove volumes (clean slate)
docker-compose down -v

# View logs before cleanup
docker-compose logs
```

---

## Performance Testing

### Test 1: Single Message Performance

Measure average time for single messages:

```bash
# Send 10 single messages and collect timing
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/v1/kafka/publish \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"Performance test message $i\",\"key\":\"perf-$i\"}" | \
    jq '.timeTakenMs'
done
```

### Test 2: Batch Performance

```bash
# Generate batch of 100 messages
messages='['
for i in {1..100}; do
  if [ $i -eq 1 ]; then
    messages="${messages}{\"message\":\"Batch message $i\",\"key\":\"batch-$i\"}"
  else
    messages="${messages},{\"message\":\"Batch message $i\",\"key\":\"batch-$i\"}"
  fi
done
messages="${messages}]"

# Send batch
curl -X POST http://localhost:8080/api/v1/kafka/publish-batch \
  -H "Content-Type: application/json" \
  -d "$messages" | jq '.totalTimeTakenMs'
```

### Test 3: Throughput Test

**Using Apache Bench (ab):**
```bash
# Install Apache Bench
# brew install httpd  (macOS)
# apt-get install apache2-utils  (Linux)

# Run 100 requests with 10 concurrent connections
ab -n 100 -c 10 -p request.json -T application/json http://localhost:8080/api/v1/kafka/publish

# Where request.json contains:
# {"message":"Test message","key":"test-key"}
```

**Using wrk:**
```bash
# Install wrk
# brew install wrk  (macOS)
# apt-get install wrk  (Linux)

# Create Lua script (script.lua):
cat > script.lua << 'EOF'
request = function()
   wrk.method = "POST"
   wrk.headers["Content-Type"] = "application/json"
   wrk.body = '{"message":"Test message ' .. math.random(1, 1000) .. '","key":"key-' .. math.random(1, 100) .. '"}'
   return wrk.format(nil)
end
EOF

# Run test
wrk -t4 -c10 -d30s -s script.lua http://localhost:8080/api/v1/kafka/publish
```

---

## Advanced Testing Scenarios

### Scenario 1: Test with JAAS Authentication

**Update application.properties:**
```properties
spring.kafka.properties.security.protocol=SASL_PLAINTEXT
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="producer" password="producer-secret";
```

**Test:**
```bash
# Restart application
mvn spring-boot:run

# Try to publish message
curl -X POST http://localhost:8080/api/v1/kafka/publish \
  -H "Content-Type: application/json" \
  -d '{"message":"Authenticated message"}'
```

### Scenario 2: Test with SSL/TLS

**Setup certificates (if available):**
```properties
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.ssl.truststore.location=file:/path/to/truststore.jks
spring.kafka.properties.ssl.truststore.password=password
```

**Test:**
```bash
mvn spring-boot:run
curl -X POST http://localhost:8080/api/v1/kafka/publish \
  -H "Content-Type: application/json" \
  -d '{"message":"SSL message"}'
```

### Scenario 3: Test Message Ordering

```bash
# Send messages with same key (should maintain order)
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/v1/kafka/publish \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"Message $i\",\"key\":\"order-test\"}"
  sleep 0.5
done

# Consume messages from topic to verify order
# docker exec kafka kafka-console-consumer \
#   --bootstrap-server localhost:9092 \
#   --topic my-topic \
#   --from-beginning
```

### Scenario 4: Test Large Messages

```bash
# Create large message (1MB)
large_msg=$(head -c 1048576 </dev/urandom | base64)

curl -X POST http://localhost:8080/api/v1/kafka/publish \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"$large_msg\",\"key\":\"large-msg\"}"
```

### Scenario 5: Test Concurrent Requests

```bash
# Send 50 concurrent requests using GNU Parallel
# Install: brew install parallel

seq 1 50 | parallel -j 50 \
  'curl -X POST http://localhost:8080/api/v1/kafka/publish \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"Concurrent msg {}\",\"key\":\"concurrent-{}\"}" \
    -s | jq ".timeTakenMs"' | \
  awk '{sum+=$1; count++} END {print "Average: " sum/count "ms"}'
```

---

## Kafka Consumer Verification

### Consume Messages from Topic

```bash
# Using kafka CLI (from container)
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic my-topic \
  --from-beginning

# View last 10 messages only
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic my-topic \
  --max-messages 10

# View messages with timestamps and keys
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic my-topic \
  --property print.timestamp=true \
  --property print.key=true \
  --max-messages 10
```

### Check Topic Details

```bash
# List all topics
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Describe topic
docker exec kafka kafka-topics \
  --describe \
  --topic my-topic \
  --bootstrap-server localhost:9092

# Check message count in topic
docker exec kafka kafka-run-class kafka.tools.JmxTool \
  --object-name kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions
```

---

## Troubleshooting Tests

### Issue: Connection Refused

```bash
# Check if Kafka is running
docker ps | grep kafka

# Check Kafka logs
docker logs kafka

# Verify port accessibility
netstat -an | grep 9092
```

### Issue: JAAS Authentication Failed

```bash
# Enable debug logging
export LOGGING_LEVEL_ORG_APACHE_KAFKA=DEBUG
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_KAFKA=DEBUG

mvn spring-boot:run

# Check application logs for authentication errors
```

### Issue: Message Not Appearing in Topic

```bash
# Check if topic exists
docker exec kafka kafka-topics \
  --list \
  --bootstrap-server localhost:9092 | grep my-topic

# If not, create topic
docker exec kafka kafka-topics \
  --create \
  --topic my-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

# Check message count
docker exec kafka kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic my-topic \
  --time -1
```

### Issue: Slow Message Publishing

```bash
# Check application metrics
curl http://localhost:8080/actuator/metrics/kafka.producer.records.sent

# View all metrics
curl http://localhost:8080/actuator/prometheus

# Check resource usage
docker stats kafka-producer
```

---

## Automated Test Script

Create `run-tests.sh`:

```bash
#!/bin/bash

echo "Starting Kafka Producer Tests..."
BASE_URL="http://localhost:8080/api/v1/kafka"

# Test 1: Health Check
echo "Test 1: Health Check"
curl -s "$BASE_URL/health"
echo ""

# Test 2: Single Message
echo "Test 2: Single Message"
response=$(curl -s -X POST "$BASE_URL/publish" \
  -H "Content-Type: application/json" \
  -d '{"message":"Test message","key":"test-key"}')
time=$(echo "$response" | jq '.timeTakenMs')
echo "Message published in $time ms"

# Test 3: Batch Messages
echo "Test 3: Batch Messages"
response=$(curl -s -X POST "$BASE_URL/publish-batch" \
  -H "Content-Type: application/json" \
  -d '[{"message":"Msg1"},{"message":"Msg2"},{"message":"Msg3"}]')
total_time=$(echo "$response" | jq '.totalTimeTakenMs')
echo "Batch published in $total_time ms"

# Test 4: Performance Test
echo "Test 4: Performance (10 messages)"
times=()
for i in {1..10}; do
  response=$(curl -s -X POST "$BASE_URL/publish" \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"Perf test $i\"}")
  time=$(echo "$response" | jq '.timeTakenMs')
  times+=($time)
done
avg=$(echo "scale=2; $(IFS=+; echo "${times[*]}")" | bc)
echo "Average time: $(($avg / 10))ms"

echo ""
echo "All tests completed!"
```

Run tests:
```bash
chmod +x run-tests.sh
./run-tests.sh
```

---

## References

- [Apache Kafka Testing Guide](https://kafka.apache.org/documentation/#testing)
- [Spring Kafka Testing](https://spring.io/projects/spring-kafka)
- [curl Documentation](https://curl.se/docs/)
- [Apache Bench Documentation](https://httpd.apache.org/docs/current/programs/ab.html)
