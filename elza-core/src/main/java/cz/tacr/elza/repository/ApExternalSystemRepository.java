package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApExternalSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository externích systémů rejstříků/osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface ApExternalSystemRepository extends JpaRepository<ApExternalSystem, Integer> {

    ApExternalSystem findExternalSystemByCode(String externalSystemCode);
}
