package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApExternalSystem;

/**
 * Repository externích systémů rejstříků/osob.
 */
@Repository
public interface ApExternalSystemRepository extends JpaRepository<ApExternalSystem, Integer> {

    ApExternalSystem findByCode(String code);
}
