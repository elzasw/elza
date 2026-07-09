package cz.tacr.elza.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import cz.tacr.elza.controller.vo.AiCitationVO;
import cz.tacr.elza.controller.vo.AiDisplayBlockVO;
import cz.tacr.elza.controller.vo.AiDocCitationsBlockVO;

/**
 * Renders an {@code elza.docCitations} result block — the knowledge/documentation
 * sources an answer relied on — as a structured {@code citations} display block.
 * The block carries only data (stable id, title, source document, resolvable
 * URL) per citation, in the order the answer first referenced them; the client
 * owns all human-readable chrome (the "Sources" heading, any fallback label) and
 * renders it in the user's language. A block with no citations produces nothing.
 */
@Component
public class DocCitationsBlockMapper implements AiBlockMapper {

    @Override
    public Set<String> objectTypes() {
        return Set.of("elza.docCitations");
    }

    @Override
    public List<AiDisplayBlockVO> map(final JsonNode data) {
        JsonNode citations = data.path("citations");
        if (!citations.isArray() || citations.isEmpty()) {
            return List.of();
        }

        List<AiCitationVO> items = new ArrayList<>();
        for (JsonNode citation : citations) {
            items.add(new AiCitationVO()
                    .fragment(citation.path("fragment").asText(""))
                    .url(text(citation, "url"))
                    .title(text(citation, "title"))
                    .source(text(citation, "source"))
                    .sourceId(text(citation, "sourceId")));
        }

        AiDisplayBlockVO block = new AiDocCitationsBlockVO().citations(items);
        return List.of(block);
    }

    /** The field's text, or {@code null} when absent or blank. */
    private static String text(final JsonNode node, final String field) {
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }
}
