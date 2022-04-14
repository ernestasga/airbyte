package io.airbyte.integrations.source.sftp.parsers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.csv.CsvReadException;
import com.google.common.collect.ImmutableMap;
import io.airbyte.commons.json.Jsons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvFileParserTest {
    public static final String LOG_FILE_CSV = "log-test.csv";
    public static final String INVALID_LOG_FILE_JSON = "invalid-test.csv";
    private final CsvFileParser csvFileParser = new CsvFileParser();
    private JsonNode expectedFirstNode;
    private JsonNode expectedSecondNode;

    @BeforeEach
    void setUp() {
        expectedFirstNode = Jsons.jsonNode(ImmutableMap.builder()
                .put("id", "1")
                .put("log", "text1")
                .put("created_at", "04192022")
                .build());
        expectedSecondNode = Jsons.jsonNode(ImmutableMap.builder()
                .put("id", "2")
                .put("log", "text2")
                .put("created_at", "04202022")
                .build());
    }

    @Test
    void parseFileTest() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(LOG_FILE_CSV);

        List<JsonNode> jsonNodes = csvFileParser.parseFile(new ByteArrayInputStream(stream.readAllBytes()));
        assertNotNull(jsonNodes);
        assertEquals(2, jsonNodes.size());
        assertEquals(expectedFirstNode, jsonNodes.get(0));
        assertEquals(expectedSecondNode, jsonNodes.get(1));
    }

    @Test
    void parseFileFirstLineTest() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(LOG_FILE_CSV);

        JsonNode jsonNode = csvFileParser.parseFileFirstEntity(new ByteArrayInputStream(stream.readAllBytes()));
        assertNotNull(jsonNode);
        assertEquals(expectedFirstNode, jsonNode);
    }

    @Test()
    void parseInvalidFileTest() throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(INVALID_LOG_FILE_JSON);

        CsvReadException thrown = assertThrows(
                CsvReadException.class,
                () -> csvFileParser.parseFile(new ByteArrayInputStream(stream.readAllBytes())),
                "Expected doThing() to throw, but it didn't"
        );

        assertTrue(thrown.getMessage().contains("Too many entries: expected at"));
    }
}