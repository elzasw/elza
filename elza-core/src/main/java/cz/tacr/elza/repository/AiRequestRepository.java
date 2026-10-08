package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.AiRequest;
import jakarta.persistence.LockModeType;

/**
 * Repository of AI task records.
 */
@Repository
public interface AiRequestRepository extends JpaRepository<AiRequest, Integer> {

    Optional<AiRequest> findByTaskUid(String taskUid);

    List<AiRequest> findByAiConversationIdOrderByCreateDateAsc(Integer aiConversationId);
    /** Removes a conversation's exchanges (the conversation delete; children first). */
    void deleteByAiConversationId(Integer aiConversationId);

    /** Open requests to resume polling for after an application start. */
    List<AiRequest> findByStateNotInAndTaskUidIsNotNull(Collection<String> states);

    /** All non-terminal requests, including those never submitted; startup reconciliation. */
    List<AiRequest> findByStateNotIn(Collection<String> states);

    /**
     * Loads the request for a read-modify-write, holding its row lock
     * ({@code SELECT … FOR UPDATE}) until the surrounding transaction ends. Two
     * threads write a live request — the task poll (state, output) and the event
     * poll (cursor, progress) — and each saves the whole entity; loaded with a
     * plain {@code findById}, the later loader overwrote the earlier writer's
     * committed columns with its own stale copy (2026-09-17). The lock has to be
     * taken by the read itself: locking an already loaded entity keeps its stale
     * snapshot. Must run inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AiRequest> findForUpdateByAiRequestId(Integer aiRequestId);
}
