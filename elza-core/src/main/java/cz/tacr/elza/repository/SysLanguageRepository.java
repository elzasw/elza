package cz.tacr.elza.repository;

import cz.tacr.elza.domain.SysLanguage;

public interface SysLanguageRepository extends ElzaJpaRepository<SysLanguage, Integer> {

    SysLanguage findByCode(String code);

    /**
     * Language by its BCP 47 tag; tags are case-insensitive ({@code en-GB} = {@code en-gb}).
     */
    SysLanguage findByTagIgnoreCase(String tag);

}
