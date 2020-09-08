package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApIndexRepository extends JpaRepository<ApIndex, Integer> {


    @Query("SELECT i FROM ap_index i JOIN FETCH i.part p JOIN p.accessPoint ap WHERE ap.accessPointId = :accessPointId AND p.deleteChange IS NULL")
    List<ApIndex> findIndicesByAccessPoint(@Param("accessPointId") Integer accessPointId);

    @Query("SELECT i FROM ap_index i WHERE i.part.partId = :partId")
    List<ApIndex> findByPartId(@Param("partId") Integer partId);

    @Query("SELECT i FROM ap_index i WHERE i.part IN :parts AND i.indexType = :indexType")
    List<ApIndex> findByPartsAndIndexType(@Param("parts") Collection<ApPart> parts, @Param("indexType") String indexType);
}
