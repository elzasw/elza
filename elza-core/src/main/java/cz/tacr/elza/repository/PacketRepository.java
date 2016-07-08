package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT p FROM arr_packet p WHERE p.fund = :fund AND upper(p.storageNumber) LIKE CONCAT(upper(:prefix), '%') AND p.state = :state ORDER BY p.storageNumber ASC")
    List<ArrPacket> findPackets(@Param("fund") ArrFund fund, @Param("prefix") String prefix, @Param("state") ArrPacket.State state);

    @Modifying
    @Query("DELETE FROM arr_packet p WHERE p.fund = :fund AND p.packetId IN :nodeIds")
    void deletePackets(@Param("fund") ArrFund fund, @Param("nodeIds") List<Integer> nodeIds);

    @Query("SELECT p.storageNumber FROM arr_packet p WHERE p.fund = :fund AND upper(p.storageNumber) LIKE CONCAT(upper(:prefix), '%') AND p.state IN :states")
    List<String> findStorageNumbers(@Param("fund") ArrFund fund, @Param("prefix") String prefix, @Param("states") List<ArrPacket.State> states);

    @Query("SELECT p.storageNumber FROM arr_packet p WHERE p.fund = :fund AND p.state IN :states")
    List<String> findStorageNumbers(@Param("fund") ArrFund fund, @Param("states") List<ArrPacket.State> states);

    @Query("SELECT p FROM arr_packet p WHERE p.fund = :fund AND p.packetId IN :nodeIds")
    List<ArrPacket> findPackets(@Param("fund") ArrFund fund, @Param("nodeIds") List<Integer> nodeIds);

}
