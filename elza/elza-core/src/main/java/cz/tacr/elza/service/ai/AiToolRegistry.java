package cz.tacr.elza.service.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import cz.tacr.elza.aiprovider.client.vo.ToolDefinition;

/**
 * The tools Elza can execute for the provider, keyed by name. The same set is
 * declared to the provider (as {@code SubmitTask.tools}) and used to dispatch
 * incoming {@code toolCalls} — declaration and execution stay in sync.
 */
@Component
public class AiToolRegistry {

    private final Map<String, AiTool> byName = new HashMap<>();

    public AiToolRegistry(final List<AiTool> tools) {
        for (AiTool tool : tools) {
            byName.put(tool.name(), tool);
        }
    }

    /** Tool definitions to declare in {@code SubmitTask.tools}. */
    public List<ToolDefinition> toolDefinitions() {
        return byName.values().stream()
                .map(tool -> new ToolDefinition()
                        .name(tool.name())
                        .description(tool.description())
                        .inputSchema(tool.inputSchema()))
                .toList();
    }

    /** The registered tool of this name, or {@code null} if none. */
    public AiTool get(final String name) {
        return byName.get(name);
    }
}
