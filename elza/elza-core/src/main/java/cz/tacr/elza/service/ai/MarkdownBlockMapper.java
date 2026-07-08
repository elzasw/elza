package cz.tacr.elza.service.ai;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import cz.tacr.elza.controller.vo.AiDisplayBlockVO;

/** Renders an {@code elza.markdown} result block as a markdown display block. */
@Component
public class MarkdownBlockMapper implements AiBlockMapper {

    @Override
    public Set<String> objectTypes() {
        return Set.of("elza.markdown");
    }

    @Override
    public List<AiDisplayBlockVO> map(final JsonNode data) {
        AiDisplayBlockVO block = new AiDisplayBlockVO();
        block.setType("markdown");
        block.setContent(data.path("markdown").asText(""));
        return List.of(block);
    }
}
