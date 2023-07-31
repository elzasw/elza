package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.projection.ApStateInfo;

@Repository
public interface ApStateRepository extends ElzaJpaRepository<ApState, Integer>, JpaSpecificationExecutor<ApState> {

    /*
    @Query("SELECT s FROM ap_state s WHERE s.accessPoint = :accessPoint AND s.deleteChangeId IS NULL")
    ApState findActiveByAccessPoint(@Param("accessPoint") ApAccessPoint accessPoint);

    @Query("SELECT s FROM ap_state s WHERE s.accessPoint IN :accessPoints AND s.deleteChangeId IS NULL")
    List<ApState> findActiveByAccessPoints(@Param("accessPoints") Collection<ApAccessPoint> accessPoints);
    */

    @Query("SELECT s1" +
            " FROM ap_state s1" +
            " JOIN FETCH s1.accessPoint" +
            " WHERE s1.accessPoint = :accessPoint" +
            " AND s1.createChangeId = (SELECT max(s2.createChangeId) FROM ap_state s2 WHERE s2.accessPoint = s1.accessPoint)")
    ApState findLastByAccessPoint(@Param("accessPoint") ApAccessPoint accessPoint);

    @Query("SELECT s1" +
            " FROM ap_state s1" +
            " JOIN FETCH s1.accessPoint" +
            " WHERE s1.accessPoint.accessPointId = :accessPointId" +
            " AND s1.createChangeId = (SELECT max(s2.createChangeId) FROM ap_state s2 WHERE s2.accessPoint = s1.accessPoint)")
    ApState findLastByAccessPointId(@Param("accessPointId") Integer accessPointId);

    /*
    @Query("SELECT s1" +
            " FROM ap_state s1" +
            " JOIN (SELECT s.accessPointId accessPointId, max(s.createChangeId) createChangeId FROM ap_state s WHERE s.accessPoint IN :accessPoints GROUP BY s.accessPointId) s2" +
            " ON s1.accessPointId = s2.accessPointId AND s1.createChangeId = s2.createChangeId" +
            " WHERE s1.accessPoint IN :accessPoints")
    */
    @Query("SELECT s1" +
            " FROM ap_state s1" +
            " WHERE s1.accessPoint IN :accessPoints" +
            " AND s1.createChangeId = (SELECT max(s2.createChangeId) FROM ap_state s2 WHERE s2.accessPoint = s1.accessPoint)")
    List<ApState> findLastByAccessPoints(@Param("accessPoints") Collection<ApAccessPoint> accessPoints);

    @Query("SELECT s1" +
            " FROM ap_state s1" +
            " WHERE s1.accessPoint.accessPointId IN :accessPointIds" +
            " AND s1.createChangeId = (SELECT max(s2.createChangeId) FROM ap_state s2 WHERE s2.accessPoint = s1.accessPoint)")
    List<ApState> findLastByAccessPointIds(@Param("accessPointIds") Collection<Integer> accessPointIds);

    @Query("SELECT s1" +
            " FROM ap_state s1" +
            " WHERE s1.replacedById IN :accessPointIds" +
            " AND s1.createChangeId = (SELECT max(s2.createChangeId) FROM ap_state s2 WHERE s2.accessPoint = s1.accessPoint)")
    List<ApState> findLastByReplacedByIds(@Param("accessPointIds") Collection<Integer> accessPointIds);

    @Query("SELECT COUNT(state) FROM ap_state state" +
           " WHERE state.accessPointId IN :accessPointIds" +
           " AND state.deleteChange IS NULL")
    int countValidByAccessPointIds(@Param("accessPointIds") Collection<Integer> accessPointIds);

    @Query("SELECT state.accessPointId FROM ap_state state" +
            " WHERE state.accessPointId IN :accessPointIds" +
            " AND state.createChangeId = (SELECT max(s.createChangeId) FROM ap_state s WHERE s.accessPoint = state.accessPoint)" +
            " AND state.deleteChange IS NOT NULL")
     List<Integer> findDeletedAccessPointIdsByAccessPointIds(@Param("accessPointIds") Collection<Integer> accessPointIds);

    @Modifying
    @Query("UPDATE ap_state  s SET s.apType = :value WHERE s.apType = :key")
    void updateApTypeByApType(@Param("key") ApType key, @Param("value") ApType value);

    @Modifying
    void deleteAllByScope(ApScope scope);

//    @Query("select s" +
//            " from ap_state s" +
//            " join ap_external_id eid on eid.accessPoint = s.accessPoint" +
//            " where eid.value = ?1" +
//            " and eid.externalIdType = ?2" +
//            " and eid.deleteChange is null" +
//            " and s.scope = ?3" +
//            " and s.deleteChange is null")
//    ApState getActiveByExternalIdAndScope(String eidValue, ApExternalIdType eidType, ApScope apScope);

    /**
     * Najde hesla podle třídy rejstříku.
     *
     * @param scope třída
     * @return nalezená hesla
     */
    List<ApState> findByScope(ApScope scope);

    @Query("SELECT new cz.tacr.elza.domain.projection.ApStateInfo(c.changeDate, s.stateApproval, rs.stateApproval, scope.name, tp.name, rtp.name, s.comment, rs.comment, usr)" +
            " FROM ap_change c" +
            " LEFT JOIN c.user usr" +
            " LEFT JOIN ap_state s ON s.createChangeId = c.changeId" +
            " LEFT JOIN s.apType tp" +
            " LEFT JOIN s.scope scope" +
            " LEFT JOIN ap_revision r ON r.createChangeId = c.changeId" +
            " LEFT JOIN ap_rev_state rs ON rs.createChangeId = c.changeId" +
            " LEFT JOIN rs.type rtp" +
            " WHERE s.accessPoint = :accessPoint OR rs.revisionId IN" +
            "   (SELECT revisionId FROM ap_revision WHERE stateId IN (SELECT stateId FROM ap_state WHERE accessPoint = :accessPoint))" +
            " ORDER BY c.changeId DESC")
    List<ApStateInfo> findInfoByAccessPoint(@Param("accessPoint") ApAccessPoint accessPoint);

    @Query("SELECT COUNT (s) FROM ap_state s WHERE s.accessPoint = :accessPoint AND s.comment IS NOT NULL")
    Integer countCommentsByAccessPoint(@Param("accessPoint") ApAccessPoint accessPoint);

    /**
     * Return number of valid accesspoints
     * 
     * @return
     */
    @Query("SELECT COUNT (s) FROM ap_state s WHERE s.deleteChange IS NULL")
    Integer countValid();

}
