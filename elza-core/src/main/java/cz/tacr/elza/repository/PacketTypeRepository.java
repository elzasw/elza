package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPacketType;

import java.util.List;

/**
 * Repository pro typ obalu.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PacketTypeRepository extends ElzaJpaRepository<RulPacketType, Integer> {

    List<RulPacketType> findByRulPackage(RulPackage rulPackage);


    void deleteByRulPackage(RulPackage rulPackage);

    RulPacketType findByCode(String packetTypeCode);
}
