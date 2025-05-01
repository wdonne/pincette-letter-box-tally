package net.pincette.letterbox.tally;

import static com.typesafe.config.ConfigFactory.defaultOverrides;
import static java.lang.Integer.parseInt;
import static java.lang.System.exit;
import static net.pincette.config.Util.configValue;
import static net.pincette.jes.tel.OtelUtil.addOtelLogHandler;
import static net.pincette.jes.tel.OtelUtil.logRecordProcessor;
import static net.pincette.jes.tel.OtelUtil.otelLogHandler;
import static net.pincette.jes.util.Configuration.loadDefault;
import static net.pincette.letterbox.tally.Common.LETTER_BOX_TALLY;
import static net.pincette.letterbox.tally.Common.LOGGER;
import static net.pincette.letterbox.tally.Common.VERSION;
import static net.pincette.util.Util.initLogging;
import static net.pincette.util.Util.isInteger;
import static net.pincette.util.Util.tryToDoWithRethrow;

import com.typesafe.config.Config;

public class Application {
  private static final String NAMESPACE = "namespace";

  private static void addOtelLogger(final Config config) {
    logRecordProcessor(config)
        .flatMap(p -> otelLogHandler(namespace(config), LETTER_BOX_TALLY, VERSION, p))
        .ifPresent(h -> addOtelLogHandler(LOGGER, h));
  }

  @SuppressWarnings("java:S106") // Not logging
  public static void main(final String[] args) {
    if (args.length != 1 || !isInteger(args[0])) {
      System.err.println("Usage: net.pincette.letterbox.tally.Application port");
      exit(1);
    }

    final Config config = defaultOverrides().withFallback(loadDefault());

    initLogging();
    addOtelLogger(config);
    LOGGER.info(() -> "Version " + VERSION);
    tryToDoWithRethrow(Server::new, s -> s.withPort(parseInt(args[0])).withConfig(config).start());
  }

  private static String namespace(final Config config) {
    return configValue(config::getString, NAMESPACE).orElse(LETTER_BOX_TALLY);
  }
}
