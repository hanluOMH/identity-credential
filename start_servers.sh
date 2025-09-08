#!/bin/bash

echo "Starting Multipaz Identity Credential Servers..."

# Start records server in background
echo "Starting Records Server on port 8007..."
./gradlew multipaz-records-server:run --args="-param server_port=8007" > records.log 2>&1 &
RECORDS_PID=$!

# Start issuer server in background
echo "Starting Issuer Server on port 8008..."
./gradlew multipaz-openid4vci-server:run --args="-param server_port=8008" > issuer.log 2>&1 &
ISSUER_PID=$!

echo "Servers started!"
echo "Records Server PID: $RECORDS_PID"
echo "Issuer Server PID: $ISSUER_PID"
echo ""
echo "Access URLs:"
echo "  Records Server: http://localhost:8007"
echo "  Issuer Server:  http://localhost:8008"
echo ""
echo "To stop servers, run: kill $RECORDS_PID $ISSUER_PID"
echo "Or use: pkill -f multipaz-records-server && pkill -f multipaz-openid4vci-server"

# Wait for both processes
wait
