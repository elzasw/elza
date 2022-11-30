package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.RulPartType;

@Repository
public interface ApIndexRepository extends JpaRepository<ApIndex, Integer> {


    @Query("SELECT i FROM ap_index i JOIN FETCH i.part p JOIN p.accessPoint ap WHERE ap.accessPointId = :accessPointId AND p.deleteChange IS NULL")
    List<ApIndex> findIndicesByAccessPoint(@Param("accessPointId") Integer accessPointId);

    @Query("SELECT i FROM ap_index i JOIN FETCH i.part p WHERE p.accessPoint IN :accessPoints AND p.deleteChange IS NULL")
    List<ApIndex> findIndicesByAccessPoints(@Param("accessPoints") Collection<ApAccessPoint> accessPoints);

    @Query("SELECT i FROM ap_index i WHERE i.part.partId = :partId")
    List<ApIndex> findByPartId(@Param("partId") Integer partId);

    @Query("SELECT i FROM ap_index i WHERE i.part IN :parts AND i.indexType = :indexType")
    List<ApIndex> findByPartsAndIndexType(@Param("parts") Collection<ApPart> parts, @Param("indexType") String indexType);

    @Query("SELECT i FROM ap_index i WHERE i.part = :part AND i.indexType = :indexType")
    ApIndex findByPartAndIndexType(@Param("part") ApPart part, @Param("indexType") String indexType);

    @Query("SELECT i FROM ap_index i JOIN i.part p WHERE p.partType = :partType AND p.deleteChange IS NULL AND i.indexType = :indexType")
    List<ApIndex> findByPartTypeAndIndexType(@Param("partType") RulPartType partType, @Param("indexType") String indexType);

    @Query("SELECT i FROM ap_index i JOIN FETCH i.part p JOIN p.accessPoint ap WHERE ap IN :accessPoints AND p = ap.preferredPart AND i.indexType = :indexType")
    List<ApIndex> findPreferredPartIndexByAccessPointsAndIndexType(@Param("accessPoints") Collection<ApAccessPoint> accessPoints, @Param("indexType") String indexType);

    @Query("SELECT i FROM ap_index i JOIN FETCH i.part p JOIN p.accessPoint ap WHERE ap IN :accessPoints AND p.partType = :partType AND p.deleteChange IS NULL AND i.indexType = :indexType")
    List<ApIndex> findPartIndexByAccessPointsAndPartTypeAndIndexType(@Param("accessPoints") Collection<ApAccessPoint> accessPoints, @Param("partType") RulPartType partType, @Param("indexType") String indexType);

    /**
     * Vyhledani vice typu partu a daneho indexu
     * 
     * Obvykle se pouzije pro zobrazeni nazvu a description k AP
     * 
     * @param accessPoints
     * @param partTypes
     * @param indexType
     * @return
     */
    @Query("SELECT i FROM ap_index i JOIN FETCH i.part p " +
            "JOIN p.accessPoint ap " +
            "WHERE ap IN :accessPoints AND p.partType IN :partTypes AND i.indexType = :indexType")
    List<ApIndex> findIndexByAccessPointsAndPartTypeAndIndexType(@Param("accessPoints") Collection<ApAccessPoint> accessPoints,
                                                                 @Param("partTypes") Collection<RulPartType> partTypes,
                                                                 @Param("indexType") String indexType);

    @Query("SELECT i FROM ap_index i JOIN FETCH i.part p JOIN p.accessPoint ap WHERE ap = :accessPoint AND p = ap.preferredPart AND i.indexType = :indexType")
    ApIndex findPreferredPartIndexByAccessPointAndIndexType(@Param("accessPoint") ApAccessPoint accessPoint, @Param("indexType") String indexType);

    @Query("SELECT i FROM ap_index i JOIN FETCH i.part p JOIN p.accessPoint ap WHERE ap.accessPointId = :accessPointId AND p = ap.preferredPart AND i.indexType = :indexType")
    ApIndex findPreferredPartIndexByAccessPointIdAndIndexType(@Param("accessPointId") Integer accessPointId, @Param("indexType") String indexType);
}
