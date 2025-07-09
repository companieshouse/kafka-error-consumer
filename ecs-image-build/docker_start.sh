#!/bin/bash

# Start script for kafka-error-consumer

PORT=8080
exec java -jar -Dserver.port="${PORT}" -XX:MaxRAMPercentage=80 "kafka-error-consumer.jar"
