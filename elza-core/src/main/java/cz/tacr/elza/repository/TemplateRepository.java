package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.RulPackage;
import org.springframework.data.domain.Sort;

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

    RulTemplate findByCode(String templateCode);

    List<RulTemplate> findByOutputType(RulOutputType outputType, Sort sort);
}
