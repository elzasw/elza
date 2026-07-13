package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.AiRequest;

/**
 * Repository of AI task records.
 */
@Repository
public interface AiRequestRepository extends JpaRepository<AiRequest, Integer> {

    Optional<AiRequest> findByTaskUid(String taskUid);

    List<AiRequest> findByAiConversationIdOrderByCreateDateAsc(Integer aiConversationId);

    /** Open requests to resume polling for after an application start. */
    List<AiRequest> findByStateNotInAndTaskUidIsNotNull(Collection<String> states);
}
