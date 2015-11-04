package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.ArrPacketType;

/**
 * Repository pro typ obalu.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PacketTypeRepository extends JpaRepository<ArrPacketType, Integer> {

}
