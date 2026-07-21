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

    /** True when the wire tool name is a client tool Elza executes itself. */
    public boolean isClientTool(final String wireName) {
        if (wireName == null) {
            return false;
        }
        for (StandardToolName name : byName.keySet()) {
            if (wireName.equals(name.getValue())) {
                return true;
            }
        }
        return false;
    }
}
