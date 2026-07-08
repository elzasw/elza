package cz.tacr.elza.service.ai;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import cz.tacr.elza.controller.vo.AiDisplayBlockVO;

/**
 * Renders an {@code elza.text} result block as a markdown display block (plain
 * text is valid CommonMark). Used by the {@code elza.echo} integration task.
 */
@Component
public class TextBlockMapper implements AiBlockMapper {

    @Override
    public Set<String> objectTypes() {
        return Set.of("elza.text");
    }

    @Override
    public List<AiDisplayBlockVO> map(final JsonNode data) {
        AiDisplayBlockVO block = new AiDisplayBlockVO();
        block.setType("markdown");
        block.setContent(data.path("text").asText(""));
        return List.of(block);
    }
}
