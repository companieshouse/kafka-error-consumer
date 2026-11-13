# kafka-error-consumer

## Introduction

A generic consumer application to move messages from a Kafka error topic to a retry topic.

This application will move records from its configured error topic into the configured retry topic.

The default behaviour is to resume from the committed offset and replay through the last record present when the first record is consumed. The application then pauses consumption, but keeps running so that it is not restarted by the infrastructure. Explicit start and end offsets override these defaults; the end offset is inclusive.

New consumers will start at the earliest offset (`ConsumerConfig.AUTO_OFFSET_RESET_CONFIG = "earliest"`).

Example 

- The last time it ran, the application consumed records from the error topic up to 55.
- When it consumes the first record, the last available record has offset 450.
- The application will move records from the error topic, between offsets 56 and 450, to the retry topic and then stop consuming.

This application processes one configured partition of the error topic, defaulting to partition 0. Set `KAFKA_ERROR_PARTITION` to select another partition. Offsets are independent on each partition.

## Requirements
In order to build kafka-error-consumer locally you will need the following:
- [Java 21](https://www.oracle.com/java/technologies/downloads/#java11)
- [Maven](https://maven.apache.org/download.cgi)
- [Git](https://git-scm.com/downloads)

## Getting started
1. Run mvn clean install
2. Run mvn spring-boot:run

## Automated replay tests

Run the unit, embedded Kafka and container integration tests with:

```bash
mvn clean verify
```

The container tests require a running Docker daemon. Testcontainers creates an
isolated Kafka broker, random host ports, and unique topics and consumer groups.
The tests do not connect to your `chs-dev` broker or use its messages. Docker
must support pausing containers for the broker-interruption scenario.

The integration suites cover:

- Exact binary payload forwarding and the replay timestamp header.
- Inclusive start/end offsets, ordered replay, and no forwarding after pausing.
- The default cutoff captured on the first consumed record, including startup
  with an empty topic and messages arriving after the cutoff.
- Selection of a non-default partition without consuming another partition.
- Restarting from committed progress and explicitly replaying earlier offsets.
- Recovery of an in-flight message after a brief broker interruption.
- Deployment environment-variable binding, mandatory topic validation, defaults,
  and the health endpoint remaining UP after replay pauses.

Run just the container integration tests with `make test-integration`, or:

```bash
mvn verify -Dskip.unit.tests=true
```

Run the existing fast tests without containers with:

```bash
mvn test -Dskip.integration.tests=true
```

The container broker is pinned to `confluentinc/cp-kafka:7.8.0` (Kafka 3.8).
Use the same image before and after a framework upgrade so that broker changes
do not obscure application regressions. Override it with
`-Dkafka.test.image=confluentinc/cp-kafka:<version>` (Confluent 7.4 or newer).
This suite does not replace a smoke test against the actual deployment broker,
including the legacy Kafka 2.8 broker in `chs-dev`.

Surefire results are written to `target/surefire-reports`; container integration
results are written to `target/failsafe-reports`. Use `verify`, not just
`integration-test`, so Failsafe failures fail the build.

The interruption test covers a transient outage followed by recovery, not
exactly-once delivery or permanent producer rejection. Those guarantees are not
established by these tests.

## Running Locally

Kafka-error-consumer is configured in docker-chs-project, the docker-compose file includes all the listed environment variables, and can be configured for local running there.

Enable the following services with `chs-dev services enable`:
- kafka-error-consumer
- kafka3
- zookeeper

**Using and configuring kafka locally:**

Enter into the kafka container using `docker exec -it docker-chs-development-kafka3-1 bash`

Cd into the folder with the kafka scripts using `cd /opt/bitnami/kafka/bin`, here you can run any of the scrips and it will print out the help information for that script

You can check what topics have been created in your kafka instance using `./kafka-topics.sh --bootstrap-server localhost:9092 --list`
this will allow you to confirm both your error queue and retry queue exist for your local testing

You can push a message onto the kafka error topic you are using (in this example I am using insolvency-delta-error) 
with the following command `./kafka-console-producer.sh --bootstrap-server localhost:9092 --topic insolvency-delta-error`.
You can then type a message, press enter, and exit the command with `command + c`

You can check the content of the topic using `./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic insolvency-delta-error --from-beginning`

To wait for new messages on insolvency-delta-retry, run this inside the Kafka container:
`./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic insolvency-delta-retry`

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
The code present in this repository is used to define and deploy a __kafka-error-consumer__ dockerised container image in AWS ECR, via the CICD platform 'Concourse'.

The same kafka-error-consumer image will be used across 12 delta consumer services (for example [insolvency-delta-consumer](https://ci-platform.companieshouse.gov.uk/teams/team-development/pipelines/insolvency-delta-consumer)).  In this "delta-consumer" service, the kafka-error-consumer image is implemented as a pipeline resource called [kafka-error-release-tag](https://github.com/companieshouse/ci-pipelines/blob/7e0cfd7c9db47d0323e87f0956549796ef12d5a7/pipelines/ssplatform/team-development/insolvency-delta-consumer#L1274), which is used to deploy a corresponding kafka-error service alongside the delta consumer. This is done via terraform in an additional ecs-service-kafka-error module, within the insolvency-delta-consumer terraform code.

#
Application specific attributes | Value                                | Description
:---------|:-----------------------------------------------------------------------------|:-----------
**Concourse pipeline**     |[Pipeline link](https://ci-platform.companieshouse.gov.uk/teams/team-development/pipelines/kafka-error-consumer) <br> [Pipeline code](https://github.com/companieshouse/ci-pipelines/blob/master/pipelines/ssplatform/team-development/kafka-error-consumer)                               | Concourse pipeline link in shared services


#
### Contributing
- Please refer to the [ECS Development and Infrastructure Documentation](https://companieshouse.atlassian.net/wiki/spaces/DEVOPS/pages/4390649858/Copy+of+ECS+Development+and+Infrastructure+Documentation+Updated) for detailed information on the infrastructure being deployed.
