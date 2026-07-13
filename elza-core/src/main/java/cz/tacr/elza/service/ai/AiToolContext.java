package cz.tacr.elza.service.ai;

/**
 * Execution context of one standard-tool call: on whose behalf the tool runs.
 * The poller executes tool calls outside the request security context, so the
 * conversation owner's identity travels explicitly; a tool that touches
 * permission-scoped data (e.g. {@code searchNodes}) must enforce that user's
 * permissions itself.
 *
 * @param userId the conversation owner's user id; {@code null} for the virtual
 *               admin account (which has no user row and full permissions)
 */
public record AiToolContext(Integer userId) {
}
