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
## Terraform ECS
### What does this code do?
The code present in this repository is used to define and deploy a dockerised container in AWS ECS.
This is done by calling a [module](https://github.com/companieshouse/terraform-modules/tree/main/aws/ecs) from terraform-modules. Application specific attributes are injected and the service is then deployed using Terraform via the CICD platform 'Concourse'.
Application specific attributes | Value                                | Description
:---------|:-----------------------------------------------------------------------------|:-----------
**ECS Cluster**        |utility                                      | ECS cluster stack the service belongs to
**Load balancer** | N/A - consumer service | The load balancer that sits in front of the service
**Concourse pipeline**     |[Pipeline link](https://ci-platform.companieshouse.gov.uk/teams/team-development/pipelines/kafka-error-consumer) <br> [Pipeline code](https://github.com/companieshouse/ci-pipelines/blob/master/pipelines/ssplatform/team-development/kafka-error-consumer)                               | Concourse pipeline link in shared services
### Contributing
- Please refer to the [ECS Development and Infrastructure Documentation](https://companieshouse.atlassian.net/wiki/spaces/DEVOPS/pages/4390649858/Copy+of+ECS+Development+and+Infrastructure+Documentation+Updated) for detailed information on the infrastructure being deployed.
### Testing
- Ensure the terraform runner local plan executes without issues. For information on terraform runners please see the [Terraform Runner Quickstart guide](https://companieshouse.atlassian.net/wiki/spaces/DEVOPS/pages/1694236886/Terraform+Runner+Quickstart).
- If you encounter any issues or have questions, reach out to the team on the **#platform** slack channel.
### Vault Configuration Updates
- Any secrets required for this service will be stored in Vault. For any updates to the Vault configuration, please consult with the **#platform** team and submit a workflow request.
### Useful Links
- [ECS service config dev repository](https://github.com/companieshouse/ecs-service-configs-dev)
- [ECS service config production repository](https://github.com/companieshouse/ecs-service-configs-production
