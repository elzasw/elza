package cz.tacr.elza.service.ai;

import cz.tacr.elza.aiprovider.client.vo.StandardToolName;

/**
 * A standard tool Elza executes locally on the provider's request during the
 * {@code awaiting_tools} loop. The tool's name and its typed argument/result
 * shape are defined by the AI provider contract (typespec-ai); this interface is
 * just the executor. The provider owns the tool's model-facing prompt and derives
 * the argument schema from the contract, so Elza only advertises the name and
 * runs the call. Implementations are Spring beans, collected by
 * {@link AiToolRegistry}.
 */
public interface AiTool {

    /** The contract tool this bean implements. */
    StandardToolName name();

    /**
     * Executes the call and returns the result payload (a contract result
     * model), which the caller serializes into {@code ToolResult.result}.
     * Throwing turns into a {@code ToolResult.error}.
     *
     * @param arguments the call's {@code arguments} object (as received from the
     *                  provider); convert it to the tool's argument model.
     * @param context   on whose behalf the call runs — the poller executes tools
     *                  outside the request security context, so a tool touching
     *                  permission-scoped data enforces the user's permissions
     *                  itself. Tools serving permission-free data ignore it.
     */
    Object execute(Object arguments, AiToolContext context);
}
