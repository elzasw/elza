package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.FundHits;
import cz.tacr.elza.aiprovider.client.vo.NodeHit;
import cz.tacr.elza.aiprovider.client.vo.SearchNodesResult;
import cz.tacr.elza.aiprovider.client.vo.ToolResult;
import cz.tacr.elza.config.JacksonConfig;
import cz.tacr.elza.controller.vo.AiRequestActivityVO;
import cz.tacr.elza.domain.AiRequestEvent;

/**
 * The AI provider client VOs use {@code JsonNullable} for their optional fields
 * (e.g. {@code ToolResult.result}). When Elza stores a tool result as a
 * {@code TOOL_RESULTS} transparency event it serializes such a VO through the
 * shared {@code ObjectMapper}, so that mapper MUST carry the
 * {@code JsonNullableModule} ({@link JacksonConfig}) — otherwise the
 * {@code JsonNullable} wrapper collapses to {@code {"present":true}} and the
 * search result (its funds/totalCount) is lost, which surfaced as "0 results"
 * for a search that had actually found some.
 */
class AiToolResultSerializationTest {

    private static ToolResult searchResult() {
        return new ToolResult()
                .callId("c1")
                .result(new SearchNodesResult()
                        .totalCount(4L)
                        .partial(false)
                        .funds(List.of(new FundHits()
                                .fundId(1082)
                                .name("Vojenské zajatecké tábory")
                                .count(4)
                                .nodes(List.of(new NodeHit().nodeId(210324).title("Žižka Alois"))))));
    }

    /** The activity derived from a TOOL_RESULTS event serialized by {@code storeMapper}. */
    private static AiRequestActivityVO activityFrom(final ObjectMapper storeMapper) throws Exception {
        String resultsJson = storeMapper.writeValueAsString(List.of(searchResult()));

        AiActivityMapper mapper = new AiActivityMapper();
        // Reading the stored JSON is plain JsonNode navigation — no module needed here.
        ReflectionTestUtils.setField(mapper, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(mapper, "toolRegistry", mock(AiToolRegistry.class));
        List<AiRequestActivityVO> activities = mapper.map(List.of(
                event(AiRequestEvent.TYPE_TOOL_CALLS,
                        "[{\"callId\":\"c1\",\"tool\":\"searchNodes\",\"arguments\":{\"fulltext\":\"Žižka\"}}]"),
                event(AiRequestEvent.TYPE_TOOL_RESULTS, resultsJson)), true);
        assertThat(activities).hasSize(1);
        return activities.get(0);
    }

    @Test
    void jsonNullableModulePreservesSearchResult() throws Exception {
        // The mapper configured as the application configures it: Spring Boot
        // installs the JacksonConfig JsonNullableModule bean on the ObjectMapper.
        ObjectMapper appMapper = new ObjectMapper().registerModule(new JacksonConfig().jsonNullableModule());

        AiRequestActivityVO activity = activityFrom(appMapper);

        assertThat(activity.getState()).isEqualTo(AiActivityMapper.STATE_DONE);
        assertThat(activity.getResultCount()).isEqualTo(4L);
        assertThat(activity.getLinks()).hasSize(1);
    }

    @Test
    void withoutJsonNullableModuleTheResultIsLost() throws Exception {
        // Documents the root cause: without the module the JsonNullable result
        // collapses to {"present":true}, so the count and links vanish.
        AiRequestActivityVO activity = activityFrom(new ObjectMapper());

        assertThat(activity.getResultCount()).isEqualTo(0L);
        assertThat(activity.getLinks()).isNullOrEmpty();
    }

    private static AiRequestEvent event(final String type, final String data) {
        AiRequestEvent event = new AiRequestEvent();
        event.setAiRequestId(1);
        event.setEventType(type);
        event.setData(data);
        event.setCreateDate(new Date());
        return event;
    }
}
