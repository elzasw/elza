package cz.tacr.elza.repository;

import java.util.List;


import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPacketType;

/**
 * Repository pro typ obalu.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PacketTypeRepository extends ElzaJpaRepository<RulPacketType, Integer> {

    List<RulPacketType> findByRulPackage(RulPackage rulPackage);


    void deleteByRulPackage(RulPackage rulPackage);

    RulPacketType findByCode(String packetTypeCode);

    @Override
    default String getClassName() {
        return RulPacketType.class.getSimpleName();
    }
}
