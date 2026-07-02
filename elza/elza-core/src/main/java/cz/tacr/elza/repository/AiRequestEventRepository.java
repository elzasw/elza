package cz.tacr.elza.repository;

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
}
