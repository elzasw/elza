package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.AiConversation;

/**
 * Repository of AI conversations.
 */
@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Integer> {

    List<AiConversation> findByUserIdOrderByLastChangeDateDesc(Integer userId);
}
