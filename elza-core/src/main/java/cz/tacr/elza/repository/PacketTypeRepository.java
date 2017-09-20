package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;

import java.util.List;

/**
 * Repository pro typ obalu.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PacketTypeRepository extends ElzaJpaRepository<RulPacketType, Integer> {

    List<RulPacketType> findByRulPackage(RulPackage rulPackage);

    List<RulPacketType> findByRulPackageAndRuleSet(RulPackage rulPackage, RulRuleSet rulRuleSet);

    void deleteByRulPackage(RulPackage rulPackage);

    RulPacketType findByCode(String packetTypeCode);
}
