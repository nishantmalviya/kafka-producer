# Spring Boot Kafka Producer - Complete Package

## 📦 What's Included

A production-ready Spring Boot application for publishing messages to Apache Kafka with JAAS authentication, SSL/TLS support, and comprehensive performance timing measurements.

---

## 🗂️ Project Files Guide

### 📄 Documentation Files

| File | Purpose |
|------|---------|
| [README.md](README.md) | Complete project documentation with API examples |
| [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) | Quick overview and getting started checklist |
| [JAAS_CONFIGURATION_GUIDE.md](JAAS_CONFIGURATION_GUIDE.md) | Detailed JAAS setup for different Kafka brokers |
| [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) | Docker, Kubernetes, and production deployment |
| [TESTING_GUIDE.md](TESTING_GUIDE.md) | Comprehensive testing scenarios and load testing |

### 💻 Java Source Files

| File | Purpose |
|------|---------|
| [KafkaProducerApplication.java](src/main/java/com/example/kafka/KafkaProducerApplication.java) | Main Spring Boot application entry point |
| [KafkaConfig.java](src/main/java/com/example/kafka/config/KafkaConfig.java) | Kafka configuration class |
| [KafkaProducerController.java](src/main/java/com/example/kafka/controller/KafkaProducerController.java) | REST API endpoints for message publishing |
| [KafkaProducerService.java](src/main/java/com/example/kafka/service/KafkaProducerService.java) | Business logic for publishing messages |
| [MessageRequest.java](src/main/java/com/example/kafka/dto/MessageRequest.java) | API request data transfer object |
| [MessageResponse.java](src/main/java/com/example/kafka/dto/MessageResponse.java) | API response with timing information |

### ⚙️ Configuration Files

| File | Purpose |
|------|---------|
| [pom.xml](pom.xml) | Maven dependencies and build configuration |
| [application.properties](src/main/resources/application.properties) | Spring Boot application configuration |
| [application.properties.example](application.properties.example) | Configuration examples for different scenarios |
| [docker-compose.yml](docker-compose.yml) | Docker Compose for local Kafka setup |
| [kafka-server-jaas.conf](kafka-server-jaas.conf) | JAAS configuration for Kafka broker |
| [.gitignore](.gitignore) | Git ignore rules (keeps secrets safe) |

### 🔧 Utility Scripts

| File | Purpose |
|------|---------|
| [quick-start.sh](quick-start.sh) | Build and run the application quickly |
| [test-api.sh](test-api.sh) | Test all API endpoints with cURL |

---

## 🚀 Quick Start (5 minutes)

### 1. Prerequisites
```bash
java -version        # Ensure Java 17+ is installed
mvn -v              # Ensure Maven is installed
docker --version    # Ensure Docker is installed (for local Kafka)
```

### 2. Start Kafka (Local Testing)
```bash
docker-compose up -d
sleep 30            # Wait for Kafka to start
```

### 3. Configure (Optional for Local Testing)
The default `application.properties` works for local Kafka without authentication.

### 4. Build & Run
```bash
mvn clean install
java -jar target/kafka-producer-1.0.0.jar
# Application starts at http://localhost:8080
```

### 5. Test the API
```bash
# Health check
curl http://localhost:8080/api/v1/kafka/health

# Publish a message
curl -X POST http://localhost:8080/api/v1/kafka/publish \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello Kafka!","key":"test-key"}'
```

---

## 📡 API Endpoints

### Publish Single Message
**POST** `/api/v1/kafka/publish`

```bash
curl -X POST http://localhost:8080/api/v1/kafka/publish \
  -H "Content-Type: application/json" \
  -d '{"message":"Your message here","key":"optional-key"}'
```

Response includes `timeTakenMs` - the time from sending to getting Kafka acknowledgement.

### Publish Batch Messages
**POST** `/api/v1/kafka/publish-batch`

```bash
curl -X POST http://localhost:8080/api/v1/kafka/publish-batch \
  -H "Content-Type: application/json" \
  -d '[
    {"message":"Message 1"},
    {"message":"Message 2"},
    {"message":"Message 3"}
  ]'
```

### Health Check
**GET** `/api/v1/kafka/health`

```bash
curl http://localhost:8080/api/v1/kafka/health
```

---

## 🔐 Configuration Highlights

### For Local Development (No Authentication)
```properties
spring.kafka.bootstrap-servers=localhost:9092
kafka.topic.name=my-topic
```

### For Production (with JAAS & SSL)
```properties
spring.kafka.bootstrap-servers=kafka.example.com:9093
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="pass";
spring.kafka.properties.ssl.truststore.location=file:/etc/kafka/truststore.jks
spring.kafka.properties.ssl.truststore.password=${SSL_TRUSTSTORE_PASSWORD}
```

**See [JAAS_CONFIGURATION_GUIDE.md](JAAS_CONFIGURATION_GUIDE.md) for all authentication options.**

---

## ⏱️ Performance Timing

Each API response includes `timeTakenMs` - the time from message publish request to receiving Kafka broker acknowledgement.

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

This helps you understand:
- End-to-end latency
- Impact of different `acks` settings
- Performance with compression
- Replication overhead

---

## 📊 Key Features

✅ **JAAS Authentication**
- PLAIN, SCRAM-SHA-256, SCRAM-SHA-512, OAuth Bearer

✅ **SSL/TLS Security**
- Server certificate verification (truststore)
- Mutual TLS (keystore)
- PEM and JKS format support

✅ **Performance Measurement**
- Timing from message send to Kafka acknowledgement
- Per-message and batch statistics

✅ **REST API**
- Single message publishing
- Batch message publishing
- Health check endpoint

✅ **High Availability**
- Async message processing
- Configurable retry policies
- Multiple replica support

✅ **Production Ready**
- Docker and Kubernetes support
- Comprehensive logging
- Configurable via properties or environment variables
- Error handling and validation

---

## 🧪 Testing

### Unit Testing
Run the test script:
```bash
chmod +x test-api.sh
./test-api.sh
```

### Load Testing
See [TESTING_GUIDE.md](TESTING_GUIDE.md) for:
- Performance testing with Apache Bench
- Throughput testing with wrk
- Concurrent request testing
- Large message testing

---

## 🐳 Docker Deployment

### Build Image
```bash
docker build -t kafka-producer:1.0.0 .
```

### Run Container
```bash
docker run -d \
  -p 8080:8080 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9093 \
  -e SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG='org.apache.kafka.common.security.plain.PlainLoginModule required username="producer" password="producer-secret";' \
  kafka-producer:1.0.0
```

See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for Kubernetes deployment.

---

## 📋 Configuration Priority

Configurations are loaded in this order (highest priority first):

1. **Environment Variables** (e.g., `SPRING_KAFKA_BOOTSTRAP_SERVERS`)
2. **Command Line Arguments** (e.g., `--spring.kafka.bootstrap-servers=...`)
3. **System Properties** (e.g., `-Dspring.kafka.bootstrap-servers=...`)
4. **application.properties File**
5. **application-{profile}.properties Files**

**Recommended for Production:**
```bash
export KAFKA_JAAS_CONFIG='org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="pass";'
export SSL_TRUSTSTORE_PASSWORD='password'
java -jar kafka-producer-1.0.0.jar
```

---

## 🔍 Common Use Cases

### Local Development
1. Use `docker-compose.yml` to start Kafka
2. Update `application.properties` with `spring.kafka.bootstrap-servers=localhost:9092`
3. Run `mvn spring-boot:run`
4. Test with `curl` or `test-api.sh`

### Docker Container
1. Build image: `docker build -t kafka-producer:1.0.0 .`
2. See docker-compose.yml for complete setup with Kafka
3. Run: `docker-compose up -d`

### Kubernetes
1. See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for complete k8s manifests
2. Create secrets for credentials and certificates
3. Deploy using `kubectl apply`

### Confluent Cloud
1. Update credentials in `application.properties`
2. See [application.properties.example](application.properties.example) for Confluent Cloud config
3. Run the application

### AWS MSK
1. See [JAAS_CONFIGURATION_GUIDE.md](JAAS_CONFIGURATION_GUIDE.md) for MSK-specific configuration
2. Configure SCRAM-SHA-512 and truststore
3. Deploy to EC2 or ECS

---

## 🛠️ Troubleshooting

### "Connection Refused"
- Check if Kafka is running: `docker-compose ps`
- Verify bootstrap servers address

### "JAAS Authentication Failed"
- Verify username and password in `application.properties`
- Ensure SASL mechanism matches broker configuration
- Check JAAS format ends with semicolon

### "SSL Handshake Exception"
- Verify truststore location exists
- Check truststore password is correct
- Verify certificate is imported in truststore

**More help:** See [TESTING_GUIDE.md](TESTING_GUIDE.md) troubleshooting section

---

## 📚 Documentation Index

| When You Want to... | Read This |
|-------------------|-----------|
| Get started quickly | [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) |
| Understand all features | [README.md](README.md) |
| Configure authentication | [JAAS_CONFIGURATION_GUIDE.md](JAAS_CONFIGURATION_GUIDE.md) |
| Deploy to production | [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) |
| Test the application | [TESTING_GUIDE.md](TESTING_GUIDE.md) |
| View code examples | See API endpoints above |

---

## 🎯 Next Steps

1. **Quick Test:** Follow [Quick Start](#-quick-start-5-minutes) above
2. **Configure:** Update [application.properties](src/main/resources/application.properties) for your Kafka broker
3. **Deploy:** Choose your deployment method (Local, Docker, Kubernetes, Cloud)
4. **Monitor:** Check logs and metrics for performance insights
5. **Scale:** Increase replicas and tune producer configuration as needed

---

## 📦 Project Structure

```
Kafka/
├── Documentation/
│   ├── README.md                      # Complete guide
│   ├── PROJECT_SUMMARY.md             # Quick overview
│   ├── JAAS_CONFIGURATION_GUIDE.md    # Authentication setup
│   ├── DEPLOYMENT_GUIDE.md            # Deployment instructions
│   └── TESTING_GUIDE.md               # Testing guide
│
├── Source Code/
│   └── src/main/java/com/example/kafka/
│       ├── KafkaProducerApplication.java
│       ├── config/KafkaConfig.java
│       ├── controller/KafkaProducerController.java
│       ├── service/KafkaProducerService.java
│       └── dto/
│           ├── MessageRequest.java
│           └── MessageResponse.java
│
├── Configuration/
│   ├── pom.xml
│   ├── application.properties
│   ├── application.properties.example
│   ├── docker-compose.yml
│   └── kafka-server-jaas.conf
│
└── Utilities/
    ├── quick-start.sh
    ├── test-api.sh
    └── .gitignore
```

---

## ✅ Verification Checklist

After deployment:
- [ ] Application starts without errors
- [ ] Health check endpoint returns "running"
- [ ] Can publish single message
- [ ] Can publish batch messages
- [ ] timeTakenMs shows reasonable values (< 500ms typical)
- [ ] Messages appear in Kafka topic
- [ ] Logs show successful message publishing
- [ ] Authentication works (if configured)
- [ ] SSL/TLS connections succeed (if configured)

---

## 📞 Support

- **Configuration Issues:** See [JAAS_CONFIGURATION_GUIDE.md](JAAS_CONFIGURATION_GUIDE.md)
- **Deployment Issues:** See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
- **Testing Issues:** See [TESTING_GUIDE.md](TESTING_GUIDE.md)
- **General Questions:** See [README.md](README.md)

---

## 🎓 Key Technologies

- **Spring Boot 3.1.5** - Framework
- **Spring Kafka** - Kafka integration
- **Apache Kafka** - Message broker
- **Docker** - Containerization
- **Kubernetes** - Orchestration (optional)

---

## 📄 License

MIT License

---

**Ready to get started? Go to [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) for a quick 5-minute setup!**
