package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.AiExternalSystem;

/**
 * Repository AI provider systémů.
 */
@Repository
public interface AiExternalSystemRepository extends JpaRepository<AiExternalSystem, Integer> {

}
