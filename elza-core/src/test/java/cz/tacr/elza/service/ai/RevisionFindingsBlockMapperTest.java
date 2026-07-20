package cz.tacr.elza.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.vo.AiDisplayBlockVO;
import cz.tacr.elza.controller.vo.AiMarkdownBlockVO;

/** Mapping of {@code elza.revisionFindings} payloads into a readable markdown block. */
public class RevisionFindingsBlockMapperTest {

    private final RevisionFindingsBlockMapper mapper = new RevisionFindingsBlockMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String markdown(final String json) {
        try {
            JsonNode data = objectMapper.readTree(json);
            List<AiDisplayBlockVO> blocks = mapper.map(data);
            assertEquals(1, blocks.size());
            assertTrue(blocks.get(0) instanceof AiMarkdownBlockVO, "findings map to a markdown block");
            return ((AiMarkdownBlockVO) blocks.get(0)).getContent();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void rendersFullFinding() {
        String md = markdown("""
            {"findings":[{
              "nodeId": 42,
              "category": "hidden_data",
              "severity": "high",
              "confidence": 85,
              "sourceItemType": "ZP2015_UNIT_CONTENT",
              "excerpt": "nepřístupné do roku 2030",
              "explanation": "Omezení přístupnosti je zapsáno ve volném textu.",
              "targetItemType": "ZP2015_RESTRICTION_ACCESS_INLINE",
              "action": "move",
              "proposedText": "Přesunout do prvku omezení přístupnosti."
            }]}""");

        assertTrue(md.contains("Skryté strukturované údaje"), "category is labeled");
        assertTrue(md.contains("závažnost vysoká"), "severity is labeled");
        assertTrue(md.contains("> nepřístupné do roku 2030"), "excerpt is quoted verbatim");
        assertTrue(md.contains("Omezení přístupnosti je zapsáno ve volném textu."));
        assertTrue(md.contains("`ZP2015_UNIT_CONTENT`"), "source item code shown");
        assertTrue(md.contains("přesunout do prvku `ZP2015_RESTRICTION_ACCESS_INLINE`"), "action + target element");
        assertTrue(md.contains("Jistota: 85 %"));
        // A single-unit check does not repeat the node id on every finding.
        assertFalse(md.contains("JP 42"));
    }

    @Test
    public void showsNodeIdsOnlyWhenFindingsSpanSeveralLevels() {
        String md = markdown("""
            {"findings":[
              {"nodeId": 1, "category": "duplicate", "severity": "low",
               "excerpt": "a", "explanation": "x"},
              {"nodeId": 2, "category": "formulation", "severity": "medium",
               "excerpt": "b", "explanation": "y"}
            ]}""");

        assertTrue(md.contains("(JP 1)"));
        assertTrue(md.contains("(JP 2)"));
        assertTrue(md.contains("Duplicitní údaj"));
        assertTrue(md.contains("závažnost střední"));
    }

    @Test
    public void unknownCodesOfTheOpenSetsAreShownRaw() {
        String md = markdown("""
            {"findings":[{
              "nodeId": 7, "category": "future_check", "severity": "high",
              "excerpt": "q", "explanation": "e", "action": "future_action"
            }]}""");

        assertTrue(md.contains("future_check"), "unknown category shown raw, not dropped");
        assertTrue(md.contains("future_action"), "unknown action shown raw, not dropped");
    }

    @Test
    public void emptyFindingsRenderTheNothingFoundMessage() {
        assertTrue(markdown("{\"findings\":[]}")
                .contains("Nebyly nalezeny podložené sémantické ani formulační nesrovnalosti."));
        assertTrue(markdown("{}")
                .contains("Nebyly nalezeny podložené sémantické ani formulační nesrovnalosti."));
    }
}
