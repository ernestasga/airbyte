/*
 * Copyright (c) 2022 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.workers.normalization;

import com.fasterxml.jackson.databind.JsonNode;
import io.airbyte.commons.json.Jsons;
import io.airbyte.commons.logging.MdcScope;
import io.airbyte.protocol.models.AirbyteLogMessage;
import io.airbyte.protocol.models.AirbyteMessage;
import io.airbyte.workers.internal.AirbyteStreamFactory;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a stream from an input stream. The produced stream attempts to parse each line of the
 * InputStream into a AirbyteMessage. If the line cannot be parsed into a AirbyteMessage it is
 * assumed to be from dbt. dbt [error] messages are also parsed
 *
 * <p>
 * If a line starts with a AirbyteMessage and then has other characters after it, that
 * AirbyteMessage will still be parsed. If there are multiple AirbyteMessage records on the same
 * line, only the first will be parsed.
 */
public class NormalizationAirbyteStreamFactory implements AirbyteStreamFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(NormalizationAirbyteStreamFactory.class);

  private final MdcScope.Builder containerLogMdcBuilder;
  private final Logger logger;
  private final List<String> dbtErrors = new ArrayList<>();

  public NormalizationAirbyteStreamFactory(final MdcScope.Builder containerLogMdcBuilder) {
    this(LOGGER, containerLogMdcBuilder);
  }

  NormalizationAirbyteStreamFactory(final Logger logger, final MdcScope.Builder containerLogMdcBuilder) {
    this.logger = logger;
    this.containerLogMdcBuilder = containerLogMdcBuilder;
  }

  @Override
  public Stream<AirbyteMessage> create(final BufferedReader bufferedReader) {
    return bufferedReader
        .lines()
        .flatMap(line -> {
          final Optional<JsonNode> jsonLine = Jsons.tryDeserialize(line);
          if (jsonLine.isEmpty()) {
            // we log as info all the lines that are not valid json
            // since dbt logs its process on stdout, we want to make
            // sure this info is available in the logs.
            try (final var mdcScope = containerLogMdcBuilder.build()) {
              logger.info(line);
              // this is really hacky and vulnerable to picking up lines we don't want
              // TODO: when we have dbt v1+ on every destination, activate JSON logging
              // and use that to pull out errors instead
              if (line.contains("[error]")) {
                dbtErrors.add(line);
              }
            }
          }
          return jsonLine.stream();
        })
        .flatMap(jsonLine -> {
          final Optional<AirbyteMessage> m = Jsons.tryObject(jsonLine, AirbyteMessage.class);
          if (m.isEmpty()) {
            // TODO: when we have dbt v1+ on every destination, activate JSON logging
            // and build a dbt json log parser to use here.
            logger.error("Deserialization failed: {}", Jsons.serialize(jsonLine));
          }
          return m.stream();
        })
        // filter logs
        .filter(airbyteMessage -> {
          final boolean isLog = airbyteMessage.getType() == AirbyteMessage.Type.LOG;
          if (isLog) {
            try (final var mdcScope = containerLogMdcBuilder.build()) {
              internalLog(airbyteMessage.getLog());
            }
          }
          return !isLog;
        });
  }

  public List<String> getDbtErrors() {
    return dbtErrors;
  }

  private void internalLog(final AirbyteLogMessage logMessage) {
    switch (logMessage.getLevel()) {
      case FATAL, ERROR -> logger.error(logMessage.getMessage());
      case WARN -> logger.warn(logMessage.getMessage());
      case INFO -> logger.info(logMessage.getMessage());
      case DEBUG -> logger.debug(logMessage.getMessage());
      case TRACE -> logger.trace(logMessage.getMessage());
    }
  }

}
