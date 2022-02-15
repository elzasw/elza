package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApCachedAccessPoint;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApCachedAccessPointRepository extends ElzaJpaRepository<ApCachedAccessPoint, Integer>, ApCachedAccessPointRepositoryCustom {

    @Query("SELECT cap FROM ap_cached_access_point cap WHERE cap.accessPoint.accessPointId = :accessPointId")
    ApCachedAccessPoint findByAccessPointId(@Param("accessPointId") Integer accessPointId);

    @Query("SELECT cap FROM ap_cached_access_point cap WHERE cap.accessPoint.accessPointId IN :accessPointIds")
    List<ApCachedAccessPoint> findByAccessPointIds(@Param("accessPointIds") Collection<Integer> accessPointIds);
}
