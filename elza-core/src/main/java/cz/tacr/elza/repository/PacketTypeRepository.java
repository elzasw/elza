package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPacketType;

/**
 * Repository pro typ obalu.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PacketTypeRepository extends JpaRepository<RulPacketType, Integer> {

    List<RulPacketType> findByRulPackage(RulPackage rulPackage);


    void deleteByRulPackage(RulPackage rulPackage);
    ArrPacketType findByCode(String packetTypeCode);

}
