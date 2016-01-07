package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrPacket;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Repository
public interface PacketRepository extends JpaRepository<ArrPacket, Integer>, PacketRepositoryCustom {

    ArrPacket findByFindingAidAndStorageNumber(ArrFindingAid findingAid, String storageNumber);

    List<ArrPacket> findByFindingAid(ArrFindingAid findingAid);
}
