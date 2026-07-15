package cz.tacr.elza.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.controller.vo.AiContextNodeVO;
import cz.tacr.elza.controller.vo.AiContextTypeVO;
import cz.tacr.elza.controller.vo.AiRequestActivityVO;
import cz.tacr.elza.domain.AiRequestEvent;

/**
 * Event → activity derivation in {@link AiActivityMapper}: pairing of tool
 * calls with their results by {@code callId}, the per-tool summaries (query,
 * result size, hit links), error results, settling of still-running steps once
 * the exchange has finished, and resilience to unreadable payloads.
 */
class AiActivityMapperTest {

    private final AiActivityMapper mapper = new AiActivityMapper();

    AiActivityMapperTest() {
        ReflectionTestUtils.setField(mapper, "objectMapper", new ObjectMapper());
        // Empty registry: no tool counts as a client tool, so a provider-stream
        // tool_call is kept (never skipped) — the same shape as the real bug,
        // where the wire's snake_case name failed to match a client tool.
        ReflectionTestUtils.setField(mapper, "toolRegistry", new AiToolRegistry(List.of()));
    }

    private static AiRequestEvent event(final String type, final String data, final long time) {
        AiRequestEvent event = new AiRequestEvent();
        event.setAiRequestEventId((int) time);
        event.setAiRequestId(1);
        event.setEventType(type);
        event.setData(data);
        event.setCreateDate(new Date(time));
        return event;
    }

    @Test
    void searchNodesCallAndResult() {
        AiRequestEvent calls = event(AiRequestEvent.TYPE_TOOL_CALLS, """
                [{"callId":"c1","tool":"searchNodes","arguments":{"fulltext":"krnov","limit":20}}]
                """, 1000);
        AiRequestEvent results = event(AiRequestEvent.TYPE_TOOL_RESULTS, """
                [{"callId":"c1","result":{
                    "totalCount":12,"partial":true,
                    "funds":[{"fundId":7,"name":"Fond A","count":12,"nodes":[
                        {"nodeId":41,"title":"Krnov - listiny"},
                        {"nodeId":42,"referenceMark":["1","2","3"]}
                    ]}]}}]
                """, 2000);

        List<AiRequestActivityVO> activities = mapper.map(List.of(calls, results), false);

        assertThat(activities).hasSize(1);
        AiRequestActivityVO activity = activities.get(0);
        assertThat(activity.getId()).isEqualTo("c1");
        assertThat(activity.getKind()).isEqualTo(AiActivityMapper.KIND_TOOL_CALL);
        assertThat(activity.getTool()).isEqualTo("searchNodes");
        assertThat(activity.getState()).isEqualTo(AiActivityMapper.STATE_DONE);
        assertThat(activity.getQuery()).isEqualTo("krnov");
        assertThat(activity.getResultCount()).isEqualTo(12L);
        assertThat(activity.getPartial()).isTrue();
        assertThat(activity.getStartDate()).isNotNull();
        assertThat(activity.getEndDate()).isNotNull();

        assertThat(activity.getLinks()).hasSize(2);
        assertThat(activity.getLinks().get(0).getLabel()).isEqualTo("Krnov - listiny");
        AiContextNodeVO target = (AiContextNodeVO) activity.getLinks().get(0).getTarget();
        assertThat(target.getType()).isEqualTo(AiContextTypeVO.NODE);
        assertThat(target.getFundId()).isEqualTo(7);
        assertThat(target.getNodeId()).isEqualTo(41);
        // no title: the reference designation becomes the label
        assertThat(activity.getLinks().get(1).getLabel()).isEqualTo("1 2 3");
    }

    @Test
    void getItemTypesCallAndResult() {
        AiRequestEvent calls = event(AiRequestEvent.TYPE_TOOL_CALLS, """
                [{"callId":"c2","tool":"getItemTypes","arguments":{"ruleSetCode":"ZP2015"}}]
                """, 1000);
        AiRequestEvent results = event(AiRequestEvent.TYPE_TOOL_RESULTS, """
                [{"callId":"c2","result":{"ruleSetCode":"ZP2015","itemTypes":[{},{},{}]}}]
                """, 2000);

        List<AiRequestActivityVO> activities = mapper.map(List.of(calls, results), false);

        assertThat(activities).hasSize(1);
        AiRequestActivityVO activity = activities.get(0);
        assertThat(activity.getQuery()).isEqualTo("ZP2015");
        assertThat(activity.getState()).isEqualTo(AiActivityMapper.STATE_DONE);
        assertThat(activity.getResultCount()).isEqualTo(3L);
        assertThat(activity.getLinks()).isEmpty();
    }

    @Test
    void pendingCallStaysRunning() {
        AiRequestEvent calls = event(AiRequestEvent.TYPE_TOOL_CALLS, """
                [{"callId":"c3","tool":"searchNodes","arguments":{"fulltext":"praha"}}]
                """, 1000);

        List<AiRequestActivityVO> activities = mapper.map(List.of(calls), false);

        assertThat(activities).hasSize(1);
        assertThat(activities.get(0).getState()).isEqualTo(AiActivityMapper.STATE_RUNNING);
        assertThat(activities.get(0).getEndDate()).isNull();
    }

    @Test
    void runningStepsSettleWhenExchangeFinished() {
        // A provider-stream client-tool call never receives a tool_result event,
        // so it stays RUNNING while the exchange runs; once the exchange has
        // finished it must not keep spinning (the reported bug).
        AiRequestEvent providerCall = event(AiRequestEvent.TYPE_PROVIDER_TOOL_CALL, """
                {"seq":6,"tool":"search_nodes","createdAt":"2026-07-15T07:43:26.021534Z"}
                """, 1000);

        assertThat(mapper.map(List.of(providerCall), false).get(0).getState())
                .isEqualTo(AiActivityMapper.STATE_RUNNING);

        List<AiRequestActivityVO> finished = mapper.map(List.of(providerCall), true);
        assertThat(finished).hasSize(1);
        assertThat(finished.get(0).getState()).isEqualTo(AiActivityMapper.STATE_DONE);
    }

    @Test
    void failedResultSurvivesExchangeFinish() {
        // A step that finished with an error keeps its error state — the
        // finished-exchange settling only touches still-running steps.
        AiRequestEvent calls = event(AiRequestEvent.TYPE_TOOL_CALLS, """
                [{"callId":"c4","tool":"searchNodes","arguments":{"fulltext":"x"}}]
                """, 1000);
        AiRequestEvent results = event(AiRequestEvent.TYPE_TOOL_RESULTS, """
                [{"callId":"c4","error":"search failed"}]
                """, 2000);

        List<AiRequestActivityVO> activities = mapper.map(List.of(calls, results), true);

        assertThat(activities.get(0).getState()).isEqualTo(AiActivityMapper.STATE_ERROR);
        assertThat(activities.get(0).getError()).isEqualTo("search failed");
    }

    @Test
    void failedToolBecomesError() {
        AiRequestEvent calls = event(AiRequestEvent.TYPE_TOOL_CALLS, """
                [{"callId":"c4","tool":"searchNodes","arguments":{"fulltext":"x"}}]
                """, 1000);
        AiRequestEvent results = event(AiRequestEvent.TYPE_TOOL_RESULTS, """
                [{"callId":"c4","error":"search failed"}]
                """, 2000);

        List<AiRequestActivityVO> activities = mapper.map(List.of(calls, results), false);

        assertThat(activities.get(0).getState()).isEqualTo(AiActivityMapper.STATE_ERROR);
        assertThat(activities.get(0).getError()).isEqualTo("search failed");
    }

    @Test
    void unknownToolAndStrayResultAreTolerated() {
        AiRequestEvent calls = event(AiRequestEvent.TYPE_TOOL_CALLS, """
                [{"callId":"c5","tool":"futureTool","arguments":{"foo":1}}]
                """, 1000);
        AiRequestEvent stray = event(AiRequestEvent.TYPE_TOOL_RESULTS, """
                [{"callId":"unknown","result":{}}]
                """, 2000);
        AiRequestEvent malformed = event(AiRequestEvent.TYPE_TOOL_CALLS, "not-json", 3000);

        List<AiRequestActivityVO> activities = mapper.map(List.of(calls, stray, malformed), false);

        assertThat(activities).hasSize(1);
        AiRequestActivityVO activity = activities.get(0);
        assertThat(activity.getTool()).isEqualTo("futureTool");
        assertThat(activity.getQuery()).isNull();
        assertThat(activity.getState()).isEqualTo(AiActivityMapper.STATE_RUNNING);
    }

    @Test
    void nonToolEventsAreIgnored() {
        AiRequestEvent submit = event(AiRequestEvent.TYPE_SUBMIT, "{\"taskType\":\"elza.chat\"}", 1000);
        AiRequestEvent output = event(AiRequestEvent.TYPE_OUTPUT, "[]", 2000);

        assertThat(mapper.map(List.of(submit, output), false)).isEmpty();
    }
}
