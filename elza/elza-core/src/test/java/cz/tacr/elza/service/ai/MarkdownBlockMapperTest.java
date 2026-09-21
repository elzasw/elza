package cz.tacr.elza.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.vo.AiContextAccesspointVO;
import cz.tacr.elza.controller.vo.AiContextFundVO;
import cz.tacr.elza.controller.vo.AiContextNodeVO;
import cz.tacr.elza.controller.vo.AiContextTypeVO;
import cz.tacr.elza.controller.vo.AiDisplayBlockVO;
import cz.tacr.elza.controller.vo.AiMarkdownBlockVO;
import cz.tacr.elza.controller.vo.AiRecordCitationVO;
import cz.tacr.elza.controller.vo.AiRecordCitationsBlockVO;

/**
 * Mapping of {@code elza.markdown} payloads: the markdown passes through unchanged
 * (record links included — the client resolves them), and the record links become
 * a {@code RECORD_CITATIONS} block with navigable targets.
 */
public class MarkdownBlockMapperTest {

    private final MarkdownBlockMapper mapper = new MarkdownBlockMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode data(final String markdown) {
        return objectMapper.createObjectNode().put("markdown", markdown);
    }

    @Test
    public void derivesRecordCitationsFromRecordLinks() {
        String markdown = "Entita [**klokani (savci)**](ap:12) je v registru, znovu [klokani](ap:12); "
                + "úroveň [Kronika školy](node:7), fond [AM Krnov](fund:3), zdroj [ZP](https://ex/a) "
                + "a prázdný [](node:8).";

        List<AiDisplayBlockVO> blocks = mapper.map(data(markdown));

        assertEquals(2, blocks.size());
        assertTrue(blocks.get(0) instanceof AiMarkdownBlockVO, "the text is the first block");
        assertEquals(markdown, ((AiMarkdownBlockVO) blocks.get(0)).getContent(), "markdown passes through unchanged");
        assertTrue(blocks.get(1) instanceof AiRecordCitationsBlockVO, "record links map to a RECORD_CITATIONS block");

        List<AiRecordCitationVO> records = ((AiRecordCitationsBlockVO) blocks.get(1)).getRecords();
        assertEquals(4, records.size(), "de-duplicated by reference, first-referenced order");

        AiRecordCitationVO entity = records.get(0);
        assertEquals("klokani (savci)", entity.getLabel(), "emphasis markers are stripped from the label");
        assertTrue(entity.getTarget() instanceof AiContextAccesspointVO);
        assertEquals(AiContextTypeVO.ACCESSPOINT, entity.getTarget().getType());
        assertEquals(Integer.valueOf(12), ((AiContextAccesspointVO) entity.getTarget()).getAccessPointId());

        AiRecordCitationVO level = records.get(1);
        assertEquals("Kronika školy", level.getLabel());
        assertTrue(level.getTarget() instanceof AiContextNodeVO);
        assertEquals(AiContextTypeVO.NODE, level.getTarget().getType());
        assertEquals(Integer.valueOf(7), ((AiContextNodeVO) level.getTarget()).getNodeId());

        AiRecordCitationVO fund = records.get(2);
        assertEquals("AM Krnov", fund.getLabel());
        assertTrue(fund.getTarget() instanceof AiContextFundVO);
        assertEquals(AiContextTypeVO.FUND, fund.getTarget().getType());
        assertEquals(Integer.valueOf(3), ((AiContextFundVO) fund.getTarget()).getFundId());

        // An empty link text yields no label — the client supplies a generic one.
        assertNull(records.get(3).getLabel());
        assertEquals(Integer.valueOf(8), ((AiContextNodeVO) records.get(3).getTarget()).getNodeId());
    }

    @Test
    public void plainMarkdownYieldsOnlyTheMarkdownBlock() {
        List<AiDisplayBlockVO> blocks = mapper.map(data("Jen text s [odkazem](https://ex/a) a `ap:12` v kódu."));

        assertEquals(1, blocks.size());
        assertTrue(blocks.get(0) instanceof AiMarkdownBlockVO);
    }

    @Test
    public void missingMarkdownRendersAsEmptyText() {
        List<AiDisplayBlockVO> blocks = mapper.map(objectMapper.createObjectNode());

        assertEquals(1, blocks.size());
        assertEquals("", ((AiMarkdownBlockVO) blocks.get(0)).getContent());
    }
}
