package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


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
            " JOIN nr.deleteChange dc" +
            " WHERE cc.changeId < ?2" +
            " AND (dc.changeId > ?2 OR dc.changeId is null) AND nr.node = ?1")
    List<ArrNodeRegister> findClosedVersion(ArrNode node, Integer versionLockChangeId);

}
