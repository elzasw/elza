package cz.tacr.elza.service.ai;

/**
 * Context of the request output being mapped to display blocks, for mappers
 * whose rendering depends on it (e.g. merging per-request proposal decisions).
 *
 * @param aiRequestId id of the {@code ai_request} whose output is mapped
 * @param blockIndex 0-based index of the mapped block within the stored output
 *            array — part of the stable addressing of a block's content (e.g.
 *            {@code AiProposalChange.changeKey})
 */
public record AiBlockContext(Integer aiRequestId, int blockIndex) {
}
