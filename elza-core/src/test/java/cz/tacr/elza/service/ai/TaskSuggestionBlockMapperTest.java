package cz.tacr.elza.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.vo.AiDisplayBlockVO;
import cz.tacr.elza.controller.vo.AiFollowUpAction;
import cz.tacr.elza.controller.vo.AiMarkdownBlockVO;

/**
 * Mapping of {@code elza.taskSuggestion} payloads (provider contract 0.18.0)
 * onto the follow-up action transport: one markdown block whose text is the
 * reason and whose single follow-up carries the suggested task, instruction
 * and — for a resumed run — the continuation token.
 */
public class TaskSuggestionBlockMapperTest {

    private final TaskSuggestionBlockMapper mapper = new TaskSuggestionBlockMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AiMarkdownBlockVO single(final String json) {
        try {
            JsonNode data = objectMapper.readTree(json);
            List<AiDisplayBlockVO> blocks = mapper.map(data);
            assertEquals(1, blocks.size());
            assertTrue(blocks.get(0) instanceof AiMarkdownBlockVO);
            return (AiMarkdownBlockVO) blocks.get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void chatSuggestionBecomesAFollowUpWithTheReasonAsText() {
        AiMarkdownBlockVO block = single("{\"taskType\":\"elza.enhanceDescription\","
                + "\"label\":\"Připravit úpravu: doplnit dataci\","
                + "\"userInstructions\":\"Doplň prvek Datace hodnotou 1920–1935.\","
                + "\"reason\":\"Datace je uvedena jen v textu.\"}");

        assertEquals("Datace je uvedena jen v textu.", block.getContent());
        assertEquals(1, block.getFollowUps().size());
        AiFollowUpAction action = block.getFollowUps().get(0);
        assertEquals("Připravit úpravu: doplnit dataci", action.getLabel());
        assertEquals("elza.enhanceDescription", action.getTaskType());
        assertEquals("Doplň prvek Datace hodnotou 1920–1935.", action.getUserInstructions());
        assertEquals("Datace je uvedena jen v textu.", action.getReason());
        assertNull(action.getContinuation());
    }

    @Test
    public void continuationTokenRidesTheAction() {
        AiMarkdownBlockVO block = single("{\"taskType\":\"elza.revision\","
                + "\"label\":\"Pokračovat v kontrole (další část)\","
                + "\"userInstructions\":\"Pokračuj v kontrole větve další částí.\","
                + "\"continuation\":\"b1:24:4711\"}");

        assertEquals("", block.getContent());
        AiFollowUpAction action = block.getFollowUps().get(0);
        assertEquals("elza.revision", action.getTaskType());
        assertEquals("b1:24:4711", action.getContinuation());
        assertNull(action.getReason());
    }

    @Test
    public void suggestionWithoutLabelOrInstructionRendersNoButton() {
        AiMarkdownBlockVO block = single("{\"taskType\":\"elza.chat\",\"label\":\"\","
                + "\"userInstructions\":\"x\",\"reason\":\"Jen důvod.\"}");

        assertEquals("Jen důvod.", block.getContent());
        assertTrue(block.getFollowUps() == null || block.getFollowUps().isEmpty());
    }
}
