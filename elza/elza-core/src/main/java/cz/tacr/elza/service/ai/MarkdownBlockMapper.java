package cz.tacr.elza.service.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import cz.tacr.elza.controller.vo.AiContextAccesspointVO;
import cz.tacr.elza.controller.vo.AiContextFundVO;
import cz.tacr.elza.controller.vo.AiContextNodeVO;
import cz.tacr.elza.controller.vo.AiContextObjectVO;
import cz.tacr.elza.controller.vo.AiContextTypeVO;
import cz.tacr.elza.controller.vo.AiDisplayBlockVO;
import cz.tacr.elza.controller.vo.AiMarkdownBlockVO;
import cz.tacr.elza.controller.vo.AiRecordCitationVO;
import cz.tacr.elza.controller.vo.AiRecordCitationsBlockVO;

/**
 * Renders an {@code elza.markdown} result block as a markdown display block and,
 * when the text links to archival records, a {@code RECORD_CITATIONS} block
 * listing them.
 *
 * <p>A record link is the provider protocol's way of citing a record of the
 * user's own archive (see {@code MarkdownPayload} in the AI provider contract):
 * an ordinary Markdown link whose target is a record <em>reference</em> —
 * {@code ap:<accessPointId>} (an entity / access point), {@code node:<nodeId>}
 * (a description level) or {@code fund:<fundId>} (a fund) — because the provider
 * knows neither this installation's addresses nor its routes. The markdown is
 * passed through unchanged (the client resolves the references onto its routes
 * when rendering); the records block is derived from the links here, so the
 * client gets a ready list without parsing the text itself. The provider emits a
 * record link only for a record the task actually received or retrieved under
 * the user's permissions, which is why the references are trusted without a
 * second lookup.
 */
@Component
public class MarkdownBlockMapper implements AiBlockMapper {

    /** A record link as the provider emits it: {@code [label](ap:1234)}, kinds ap / node / fund. */
    static final Pattern RECORD_LINK = Pattern.compile("\\[([^\\]\\n]*)\\]\\((ap|node|fund):(\\d+)\\)");

    private static final String KIND_ENTITY = "ap";
    private static final String KIND_NODE = "node";
    private static final String KIND_FUND = "fund";

    @Override
    public Set<String> objectTypes() {
        return Set.of("elza.markdown");
    }

    @Override
    public List<AiDisplayBlockVO> map(final JsonNode data) {
        String markdown = data.path("markdown").asText("");
        List<AiDisplayBlockVO> blocks = new ArrayList<>();
        blocks.add(new AiMarkdownBlockVO().content(markdown));
        List<AiRecordCitationVO> records = recordCitations(markdown);
        if (!records.isEmpty()) {
            blocks.add(new AiRecordCitationsBlockVO().records(records));
        }
        return blocks;
    }

    /**
     * The records the markdown links to, in first-referenced order and
     * de-duplicated by reference; each with its link text as the label (null when
     * the link carried none — the client shows a generic label) and a navigation
     * target in the UI-context vocabulary.
     */
    static List<AiRecordCitationVO> recordCitations(final String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return List.of();
        }
        Map<String, AiRecordCitationVO> byReference = new LinkedHashMap<>();
        Matcher matcher = RECORD_LINK.matcher(markdown);
        while (matcher.find()) {
            String kind = matcher.group(2);
            int id;
            try {
                id = Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException e) {
                continue; // beyond int range — not an Elza id
            }
            String reference = kind + ":" + id;
            if (byReference.containsKey(reference)) {
                continue;
            }
            AiContextObjectVO target = target(kind, id);
            if (target != null) {
                byReference.put(reference, new AiRecordCitationVO().label(label(matcher.group(1))).target(target));
            }
        }
        return new ArrayList<>(byReference.values());
    }

    private static AiContextObjectVO target(final String kind, final int id) {
        if (KIND_ENTITY.equals(kind)) {
            return new AiContextAccesspointVO().accessPointId(id).type(AiContextTypeVO.ACCESSPOINT);
        }
        if (KIND_NODE.equals(kind)) {
            return new AiContextNodeVO().nodeId(id).type(AiContextTypeVO.NODE);
        }
        if (KIND_FUND.equals(kind)) {
            return new AiContextFundVO().fundId(id).type(AiContextTypeVO.FUND);
        }
        return null;
    }

    /** The link text as a plain label: surrounding emphasis / code markers stripped; null when empty. */
    private static String label(final String text) {
        if (text == null) {
            return null;
        }
        String label = text.trim().replaceAll("^[*_`]+|[*_`]+$", "").trim();
        return label.isEmpty() ? null : label;
    }
}
