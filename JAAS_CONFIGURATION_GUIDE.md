# JAAS Configuration Guide for Spring Boot Kafka Producer

## Overview

JAAS (Java Authentication and Authorization Service) is used to authenticate the Kafka client with the Kafka broker. This guide covers different JAAS configurations for various Kafka setups.

## Table of Contents

1. [SASL/PLAIN Authentication](#saslplain-authentication)
2. [SASL/SCRAM Authentication](#saslscram-authentication)
3. [SASL/OAUTHBEARER Authentication](#saslOAuthBearer-authentication)
4. [Environment Variables](#environment-variables)
5. [Troubleshooting](#troubleshooting)

---

## SASL/PLAIN Authentication

PLAIN is the simplest SASL mechanism. Username and password are sent in plaintext (but over SSL/TLS for security).

### Configuration in application.properties

```properties
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="your_username" password="your_password";
```

### Key Points

- `username`: Kafka broker username
- `password`: Kafka broker password
- Must use `SASL_SSL` or `SASL_PLAINTEXT` as protocol
- Format: `org.apache.kafka.common.security.plain.PlainLoginModule required username="xxx" password="yyy";`
- Credentials are plaintext in memory, so always use with SSL/TLS

### Example for Different Brokers

#### Local Kafka (Development)
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.properties.security.protocol=SASL_PLAINTEXT
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="producer" password="producer-secret";
```

#### Confluent Cloud
```properties
spring.kafka.bootstrap-servers=pkc-xxx.us-east-1.provider.confluent.cloud:9092
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="YOUR_API_KEY" password="YOUR_API_SECRET";
```

#### AWS MSK
```properties
spring.kafka.bootstrap-servers=b-1.mycluster.xxxxx.kafka.us-east-1.amazonaws.com:9092
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="mskuser" password="mskpassword";
spring.kafka.properties.ssl.truststore.location=file:/var/secrets/AmazonRootCA1.pem
```

---

## SASL/SCRAM Authentication

SCRAM (Salted Challenge Response Authentication Mechanism) is more secure than PLAIN as it doesn't send passwords in plaintext.

### Available Mechanisms

- `SCRAM-SHA-256` - SHA-256 based authentication
- `SCRAM-SHA-512` - SHA-512 based authentication (more secure)

### Configuration in application.properties

```properties
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=SCRAM-SHA-512
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="your_username" password="your_password";
```

### Example for AWS MSK with SCRAM-SHA-512

```properties
spring.kafka.bootstrap-servers=b-1.mycluster.xxxxx.kafka.us-east-1.amazonaws.com:9092
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=SCRAM-SHA-512
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="mskuser" password="mskpassword";
spring.kafka.properties.ssl.truststore.location=file:/etc/kafka/secrets/AmazonRootCA1.pem
spring.kafka.properties.ssl.truststore.type=PEM
```

---

## SASL/OAuth Bearer Authentication

OAuth Bearer tokens are used for authentication with OAuth 2.0 providers.

### Configuration in application.properties

```properties
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=OAUTHBEARER
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;
spring.kafka.properties.sasl.login.callback.handler.class=org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerLoginCallbackHandler
spring.kafka.properties.sasl.oauthbearer.token.endpoint.url=https://your-oauth-provider.com/oauth/authorize
spring.kafka.properties.sasl.oauthbearer.client.id=your_client_id
spring.kafka.properties.sasl.oauthbearer.client.secret=your_client_secret
spring.kafka.properties.sasl.oauthbearer.scope=your_scope
```

---

## Environment Variables

For production deployments, use environment variables instead of hardcoding credentials in properties files.

### Method 1: Using Spring Property Placeholders

```properties
spring.kafka.properties.sasl.jaas.config=${KAFKA_JAAS_CONFIG}
spring.kafka.properties.ssl.truststore.password=${SSL_TRUSTSTORE_PASSWORD}
spring.kafka.properties.ssl.keystore.password=${SSL_KEYSTORE_PASSWORD}
```

Set environment variables:
```bash
export KAFKA_JAAS_CONFIG='org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="pass";'
export SSL_TRUSTSTORE_PASSWORD='truststore_password'
export SSL_KEYSTORE_PASSWORD='keystore_password'
```

### Method 2: application-prod.properties

Create `application-prod.properties`:

```properties
spring.kafka.properties.sasl.jaas.config=${KAFKA_JAAS_CONFIG}
```

Run application with production profile:
```bash
java -Dspring.profiles.active=prod \
  -DKAFKA_JAAS_CONFIG='org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="pass";' \
  -jar kafka-producer-1.0.0.jar
```

### Method 3: Java System Properties

```bash
java -Dspring.kafka.properties.sasl.jaas.config='org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="pass";' \
  -jar kafka-producer-1.0.0.jar
```

### Method 4: Command Line Arguments

```bash
java -jar kafka-producer-1.0.0.jar \
  --spring.kafka.properties.sasl.jaas.config='org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="pass";'
```

---

## Complete Configuration Examples

### Example 1: Confluent Cloud (Minimal)

```properties
spring.kafka.bootstrap-servers=pkc-xxxxxx.us-east-1.provider.confluent.cloud:9092
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="${CONFLUENT_API_KEY}" password="${CONFLUENT_API_SECRET}";
kafka.topic.name=my-topic
spring.kafka.producer.acks=all
```

### Example 2: Self-Hosted with Full Configuration

```properties
spring.kafka.bootstrap-servers=kafka-broker-1:9093,kafka-broker-2:9093,kafka-broker-3:9093
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=SCRAM-SHA-512
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="${KAFKA_USERNAME}" password="${KAFKA_PASSWORD}";

# SSL/TLS Configuration
spring.kafka.properties.ssl.truststore.location=file:/etc/kafka/secrets/truststore.jks
spring.kafka.properties.ssl.truststore.password=${SSL_TRUSTSTORE_PASSWORD}
spring.kafka.properties.ssl.truststore.type=JKS

# Optional: Client certificate (mutual TLS)
spring.kafka.properties.ssl.keystore.location=file:/etc/kafka/secrets/keystore.jks
spring.kafka.properties.ssl.keystore.password=${SSL_KEYSTORE_PASSWORD}
spring.kafka.properties.ssl.key.password=${SSL_KEY_PASSWORD}
spring.kafka.properties.ssl.keystore.type=JKS

# Producer Settings
kafka.topic.name=production-topic
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
spring.kafka.producer.compression-type=snappy
```

### Example 3: Azure Event Hubs

```properties
spring.kafka.bootstrap-servers=mynamespace.servicebus.windows.net:9093
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="${AZURE_EVENTHUBS_CONNECTION_STRING}";
kafka.topic.name=my-event-hub
```

---

## Special Characters in Credentials

If your username or password contains special characters, they need to be properly escaped:

| Character | Escape Sequence |
|-----------|-----------------|
| `;` | `\;` |
| `=` | `\=` |
| `"` | `\"` |
| `\` | `\\` |

### Example

```properties
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="user@domain" password="pass\=word123";
```

---

## Troubleshooting

### Error: `SaslAuthenticationException`

**Symptoms:**
```
org.apache.kafka.common.errors.SaslAuthenticationException: Authentication failed during authentication with broker
```

**Causes & Solutions:**

1. **Wrong credentials**
   - Verify username and password are correct
   - Check for trailing/leading whitespace

2. **Wrong SASL mechanism**
   - Ensure `sasl.mechanism` matches broker configuration
   - Common mechanisms: `PLAIN`, `SCRAM-SHA-256`, `SCRAM-SHA-512`

3. **Missing JAAS configuration**
   - Verify `sasl.jaas.config` property is set
   - Check format is correct (ends with semicolon)

**Debug:**
```bash
# Enable DEBUG logging
logging.level.org.apache.kafka=DEBUG
```

### Error: `SSLHandshakeException`

**Symptoms:**
```
javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed
```

**Solutions:**

1. **Truststore not found**
   - Verify `ssl.truststore.location` path exists
   - Use absolute paths: `file:/var/kafka/certs/truststore.jks`

2. **Truststore password incorrect**
   - Verify `ssl.truststore.password` is correct
   - Check password doesn't have shell metacharacters

3. **Certificate not in truststore**
   - Import CA certificate:
     ```bash
     keytool -import -alias ca -file ca-cert.pem -storetype JKS -keystore truststore.jks
     ```

### Error: `TimeoutException`

**Symptoms:**
```
org.apache.kafka.common.errors.TimeoutException: Failed to send metadata request
```

**Solutions:**

1. **Wrong bootstrap servers**
   - Verify broker addresses and ports
   - Check firewall allows connections

2. **Broker not running**
   - Ensure Kafka broker is running
   - Check broker logs for errors

---

## Security Best Practices

1. **Never commit credentials to version control**
   - Use `.gitignore` for `application.properties`
   - Use environment variables for sensitive data

2. **Always use TLS in production**
   - Use `SASL_SSL` instead of `SASL_PLAINTEXT`
   - Validate server certificates

3. **Use strong authentication**
   - Prefer `SCRAM-SHA-512` over `PLAIN`
   - Use complex passwords

4. **Restrict file permissions**
   - Keystores: `chmod 600`
   - Truststores: `chmod 644`

5. **Rotate credentials regularly**
   - Change passwords periodically
   - Use secrets management tools (Vault, etc.)

---

## References

- [Apache Kafka Security Documentation](https://kafka.apache.org/documentation/#security)
- [Spring Kafka Properties](https://docs.spring.io/spring-kafka/docs/current/api/org/springframework/kafka/core/KafkaTemplate.html)
- [Kafka Client Configuration](https://kafka.apache.org/documentation/#producerconfigs)
