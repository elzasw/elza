package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParInstitution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParInstitutionRepository extends JpaRepository<ParInstitution, Integer> {

    Optional<ParInstitution> findByInternalCode(String code);
}
