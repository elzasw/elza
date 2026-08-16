package cz.tacr.elza.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.AiProposalDecision;

/**
 * Repository for {@link AiProposalDecision} — the user's decisions on AI
 * node-update proposals.
 */
public interface AiProposalDecisionRepository extends JpaRepository<AiProposalDecision, Integer> {

    List<AiProposalDecision> findByAiRequestId(Integer aiRequestId);

    Optional<AiProposalDecision> findByAiRequestIdAndChangeKey(Integer aiRequestId, String changeKey);
}
