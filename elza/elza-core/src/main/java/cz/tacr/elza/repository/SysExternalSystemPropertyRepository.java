package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.SysExternalSystemProperty;
import cz.tacr.elza.domain.UsrUser;

public interface SysExternalSystemPropertyRepository extends ElzaJpaRepository<SysExternalSystemProperty, Integer> {

	List<SysExternalSystemProperty> findByExternalSystemAndUser(ApExternalSystem externalSystem, UsrUser user);

	List<SysExternalSystemProperty> findByExternalSystemIdAndUserId(Integer externalSystemId, Integer userId);

    List<SysExternalSystemProperty> findByUserId(Integer userId);

	List<SysExternalSystemProperty> findByExternalSystemId(Integer externalSystemId);
}
