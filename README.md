# Kafka Producer with Spring Boot

A Spring Boot application that demonstrates how to produce messages to Apache Kafka with JAAS authentication, SSL/TLS certificate support, and performance timing measurements.

## Features

- **Spring Boot REST API** for publishing messages to Kafka
- **JAAS Authentication** (PLAIN mechanism) configured via `application.properties`
- **SSL/TLS Support** with customizable certificate paths
- **Performance Timing** - Measures time taken from message send to Kafka acknowledgement
- **Batch Publishing** - Support for publishing multiple messages in one API call
- **Comprehensive Logging** - Detailed logging of message publishing lifecycle
- **Async Processing** - Non-blocking, async message publishing with callbacks
- **Health Check Endpoint** - Service health verification

## Project Structure

```
src/main/java/com/example/kafka/
├── KafkaProducerApplication.java      # Main Spring Boot application
├── config/
│   └── KafkaConfig.java               # Kafka configuration
├── controller/
│   └── KafkaProducerController.java    # REST API endpoints
├── service/
│   └── KafkaProducerService.java       # Business logic for publishing
└── dto/
    ├── MessageRequest.java             # Request DTO
    └── MessageResponse.java            # Response DTO

src/main/resources/
└── application.properties              # Configuration properties
```

## Prerequisites

- Java 11 or higher
- Spring Boot 2.7.x
- Apache Kafka 2.8+
- Kafka broker configured with SASL/PLAIN and SSL/TLS

## Configuration

### application.properties

Configure the following properties for your Kafka setup:

```properties
# Kafka Bootstrap Servers
spring.kafka.bootstrap-servers=localhost:9092

# JAAS Configuration (SASL/PLAIN)
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="your_username" password="your_password";

# SSL/TLS Certificate Configuration
spring.kafka.properties.ssl.truststore.location=file:${user.home}/kafka/certificates/truststore.jks
spring.kafka.properties.ssl.truststore.password=your_truststore_password
spring.kafka.properties.ssl.keystore.location=file:${user.home}/kafka/certificates/keystore.jks
spring.kafka.properties.ssl.keystore.password=your_keystore_password
spring.kafka.properties.ssl.key.password=your_key_password

# Kafka Topic Name
kafka.topic.name=my-topic

# Producer Configuration
spring.kafka.producer.acks=all                           # Wait for all replicas
spring.kafka.producer.retries=3                          # Retry attempts
spring.kafka.producer.compression-type=snappy           # Compression
spring.kafka.producer.properties.linger.ms=10           # Batch wait time
spring.kafka.producer.properties.batch.size=16384       # Batch size
```

### JAAS Configuration Details

#### PLAIN Authentication
```properties
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="pass";
```

#### Using Environment Variables (Recommended for Production)
```properties
spring.kafka.properties.sasl.jaas.config=${KAFKA_JAAS_CONFIG}
```

Then set environment variable:
```bash
export KAFKA_JAAS_CONFIG='org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="pass";'
```

### SSL/TLS Certificate Setup

#### Using JKS Keystores (Java Native Format)

1. **Truststore** - For trusting Kafka broker certificate:
```bash
keytool -import -alias ca -file ca-cert.pem -storetype JKS -keystore truststore.jks
```

2. **Keystore** - For client certificate (if mutual TLS is required):
```bash
keytool -import -alias broker -file broker-cert.pem -storetype JKS -keystore keystore.jks
```

#### Using PEM Certificates

If your certificates are in PEM format:
```properties
spring.kafka.properties.ssl.ca.location=file:${user.home}/kafka/certificates/ca-cert.pem
```

#### Certificate File Structure

```
${user.home}/kafka/certificates/
├── truststore.jks
├── keystore.jks
├── ca-cert.pem
└── client-cert.pem
```

## Building the Application

```bash
mvn clean install
```

## Running the Application

```bash
mvn spring-boot:run
```

Or using JAR:
```bash
java -jar target/kafka-producer-1.0.0.jar
```

## API Endpoints

### 1. Publish Single Message

**Endpoint:** `POST /api/v1/kafka/publish`

**Request:**
```json
{
  "message": "Hello, Kafka!",
  "key": "my-key"
}
```

**Response:**
```json
{
  "status": "SUCCESS",
  "message": "Message sent successfully",
  "timeTakenMs": 45,
  "timestamp": 1700000000000,
  "offset": 100,
  "partition": 0,
  "topic": "my-topic"
}
```

**Using cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/kafka/publish \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello, Kafka!","key":"my-key"}'
```

### 2. Publish Batch Messages

**Endpoint:** `POST /api/v1/kafka/publish-batch`

**Request:**
```json
[
  {"message": "Message 1", "key": "key-1"},
  {"message": "Message 2", "key": "key-2"},
  {"message": "Message 3", "key": "key-3"}
]
```

**Response:**
```json
{
  "totalMessages": 3,
  "totalTimeTakenMs": 120,
  "responses": [
    {
      "status": "SUCCESS",
      "message": "Message sent successfully",
      "timeTakenMs": 40,
      "offset": 100,
      "partition": 0,
      "topic": "my-topic"
    },
    ...
  ]
}
```

**Using cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/kafka/publish-batch \
  -H "Content-Type: application/json" \
  -d '[{"message":"Message 1"},{"message":"Message 2"}]'
```

### 3. Health Check

**Endpoint:** `GET /api/v1/kafka/health`

**Response:**
```
Kafka Producer Service is running
```

**Using cURL:**
```bash
curl http://localhost:8080/api/v1/kafka/health
```

## Performance Timing

The application measures the time from when the message is sent until an acknowledgement is received from Kafka. This includes:

1. **Message Serialization**
2. **Network Transmission**
3. **Kafka Broker Processing**
4. **Replication** (if configured with `acks=all`)
5. **Response Transmission Back to Client**

The `timeTakenMs` field in the response shows the total elapsed time in milliseconds.

## Logging

The application uses SLF4J with Logback. Log levels can be configured in `application.properties`:

```properties
logging.level.root=INFO
logging.level.com.example=DEBUG
logging.level.org.springframework.kafka=DEBUG
```

### Sample Log Output

```
2026-11-23 23:51:42 - INFO - Received request to publish message: MessageRequest(message=Hello Kafka, key=null)
2026-11-23 23:51:42 - INFO - Sending message to topic 'my-topic' with key 'key-1700000000000'. Message: Hello Kafka
2026-11-23 23:51:42 - INFO - Message sent successfully to topic 'my-topic' in 45 ms. Partition: 0, Offset: 100, Timestamp: 1700000000000
```

## Troubleshooting

### JAAS Authentication Fails

**Error:** `org.apache.kafka.common.errors.SaslAuthenticationException`

**Solution:** Verify JAAS configuration in `application.properties`:
- Check username and password are correct
- Ensure SASL mechanism matches Kafka broker configuration
- Verify `security.protocol` is set to `SASL_SSL` or `SASL_PLAINTEXT`

### SSL/TLS Certificate Errors

**Error:** `javax.net.ssl.SSLHandshakeException`

**Solution:**
- Verify certificate paths in `application.properties`
- Ensure certificates are accessible and permissions are correct
- Check truststore contains the correct CA certificate
- Verify passwords for keystores and keys

### Connection Timeout

**Error:** `org.apache.kafka.common.errors.TimeoutException`

**Solution:**
- Verify `spring.kafka.bootstrap-servers` points to correct Kafka broker
- Check firewall rules allow connections on Kafka port
- Ensure Kafka broker is running and accessible

### Message Not Appearing in Topic

**Solution:**
- Verify topic name matches in `application.properties`
- Check topic exists: `kafka-topics --list --bootstrap-server localhost:9092`
- Verify producer has permissions to write to topic
- Check Kafka broker logs for errors

## Example Docker Setup

If using Docker Compose for Kafka:

```yaml
version: '3'
services:
  kafka:
    image: confluentinc/cp-kafka:7.4.0
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,SASL_SSL://kafka:9093
      KAFKA_SECURITY_INTER_BROKER_PROTOCOL: SASL_SSL
      KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL: PLAIN
      KAFKA_SASL_ENABLED_MECHANISMS: PLAIN
    depends_on:
      - zookeeper
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
```

## Performance Considerations

1. **Acks Configuration:**
   - `acks=1` - Fastest, lowest durability
   - `acks=all` - Slowest, highest durability (recommended)

2. **Batch Settings:**
   - Increase `linger.ms` for better throughput
   - Increase `batch.size` for better compression

3. **Compression:**
   - `snappy` - Good balance of compression and speed
   - `gzip` - Better compression, slower
   - `lz4` - Faster compression, less compression ratio

## Testing

```bash
# Test single message
curl -X POST http://localhost:8080/api/v1/kafka/publish \
  -H "Content-Type: application/json" \
  -d '{"message":"Test message","key":"test-key"}'

# Test batch
curl -X POST http://localhost:8080/api/v1/kafka/publish-batch \
  -H "Content-Type: application/json" \
  -d '[{"message":"Msg1"},{"message":"Msg2"}]'
```

## References

- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Apache Kafka Security Documentation](https://kafka.apache.org/documentation/#security)
- [Kafka JAAS Configuration](https://kafka.apache.org/documentation/#security_sasl)
- [Kafka SSL/TLS Setup](https://kafka.apache.org/documentation/#security_ssl)

## License

MIT License

## Author

Created with Spring Boot and Spring Kafka
