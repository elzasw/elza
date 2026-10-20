package cz.tacr.elza.service.ai;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import cz.tacr.elza.controller.vo.AiDisplayBlockVO;
import cz.tacr.elza.controller.vo.AiFollowUpAction;
import cz.tacr.elza.controller.vo.AiMarkdownBlockVO;

/**
 * Renders an {@code elza.taskSuggestion} result block (provider contract
 * 0.18.0 — a follow-up the provider proposes as the conversation's next step:
 * the chat handing over a drafted edit as an {@code elza.enhanceDescription}
 * exchange, a stopped branch check offering its next part) as one markdown
 * block carrying the suggestion as an {@link AiFollowUpAction}: the transport
 * the per-finding "Připravit opravu" buttons already ride, so the panel renders
 * the action with no new UI. The block's text is the provider's {@code reason}
 * (why the step is suggested), the button its {@code label}; clicking submits
 * the suggested task with the current context, or — with a
 * {@code continuation} — resumes the previous exchange's run. Nothing runs by
 * itself.
 *
 * <p>A block without a label or an instruction cannot be acted on; it is
 * rendered as its reason (or nothing) and logged, never as a dead button.
 */
@Component
public class TaskSuggestionBlockMapper implements AiBlockMapper {

    private static final Logger logger = LoggerFactory.getLogger(TaskSuggestionBlockMapper.class);

    @Override
    public Set<String> objectTypes() {
        return Set.of("elza.taskSuggestion");
    }

    @Override
    public List<AiDisplayBlockVO> map(final JsonNode data) {
        String label = data.path("label").asText("").trim();
        String instructions = data.path("userInstructions").asText("").trim();
        String reason = data.path("reason").asText("").trim();
        AiMarkdownBlockVO block = new AiMarkdownBlockVO().content(reason);
        if (label.isEmpty() || instructions.isEmpty()) {
            logger.warn("AI task suggestion without a label or instruction is not actionable: {}", data);
            return List.of(block);
        }
        AiFollowUpAction action = new AiFollowUpAction()
                .label(label)
                .taskType(StringUtils.defaultIfBlank(data.path("taskType").asText(null), null))
                .userInstructions(instructions)
                .reason(reason.isEmpty() ? null : reason)
                .continuation(StringUtils.defaultIfBlank(data.path("continuation").asText(null), null));
        block.addFollowUpsItem(action);
        return List.of(block);
    }
}
