package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApExternalSystem;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository externích systémů rejstříků/osob.
 */
@Repository
public interface ApExternalSystemRepository extends JpaRepository<ApExternalSystem, Integer> {

    ApExternalSystem findByCode(String code);
}
