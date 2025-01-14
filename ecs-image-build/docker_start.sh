#!/bin/bash

# Start script for kafka-error-consumer


PORT=8080
exec java -jar -Dserver.port="${PORT}" "kafka-error-consumer.jar"
