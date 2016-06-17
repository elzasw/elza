package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulPackage;

import java.util.List;

/**
 * Output Type Repository
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 17.6.2016
 */
public interface OutputTypeRepository extends ElzaJpaRepository<RulOutputType, Integer> {

    List<RulOutputType> findByRulPackage(RulPackage rulPackage);

    void deleteByRulPackage(RulPackage rulPackage);

    RulOutputType findByCode(String packetTypeCode);

}
