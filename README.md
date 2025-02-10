# kafka-error-consumer

## Introduction

A generic consumer application to move messages from a Kafka error topic to a retry topic.

This application will move records from its configured error topic into the configured retry topic.

The default behaviour is to start at the previous offset and continue processing until it reaches the offset which was current when the application started. The application then stops consuming records, but does not end so that it is not restarted by the infrastructure.

New consumers will start at the earliest offset (`ConsumerConfig.AUTO_OFFSET_RESET_CONFIG = "earliest"`).

Example 

- The last time it ran, the application consumed records from the error topic up to 55.
- When it starts, the current offset is 450.
- The application will move records from the error topic, between offsets 56 and 450, to the retry topic and then stop consuming.

Currently, this application will only process records on partition 0 of the error topic. This simplifies management of offsets, which are different on each partition.

## Requirements
In order to build kafka-error-consumer locally you will need the following:
- [Java 21](https://www.oracle.com/java/technologies/downloads/#java11)
- [Maven](https://maven.apache.org/download.cgi)
- [Git](https://git-scm.com/downloads)

## Getting started
1. Run mvn clean install
2. Run mvn spring-boot:run

## Running Locally

Kafka-error-consumer is configured in docker-chs-project, the docker-compose file includes all the listed environment variables, and can be configured for local running there.

Enable the following services with `chs-dev services enable`:
- kafka-error-consumer
- kafka
- zookeeper

**Using and configuring kafka locally:**

Enter into the kafka container using `docker exec -it docker-chs-development-kafka-1 bash`

Cd into the folder with the kafka scripts using `cd /opt/bitnami/kafka/bin`, here you can run any of the scrips and it will print out the help information for that script

You can check what topics have been created in your kafka instance using `./kafka-topics.sh --bootstrap-server localhost:9092 --list`
this will allow you to confirm both your error queue and retry queue exist for your local testing

You can push a message onto the kafka error topic you are using (in this example I am using insolvency-delta-error) 
with the following command `./kafka-console-producer.sh --bootstrap-server localhost:9092 --topic insolvency-delta-error`.
You can then type a message, press enter, and exit the command with `command + c`

You can check the content of the topic using `./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic insolvency-delta-error --from-beginning`

Providing everything is working, you should see your message on the retry queue, confirming kafka-error-consumer is working correctly.

Environment Variables
---------------------
The supported environmental variables are as follows.

### Deployment Variables
Name                                      | Description                                                 | Mandatory | Default            | Example
------                                    | ------                                                      | ------    | ------             | ------
KAFKA_ERROR_BOOTSTRAP_ADDRESS             | Kafka bootstrap address                                     |           | localhost:9092     | kafka-url:9092
KAFKA_ERROR_ERROR_TOPIC                   | name of the error topic to consume from                     | ✓         |                    | insolvency-delta-error
KAFKA_ERROR_RETRY_TOPIC                   | name of the retry topic to send to                          | ✓         |                    | insolvency-delta-retry
KAFKA_ERROR_CONSUMER_GROUP_ID             | name of kafka consumer group ID to use                      |           | kafka-error        | kafka-error-2
KAFKA_ERROR_START_OFFSET                  | offset to start consuming from                              |           | (uses last offset) | 389
KAFKA_ERROR_END_OFFSET                    | offset to consume to                                        |           | (to last record)   | 389
KAFKA_ERROR_PARTITION                     | partition to consume from                                   |           | 0                  | 1
LOGGER_NAMESPACE                          | namespace for CH structured logging                         | ✓         |                    | insolvency-delta-error-consumer
#
## ECR container
### What does this code do?
The code present in this repository is used to define and deploy a __kafka-error-consumer__ dockerised container image in AWS ECS.

This is done by calling a [module](https://github.com/companieshouse/terraform-modules/tree/main/aws/ecs) from terraform-modules. Application specific attributes are injected and the service is then deployed using Terraform via the CICD platform 'Concourse'. 

Once creating the image for this service, the kafka-error-consumer will be used accross twelve delta consumer services (like for example [insolvency-delta-consumer](https://ci-platform.companieshouse.gov.uk/teams/team-development/pipelines/insolvency-delta-consumer)).  In this "delta-consumer" service, the kafka-error-consumer image is implemented as a pipeline resource called kafka-error-release-tag, displaying its functionality inside the insolvency-delta-consumer service pipeline.

#
Application specific attributes | Value                                | Description
:---------|:-----------------------------------------------------------------------------|:-----------
**Concourse pipeline**     |[Pipeline link](https://ci-platform.companieshouse.gov.uk/teams/team-development/pipelines/kafka-error-consumer) <br> [Pipeline code](https://github.com/companieshouse/ci-pipelines/blob/master/pipelines/ssplatform/team-development/kafka-error-consumer)                               | Concourse pipeline link in shared services


#
### Contributing
- Please refer to the [ECS Development and Infrastructure Documentation](https://companieshouse.atlassian.net/wiki/spaces/DEVOPS/pages/4390649858/Copy+of+ECS+Development+and+Infrastructure+Documentation+Updated) for detailed information on the infrastructure being deployed.
