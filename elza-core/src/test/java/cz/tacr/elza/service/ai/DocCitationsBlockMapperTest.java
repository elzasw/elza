package cz.tacr.elza.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.vo.AiCitationVO;
import cz.tacr.elza.controller.vo.AiDisplayBlockVO;
import cz.tacr.elza.controller.vo.AiDocCitationsBlockVO;

/** Mapping of {@code elza.docCitations} payloads into a structured citations display block. */
public class DocCitationsBlockMapperTest {

    private final DocCitationsBlockMapper mapper = new DocCitationsBlockMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode data(final String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void emitsStructuredCitationsInOrder() {
        JsonNode data = data("""
            {"citations":[
              {"fragment":"zp2015/a","url":"https://ex/a","title":"Title A","source":"Source A","sourceId":"zp2015"},
              {"fragment":"frag/b","source":"Source B"}
            ]}""");

        List<AiDisplayBlockVO> blocks = mapper.map(data);

        assertEquals(1, blocks.size());
        AiDisplayBlockVO raw = blocks.get(0);
        assertTrue(raw instanceof AiDocCitationsBlockVO, "elza.docCitations maps to a DOC_CITATIONS block");
        AiDocCitationsBlockVO block = (AiDocCitationsBlockVO) raw;

        List<AiCitationVO> citations = block.getCitations();
        assertEquals(2, citations.size());

        AiCitationVO first = citations.get(0);
        assertEquals("zp2015/a", first.getFragment());
        assertEquals("https://ex/a", first.getUrl());
        assertEquals("Title A", first.getTitle());
        assertEquals("Source A", first.getSource());
        assertEquals("zp2015", first.getSourceId());

        AiCitationVO second = citations.get(1);
        assertEquals("frag/b", second.getFragment());
        assertEquals("Source B", second.getSource());
        // Absent optional fields are left null for the client to resolve/localize.
        assertNull(second.getUrl());
        assertNull(second.getTitle());
        assertNull(second.getSourceId());
    }

    @Test
    public void producesNothingForEmptyCitations() {
        assertTrue(mapper.map(data("{\"citations\":[]}")).isEmpty());
        assertTrue(mapper.map(data("{}")).isEmpty());
    }
}
