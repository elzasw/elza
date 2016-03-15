package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrPacket;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Repository
public interface PacketRepository extends JpaRepository<ArrPacket, Integer>, PacketRepositoryCustom {

    ArrPacket findByFundAndStorageNumber(ArrFund fund, String storageNumber);

    List<ArrPacket> findByFund(ArrFund fund);
}
