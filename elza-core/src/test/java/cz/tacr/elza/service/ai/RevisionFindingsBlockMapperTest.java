package cz.tacr.elza.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.vo.AiDisplayBlockVO;
import cz.tacr.elza.controller.vo.AiFollowUpAction;
import cz.tacr.elza.controller.vo.AiMarkdownBlockVO;

/**
 * Mapping of {@code elza.revisionFindings} payloads into markdown blocks — one
 * per finding, each carrying its "prepare the fix" follow-up action (the
 * revision→fix conversation handoff: the action submits an
 * {@code elza.enhanceDescription} follow-up with a seeded instruction naming
 * the finding).
 */
public class RevisionFindingsBlockMapperTest {

    /** The rule-set names the production mapper reads from the static data. */
    private static final Map<String, String> ITEM_TYPE_NAMES = Map.of(
            "ZP2015_UNIT_CONTENT", "Obsah jednotky popisu",
            "ZP2015_RESTRICTION_ACCESS_INLINE", "Omezení přístupnosti");
    private static final Map<String, String> PART_TYPE_NAMES = Map.of(
            "PT_BODY", "Tělo");

    private final RevisionFindingsBlockMapper mapper =
            new RevisionFindingsBlockMapper(ITEM_TYPE_NAMES::get, PART_TYPE_NAMES::get);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<AiDisplayBlockVO> blocks(final String json) {
        try {
            JsonNode data = objectMapper.readTree(json);
            List<AiDisplayBlockVO> blocks = mapper.map(data);
            blocks.forEach(block -> assertTrue(block instanceof AiMarkdownBlockVO,
                    "findings map to markdown blocks"));
            return blocks;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** All blocks' markdown joined — for assertions spanning the whole rendering. */
    private String markdown(final String json) {
        return blocks(json).stream()
                .map(block -> ((AiMarkdownBlockVO) block).getContent())
                .collect(Collectors.joining("\n"));
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
        // Elements are shown by their configured names, never by rule-set codes.
        assertTrue(md.contains("Zdrojový prvek: Obsah jednotky popisu"), "source element named");
        assertFalse(md.contains("ZP2015_UNIT_CONTENT"), "the code itself is not shown");
        assertTrue(md.contains("přesunout do prvku Omezení přístupnosti"), "action + target element named");
        assertTrue(md.contains("Jistota: 85 %"));
        // A single-unit check does not repeat the node id on every finding.
        assertFalse(md.contains("JP 42"));
    }

    @Test
    public void eachFindingIsItsOwnBlockWithAPrepareTheFixAction() {
        List<AiDisplayBlockVO> blocks = blocks("""
            {"findings":[
              {"nodeId": 1, "category": "duplicate", "severity": "low",
               "excerpt": "a", "explanation": "x"},
              {"nodeId": 2, "category": "formulation", "severity": "medium",
               "excerpt": "dtto č. 42 - 52", "explanation": "y"}
            ]}""");

        assertEquals(2, blocks.size(), "one block per finding");
        for (AiDisplayBlockVO block : blocks) {
            assertEquals(1, block.getFollowUps().size(), "each finding carries its fix action");
        }
        AiFollowUpAction second = blocks.get(1).getFollowUps().get(0);
        assertEquals("Připravit opravu", second.getLabel());
        // The handoff: an enhance exchange submitted into the revision thread.
        assertEquals("elza.enhanceDescription", second.getTaskType());
        // The seed pins WHICH finding — number, category, verbatim excerpt (the
        // complete findings reach the fix task through the conversation chain).
        assertEquals("Připrav opravu zjištění č. 2 (Formulace): „dtto č. 42 - 52“.",
                second.getUserInstructions());
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
              "excerpt": "q", "explanation": "e", "action": "future_action",
              "sourceItemType": "OTHER_RULESET_ELEMENT"
            }]}""");

        assertTrue(md.contains("future_check"), "unknown category shown raw, not dropped");
        assertTrue(md.contains("future_action"), "unknown action shown raw, not dropped");
        // An element code the rule set cannot name stays visible as the code.
        assertTrue(md.contains("Zdrojový prvek: `OTHER_RULESET_ELEMENT`"), "unknown element code shown raw");
    }

    @Test
    public void entityFindingNamesThePart() {
        String md = markdown("""
            {"findings":[{
              "accessPointId": 34163, "category": "contradiction", "severity": "medium",
              "sourcePartType": "PT_BODY", "excerpt": "do roku 1445", "explanation": "e"
            }]}""");

        assertTrue(md.contains("(část Tělo)"), "part shown by its configured name");
        assertFalse(md.contains("PT_BODY"));
    }

    @Test
    public void emptyFindingsRenderTheNothingFoundMessageWithoutActions() {
        List<AiDisplayBlockVO> blocks = blocks("{\"findings\":[]}");
        assertEquals(1, blocks.size());
        assertTrue(((AiMarkdownBlockVO) blocks.get(0)).getContent()
                .contains("Nebyly nalezeny podložené sémantické ani formulační nesrovnalosti."));
        // Nothing to fix — no action button.
        assertTrue(blocks.get(0).getFollowUps().isEmpty());
        assertTrue(markdown("{}")
                .contains("Nebyly nalezeny podložené sémantické ani formulační nesrovnalosti."));
    }
}
