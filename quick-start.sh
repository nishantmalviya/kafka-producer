#!/bin/bash
# quick-start.sh - Quick start guide for the Kafka Producer application

echo "=========================================="
echo "Kafka Producer Application - Quick Start"
echo "=========================================="
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed. Please install Maven first."
    exit 1
fi

echo "Step 1: Building the application..."
mvn clean install -q -DskipTests
if [ $? -eq 0 ]; then
    echo "✓ Build successful"
else
    echo "✗ Build failed"
    exit 1
fi
echo ""

echo "Step 2: Starting the application..."
echo "The application will be available at http://localhost:8080"
echo ""
echo "Before running, ensure:"
echo "  1. Kafka broker is running and accessible"
echo "  2. application.properties is configured with correct Kafka details"
echo "  3. SSL certificates are in place (if using SASL_SSL)"
echo ""
echo "Starting application..."
mvn spring-boot:run

