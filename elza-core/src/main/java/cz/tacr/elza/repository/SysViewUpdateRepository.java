package cz.tacr.elza.repository;

import cz.tacr.elza.domain.SysViewUpdate;

public interface SysViewUpdateRepository extends ElzaJpaRepository<SysViewUpdate, Integer> {

	SysViewUpdate findByViewName(String viewName);
}
