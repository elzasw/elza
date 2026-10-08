package cz.tacr.elza.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.converter.UnitDateConverter;
import cz.tacr.elza.domain.converter.UnitDateLexicon;
import cz.tacr.elza.exception.SystemException;

/**
 * Runs the contract corpus {@code unitdate/cases.json}, shared with the client parser
 * (elza-react {@code src/components/shared/unitdate/corpus.test.ts}): every input parses to the
 * same stored form on both sides, and the server renders it in each language as the case says.
 */
public class UnitDateCorpusTest {

    public static final String CORPUS = "/unitdate/cases.json";

    static List<JsonNode> cases() throws IOException {
        try (InputStream stream = UnitDateCorpusTest.class.getResourceAsStream(CORPUS)) {
            JsonNode root = new ObjectMapper().readTree(stream);
            List<JsonNode> result = new ArrayList<>();
            root.get("cases").forEach(result::add);
            return result;
        }
    }

    @TestFactory
    List<DynamicTest> corpus() throws IOException {
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode c : cases()) {
            String input = c.get("input").asText();
            tests.add(DynamicTest.dynamicTest("\"" + input + "\"", () -> check(c)));
        }
        return tests;
    }

    private static void check(final JsonNode c) {
        String input = c.get("input").asText();
        if (c.path("error").asBoolean(false)) {
            assertThrows(SystemException.class,
                         () -> UnitDateConverter.convertToUnitDate(input, new ArrDataUnitdate()),
                         "expected an error for: " + input);
            return;
        }
        ArrDataUnitdate unitDate = UnitDateConverter.convertToUnitDate(input, new ArrDataUnitdate());
        assertEquals(c.get("format").asText(), unitDate.getFormat(), "format of " + input);
        assertEquals(c.get("from").asText(), unitDate.getValueFrom(), "from of " + input);
        assertEquals(c.get("to").asText(), unitDate.getValueTo(), "to of " + input);
        assertEquals(c.path("fromEstimated").asBoolean(false), unitDate.getValueFromEstimated(),
                     "fromEstimated of " + input);
        assertEquals(c.path("toEstimated").asBoolean(false), unitDate.getValueToEstimated(),
                     "toEstimated of " + input);
        for (UnitDateLexicon.Language language : UnitDateLexicon.get().languages()) {
            JsonNode expected = c.get(language.tag);
            if (expected != null) {
                assertEquals(expected.asText(), UnitDateConverter.convertToString(unitDate, language.tag),
                             language.tag + " text of " + input);
            }
        }
    }

    /** Every case states both texts, so a language is never left to drift. */
    @Test
    void everyValidCaseHasEveryLanguage() throws IOException {
        for (JsonNode c : cases()) {
            if (c.path("error").asBoolean(false)) {
                continue;
            }
            for (UnitDateLexicon.Language language : UnitDateLexicon.get().languages()) {
                assertEquals(true, c.has(language.tag), "case " + c.get("input").asText() + " lacks " + language.tag);
            }
        }
    }

    /** The texts the server renders parse back to the same stored form in every language. */
    @TestFactory
    List<DynamicTest> renderedTextsParseBack() throws IOException {
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode c : cases()) {
            if (c.path("error").asBoolean(false)) {
                continue;
            }
            for (UnitDateLexicon.Language language : UnitDateLexicon.get().languages()) {
                String text = c.get(language.tag).asText();
                tests.add(DynamicTest.dynamicTest(language.tag + " \"" + text + "\"", () -> {
                    ArrDataUnitdate parsed = UnitDateConverter.convertToUnitDate(text, new ArrDataUnitdate());
                    assertEquals(c.get("format").asText(), parsed.getFormat(), "format of " + text);
                    assertEquals(c.get("from").asText(), parsed.getValueFrom(), "from of " + text);
                    assertEquals(c.get("to").asText(), parsed.getValueTo(), "to of " + text);
                }));
            }
        }
        return tests;
    }
}
