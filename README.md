# kafka-error-consumer

## Introduction

A generic consumer application to move messages from a Kafka error topic to a retry topic.

This application will move records from its configured error topic into the configured retry topic.

The default behaviour is to start at the previous offset and continue processing until it reaches the offset which was current when the application started. The application then stops consuming records, but does not end so that it is not restarted by the infrastructure.

Example 

- The last time it ran, the application consumed records from the error topic up to 55.
- When it starts, the current offset is 450.
- The application will move records from the error topic, between offsets 56 and 450, to the retry topic and then stop consuming.

Currently, this application will only process records on partition 0 of the error topic. This simplifies management of offsets, which are different on each partition.

## Requirements
In order to build kafka-error-consumer locally you will need the following:
- [Java 11](https://www.oracle.com/java/technologies/downloads/#java11)
- [Maven](https://maven.apache.org/download.cgi)
- [Git](https://git-scm.com/downloads)

## Getting started
1. Run mvn clean install
2. Run mvn spring-boot:run

Environment Variables
---------------------
The supported environmental variables are as follows.

### Deployment Variables
Name                                      | Description                                                 | Mandatory | Default            | Example
------                                    | ------                                                      | ------    | ------             | ------
KAFKA_ERROR_BOOTSTRAP_ADDRESS             | Kafka bootstrap address                                     |           | localhost:9092     | kafka-url:9092
KAFKA_ERROR_ERROR_TOPIC                   | name of the error topic to consume from                     | ✓         |                    | insolvency-delta-error
KAFKA_ERROR_RETRY_TOPIC                   | name of the retry topic to send to                          | ✓         |                    | insolvency-delta-retry
KAFKA_ERROR_GROUP_ID                      | name of kafka client group ID to use                        |           | kafka-error        | kafka-error-2
