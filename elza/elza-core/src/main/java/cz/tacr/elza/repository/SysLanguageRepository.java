package cz.tacr.elza.repository;

import cz.tacr.elza.domain.SysLanguage;

public interface SysLanguageRepository extends ElzaJpaRepository<SysLanguage, Integer> {

    SysLanguage findByCode(String code);

}
