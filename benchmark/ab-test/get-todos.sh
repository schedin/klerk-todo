#!/bin/bash

# Configuration
HOST="localhost"
PORT="8080"
ENDPOINT="/api/todos/random"
URL="http://$HOST:$PORT$ENDPOINT"
NUM_REQUESTS=100000
CONCURRENCY=1
AUTH_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJBbGljZSIsImdyb3VwcyI6WyJhZG1pbnMiLCJ1c2VycyJdLCJpc3MiOiJ0b2RvLWFwcCIsImF1ZCI6InRvZG8tYXBwLXVzZXJzIiwiaWF0IjoxNzQ3MDUwODI5LCJleHAiOjE3NDcxMzcyMjl9.CYHPCdVTVN2SXWvcLSBCxad-3-GNDD-dV9qQ4bnKKbY"

echo "Running benchmark for GET todos..."

ab -n $NUM_REQUESTS -c $CONCURRENCY -H "Authorization: Bearer $AUTH_TOKEN" -k $URL

echo "Benchmark completed!"
