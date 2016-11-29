package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RegExternalSystem;

/**
 * Repository externích systémů rejstříků/osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface RegExternalSystemRepository extends JpaRepository<RegExternalSystem, Integer> {

    RegExternalSystem findExternalSystemByCode(String externalSystemCode);
}
