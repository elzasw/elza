package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.RulPackage;

import java.util.List;

/**
 * Template Repository
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 17.6.2016
 */
public interface TemplateRepository extends ElzaJpaRepository<RulTemplate, Integer> {

    List<RulTemplate> findByRulPackage(RulPackage rulPackage);

    void deleteByRulPackage(RulPackage rulPackage);

    RulTemplate findByCode(String packetTypeCode);

}
