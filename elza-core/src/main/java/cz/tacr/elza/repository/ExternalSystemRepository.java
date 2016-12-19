package cz.tacr.elza.repository;

import cz.tacr.elza.domain.SysExternalSystem;
import org.springframework.stereotype.Repository;

/**
 * @author Martin Šlapa
 * @since 05.12.2016
 */
@Repository
public interface ExternalSystemRepository extends ElzaJpaRepository<SysExternalSystem, Integer> {

    SysExternalSystem findByCode(String code);
}
