package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.AiRequestEvent;

/**
 * Repository of AI request events (the exchange log of a request).
 */
@Repository
public interface AiRequestEventRepository extends JpaRepository<AiRequestEvent, Integer> {

    List<AiRequestEvent> findByAiRequestIdOrderByCreateDateAsc(Integer aiRequestId);

    /**
     * Events of several requests in one query, in stable per-request order (the
     * id breaks create-date ties, so a tool call always precedes its result).
     */
    List<AiRequestEvent> findByAiRequestIdInOrderByCreateDateAscAiRequestEventIdAsc(
            Collection<Integer> aiRequestIds);
}
