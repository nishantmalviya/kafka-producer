#!/bin/bash
# test-api.sh - Script to test the Kafka Producer API endpoints

BASE_URL="http://localhost:8080/api/v1/kafka"
TOPIC="my-topic"

echo "=========================================="
echo "Kafka Producer API Test Script"
echo "=========================================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Health Check
echo -e "${YELLOW}Test 1: Health Check${NC}"
echo "Endpoint: GET $BASE_URL/health"
response=$(curl -s "$BASE_URL/health")
echo "Response: $response"
echo ""

# Test 2: Single Message
echo -e "${YELLOW}Test 2: Send Single Message${NC}"
echo "Endpoint: POST $BASE_URL/publish"
payload='{"message":"Hello from Kafka Producer!","key":"test-key-1"}'
echo "Payload: $payload"
response=$(curl -s -X POST "$BASE_URL/publish" \
  -H "Content-Type: application/json" \
  -d "$payload")
echo "Response:"
echo "$response" | python3 -m json.tool
echo ""

# Test 3: Multiple Messages
echo -e "${YELLOW}Test 3: Send Multiple Messages${NC}"
echo "Endpoint: POST $BASE_URL/publish-batch"
payload='[
  {"message":"Message 1","key":"batch-key-1"},
  {"message":"Message 2","key":"batch-key-2"},
  {"message":"Message 3","key":"batch-key-3"}
]'
echo "Payload: $payload"
response=$(curl -s -X POST "$BASE_URL/publish-batch" \
  -H "Content-Type: application/json" \
  -d "$payload")
echo "Response:"
echo "$response" | python3 -m json.tool
echo ""

# Test 4: Message without Key (auto-generated)
echo -e "${YELLOW}Test 4: Send Message without Key${NC}"
echo "Endpoint: POST $BASE_URL/publish"
payload='{"message":"Message with auto-generated key"}'
echo "Payload: $payload"
response=$(curl -s -X POST "$BASE_URL/publish" \
  -H "Content-Type: application/json" \
  -d "$payload")
echo "Response:"
echo "$response" | python3 -m json.tool
echo ""

# Test 5: Error Test - Empty Message
echo -e "${YELLOW}Test 5: Error Test - Empty Message${NC}"
echo "Endpoint: POST $BASE_URL/publish"
payload='{"message":"","key":"error-test"}'
echo "Payload: $payload"
response=$(curl -s -X POST "$BASE_URL/publish" \
  -H "Content-Type: application/json" \
  -d "$payload")
echo "Response:"
echo "$response" | python3 -m json.tool
echo ""

echo -e "${GREEN}All tests completed!${NC}"
