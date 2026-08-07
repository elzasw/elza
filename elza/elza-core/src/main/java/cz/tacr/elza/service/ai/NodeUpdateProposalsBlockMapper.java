package cz.tacr.elza.service.ai;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import cz.tacr.elza.controller.vo.AiDisplayBlockVO;

/**
 * Renders an {@code elza.nodeUpdateProposals} result block (the proposed
 * description changes of {@code elza.enhanceDescription},
 * tasks/elza-enhance-description.md) as the interactive
 * {@code NODE_UPDATE_PROPOSALS} display block: per-change cards with old→new
 * diff rows, validated against the level's current state and merged with the
 * user's persisted decisions. The heavy lifting lives in
 * {@link AiProposalService}, which also executes the apply/reject decisions
 * over the same addressing ({@code changeKey}).
 */
@Component
public class NodeUpdateProposalsBlockMapper implements AiBlockMapper {

    private final AiProposalService proposalService;

    public NodeUpdateProposalsBlockMapper(final AiProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @Override
    public Set<String> objectTypes() {
        return Set.of(AiProposalService.OBJECT_TYPE);
    }

    @Override
    public List<AiDisplayBlockVO> map(final JsonNode data) {
        return map(data, null);
    }

    @Override
    public List<AiDisplayBlockVO> map(final JsonNode data, final AiBlockContext context) {
        return List.of(proposalService.toBlock(data, context));
    }
}
