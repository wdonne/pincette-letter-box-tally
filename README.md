# The Tally Letter Box

This endpoint follows the [letter box pattern](https://github.com/wdonne/pincette-letter-box). It delivers form submissions from the [Tally](https://tally.so) form service to a configured Kafka topic. The submissions must have the `Tally-Signature` header. The fields `_id` and `_corr` are added with a random UUID if they are not yet present in the message.

## Configuration

The configuration is managed by the [Lightbend Config package](https://github.com/lightbend/config). By default it will try to load `conf/application.conf`. An alternative configuration may be loaded by adding `-Dconfig.resource=myconfig.conf`, where the file is also supposed to be in the `conf` directory, or `-Dconfig.file=/conf/myconfig.conf`. If no configuration file is available it will load a default one from the resources. The following entries are available:

|Entry|Mandatory|Description|
|---|---|---|
|asString|No|When set to `true`, the JSON messages are serialised to Kafka as strings. Otherwise, they are serialised as compressed CBOR. The default value is `false`.|
|kafka|Yes|All Kafka settings come below this entry. So for example, the setting `bootstrap.servers` would go to the entry `kafka.bootstrap.servers`.|
|namespace|No|A name to distinguish several deployments in the same environment. The default value is `sse`.|
|otlp.grpc|No|The OpenTelemetry endpoint for logs and metrics. It should be a URL like `http://localhost:4317`.|
|signingSecret|Yes|The secret that is used to verify the submissions. You should generate it in the Tally form designer.|
|topic|Yes|The Kafka topic in which the messages are published.|
|traceSamplePercentage|No|The percentage of distributed trace samples that are retained. The value should be between 1 and 100. The default is 10. You should use the same percentage in all components that contribute to a trace, otherwise you may see incomplete traces.|
|tracesTopic|No|The Kafka topic to which the event traces are sent.|

## Telemetry

A few OpenTelemetry observable counters are emitted every minute. The following table shows the counters.

|Counter|Description|
|---|---|
|http.server.average_duration_millis|The average request duration in the measured interval.|
|http.server.average_request_bytes|The average request body size in bytes in the measured interval.|
|http.server.average_response_bytes|The average response body size in bytes in the measured interval.|
|http.server.letter_box_messages|The number of delivered letter box messages in the measured interval.|
|http.server.requests|The number of requests during the measured interval.|

The following attributes are added to the counters.

|Attribute|Description|
|---|---|
|form|The ID of the Tally form.|
|http.request.method|The request method.|
|http.response.status_code|The status code of the response.|
|instance|The UUID of the JES HTTP instance.|

The logs are also sent to the OpenTelemetry endpoint.

The event traces are JSON messages, as described in [JSON Streams Telemetry](https://jsonstreams.io/docs/logging.html). They are sent to the Kafka topic set in the `tracesTopic` configuration field.

## Building and Running

You can build the tool with `mvn clean package`. This will produce a self-contained JAR-file in the `target` directory with the form `pincette-letter-box-tally-<version>-jar-with-dependencies.jar`. You can launch this JAR with `java -jar`.

## Docker

Docker images can be found at [https://hub.docker.com/repository/docker/wdonne/pincette-letter-box-tally](https://hub.docker.com/repository/docker/wdonne/pincette-letter-box-tally).

## Kubernetes

You can mount the configuration in a `ConfigMap` and `Secret` combination. The `ConfigMap` should be mounted at `/conf/application.conf`. You then include the secret in the configuration from where you have mounted it. See also [https://github.com/lightbend/config/blob/main/HOCON.md#include-syntax](https://github.com/lightbend/config/blob/main/HOCON.md#include-syntax).