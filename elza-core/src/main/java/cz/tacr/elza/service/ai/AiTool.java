package cz.tacr.elza.service.ai;

import java.util.Map;

/**
 * A tool Elza executes locally on the provider's request during the
 * {@code awaiting_tools} loop. Each standard tool has a well-known name and a
 * typed argument/result shape defined by the AI provider contract (typespec-ai);
 * on the wire the generic {@code ToolDefinition} / {@code ToolCall} /
 * {@code ToolResult} envelope carries them.
 *
 * <p>Implementations are Spring beans; {@link AiToolRegistry} collects them by
 * {@link #name()}.
 */
public interface AiTool {

    /** Well-known tool name, e.g. {@code getItemTypes}. */
    String name();

    /** What the tool does and when to use it — written for the model. */
    String description();

    /**
     * JSON Schema of the tool's arguments, declared to the provider as
     * {@code ToolDefinition.inputSchema}; matches the contract's {@code …Params}
     * model.
     */
    Map<String, Object> inputSchema();

    /**
     * Executes the call and returns the result payload (a contract result
     * model), which the caller serializes into {@code ToolResult.result}.
     * Throwing turns into a {@code ToolResult.error}.
     *
     * @param arguments the call's {@code arguments} object (as received from the
     *                  provider); convert it to the tool's {@code …Params} model.
     */
    Object execute(Object arguments);
}
