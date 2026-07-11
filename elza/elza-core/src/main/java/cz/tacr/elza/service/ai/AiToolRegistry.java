package cz.tacr.elza.service.ai;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import cz.tacr.elza.aiprovider.client.vo.StandardToolName;

/**
 * The standard tools Elza can execute for the provider, keyed by name. The same
 * set is advertised to the provider (as {@code SubmitTask.tools}) and used to
 * dispatch incoming {@code toolCalls} — declaration and execution stay in sync.
 */
@Component
public class AiToolRegistry {

    private final Map<StandardToolName, AiTool> byName = new EnumMap<>(StandardToolName.class);

    public AiToolRegistry(final List<AiTool> tools) {
        for (AiTool tool : tools) {
            byName.put(tool.name(), tool);
        }
    }

    /** Names of the tools to advertise in {@code SubmitTask.tools}. */
    public List<StandardToolName> toolNames() {
        return List.copyOf(byName.keySet());
    }

    /** The registered tool of this name, or {@code null} if none. */
    public AiTool get(final StandardToolName name) {
        return byName.get(name);
    }
}
