#!/bin/bash

# Configuration
HOST="localhost"
PORT="8080"
ENDPOINT="/api/todos"
URL="http://$HOST:$PORT$ENDPOINT"
CONTENT_TYPE="application/json"
AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJBbGljZSIsImdyb3VwcyI6WyJhZG1pbnMiLCJ1c2VycyJdLCJpc3MiOiJ0b2RvLWFwcCIsImF1ZCI6InRvZG8tYXBwLXVzZXJzIiwiaWF0IjoxNzQ2Nzk2NTYxLCJleHAiOjE3NDY4ODI5NjF9.QwsYNLhNfrpIMLQ1bV2qWOujyLc9pI-vg1RJzR34c2M"

# Performance test configuration
TOTAL_REQUESTS=10000    # Total number of requests to send
CONCURRENCY=1         # Number of concurrent requests

# Create directories if they don't exist
mkdir -p ../build/test-data
mkdir -p ../build/reports

# Check if we have generated test data
if [ ! "$(ls -A ../build/test-data 2>/dev/null)" ]; then
    echo "Generating test data..."
    kotlin generate-test-data.kts
fi

# Select a single TODO file for testing
TEST_FILE="../build/test-data/todo-0.json"

# Make sure the test file exists
if [ ! -f "$TEST_FILE" ]; then
    echo "Error: Test file $TEST_FILE not found!"
    exit 1
fi

echo "=== Running benchmark test ==="
echo "URL: $URL"
echo "Sending $TOTAL_REQUESTS requests with concurrency $CONCURRENCY"
echo "Using test file: $TEST_FILE"
echo ""

# Run the benchmark test
ab -n $TOTAL_REQUESTS -c $CONCURRENCY -p "$TEST_FILE" -T "$CONTENT_TYPE" \
   -H "Authorization: Bearer $AUTH_TOKEN" \
   -v 2 "$URL" > "../build/reports/benchmark-report.txt" 2>&1

# Display a summary of the results
echo ""
echo "Benchmark completed!"
echo ""
echo "Summary of results:"
grep -A 10 "Document Length" "../build/reports/benchmark-report.txt" | head -11
echo ""
echo "Full report saved to ../build/reports/benchmark-report.txt"
