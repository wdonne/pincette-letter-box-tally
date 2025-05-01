package net.pincette.letterbox.tally;

import static java.lang.System.getenv;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getEncoder;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static net.pincette.json.JsonUtil.getString;
import static net.pincette.json.JsonUtil.string;
import static net.pincette.letterbox.tally.Common.LETTER_BOX_TALLY;
import static net.pincette.letterbox.tally.Common.VERSION;
import static net.pincette.util.Collections.map;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.tryToGetRethrow;

import com.typesafe.config.Config;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.json.JsonObject;
import net.pincette.letterbox.kafka.Publisher;
import net.pincette.letterbox.kafka.Publisher.Context;
import net.pincette.netty.http.HttpServer;

public class Server implements AutoCloseable {
  private static final String FORM = "form";
  private static final String FORM_ID = "/data/formId";
  private static final String HEADER = "Tally-Signature";
  private static final String INSTANCE_ENV = "INSTANCE";
  private static final String SIGNING_SECRET = "signingSecret";

  private final Config config;
  private final HttpServer httpServer;
  private final int port;
  private final Publisher publisher;

  @SuppressWarnings("java:S2095") // It is closed in the close method.
  private Server(final int port, final Config config) {
    this.port = port;
    this.config = config;
    publisher =
        config != null
            ? new Publisher()
                .withConfig(config)
                .withServiceName(LETTER_BOX_TALLY)
                .withServiceVersion(VERSION)
                .withInstance(
                    ofNullable(getenv(INSTANCE_ENV)).orElseGet(() -> randomUUID().toString()))
                .withVerify(verify(mac(config.getString(SIGNING_SECRET))))
                .withTelemetryAttributes(telemetryAttributes())
            : null;
    httpServer =
        port != -1 && publisher != null ? new HttpServer(port, publisher.requestHandler()) : null;
  }

  public Server() {
    this(-1, null);
  }

  private static Mac mac(final String secret) {
    return tryToGetRethrow(
            () -> {
              final Mac mac = Mac.getInstance("HmacSHA256");

              mac.init(new SecretKeySpec(secret.getBytes(US_ASCII), "HmacSHA256"));

              return mac;
            })
        .orElse(null);
  }

  private static Predicate<Context> verify(final Mac mac) {
    return context ->
        ofNullable(context.request().headers().get(HEADER))
            .filter(signature -> verifySignature(context.message(), signature, mac))
            .isPresent();
  }

  private static boolean verifySignature(
      final JsonObject json, final String signature, final Mac mac) {
    return getEncoder()
        .encodeToString(mac.doFinal(string(json, false).getBytes(UTF_8)))
        .equals(signature);
  }

  public void close() {
    httpServer.close();
    publisher.close();
  }

  public CompletionStage<Boolean> run() {
    return httpServer.run();
  }

  public void start() {
    httpServer.start();
  }

  private Function<Context, Map<String, String>> telemetryAttributes() {
    return context -> map(pair(FORM, getString(context.message(), FORM_ID).orElse("")));
  }

  public Server withConfig(final Config config) {
    return new Server(port, config);
  }

  public Server withPort(final int port) {
    return new Server(port, config);
  }
}
