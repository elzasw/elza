package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


/**
 * Respozitory pro vazby mezi node a heslem rejstříku.
 */
@Repository
public interface NodeRegisterRepository extends JpaRepository<ArrNodeRegister, Integer> {

    /**
     * Pro otevženou verzi.
     * @param node  node
     * @return                          kolekce propojení nodu a hesla
     */
    List<ArrNodeRegister> findByNodeAndDeleteChangeIsNull(ArrNode node);

    /**
     * Pro zavřenou verzi.
     *
     * @param node                      node
     * @param versionLockChangeId       id zamykací změny
     * @return                          kolekce propojení nodu a hesla
     */
    @Query("SELECT nr FROM arr_node_register nr" +
            " JOIN nr.createChange cc" +
            " LEFT JOIN nr.deleteChange dc" +
            " WHERE cc.changeId < ?2" +
            " AND (dc.changeId > ?2 OR dc.changeId is null) AND nr.node = ?1")
    List<ArrNodeRegister> findClosedVersion(ArrNode node, Integer versionLockChangeId);

    /**
     * Vazby uzlu na heslo.
     * @param record    heslo
     * @return          množina vazeb, nebo prázdná
     */
    @Query("SELECT nr FROM arr_node_register nr WHERE nr.record = ?1")
    List<ArrNodeRegister> findByRecordId(ApAccessPoint record);

    /**
     * Počet vazeb uzlu na heslo.
     * @param record    heslo
     * @return          počet vazeb
     */
    @Query("SELECT COUNT(nr) FROM arr_node_register nr WHERE nr.record = ?1")
    long countByRecordId(ApAccessPoint record);

    //List<ArrNodeRegister> findByNode(ArrNode node);

    @Query("SELECT record FROM arr_node_register nr WHERE nr.node = ?1")
    List<ApAccessPoint> findRecordsByNode(ArrNode node);

    List<ArrNodeRegister> findByNodeIdInAndDeleteChangeIsNull(Collection<Integer> nodeIds);

    /**
     * Hledá v otevřené verzi pomocí rejstříku
     * @param record rejstřík
     * @return seznam Node registrů v otevřené verzi
     */
    @Query("SELECT nr FROM arr_node_register nr JOIN FETCH nr.record WHERE nr.deleteChange IS NULL AND nr.record = ?1")
    List<ArrNodeRegister> findByRecordAndDeleteChangeIsNull(ApAccessPoint record);

    /**
     * Hledá počet v otevřené verzi pomocí rejstříku
     * @param record rejstřík
     * @return počet Node registrů v otevřené verzi
     */
    @Query("SELECT COUNT(nr) FROM arr_node_register nr WHERE nr.deleteChange IS NULL AND nr.record = ?1")
    long countByRecordAndDeleteChangeIsNull(ApAccessPoint record);

    void deleteByNodeFund(ArrFund fund);
}
