package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.RulPackage;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Template Repository
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 17.6.2016
 */
public interface TemplateRepository extends ElzaJpaRepository<RulTemplate, Integer> {

    List<RulTemplate> findByRulPackage(RulPackage rulPackage);

    @Query("SELECT template FROM rul_template template WHERE template.package = :rulPackage AND template.deleted = false")
    List<RulTemplate> findByRulPackageAndNotDeleted(@Param(value = "rulPackage") final RulPackage rulPackage);

    void deleteByRulPackage(RulPackage rulPackage);

    RulTemplate findByCode(String templateCode);

    List<RulTemplate> findByOutputType(RulOutputType outputType, Sort sort);

    @Query("SELECT template FROM rul_template template WHERE template.outputType = :outputType AND template.deleted = false")
    List<RulTemplate> findNotDeletedByOutputType(@Param(value = "outputType") final RulOutputType outputType, Sort sort);
}
