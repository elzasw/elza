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
 * result size, hit links), error results, and resilience to unreadable
 * payloads.
 */
class AiActivityMapperTest {

    private final AiActivityMapper mapper = new AiActivityMapper();

    AiActivityMapperTest() {
        ReflectionTestUtils.setField(mapper, "objectMapper", new ObjectMapper());
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

        List<AiRequestActivityVO> activities = mapper.map(List.of(calls, results));

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

        List<AiRequestActivityVO> activities = mapper.map(List.of(calls, results));

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

        List<AiRequestActivityVO> activities = mapper.map(List.of(calls));

        assertThat(activities).hasSize(1);
        assertThat(activities.get(0).getState()).isEqualTo(AiActivityMapper.STATE_RUNNING);
        assertThat(activities.get(0).getEndDate()).isNull();
    }

    @Test
    void failedToolBecomesError() {
        AiRequestEvent calls = event(AiRequestEvent.TYPE_TOOL_CALLS, """
                [{"callId":"c4","tool":"searchNodes","arguments":{"fulltext":"x"}}]
                """, 1000);
        AiRequestEvent results = event(AiRequestEvent.TYPE_TOOL_RESULTS, """
                [{"callId":"c4","error":"search failed"}]
                """, 2000);

        List<AiRequestActivityVO> activities = mapper.map(List.of(calls, results));

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

        List<AiRequestActivityVO> activities = mapper.map(List.of(calls, stray, malformed));

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

        assertThat(mapper.map(List.of(submit, output))).isEmpty();
    }
}
