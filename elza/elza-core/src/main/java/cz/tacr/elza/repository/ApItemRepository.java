package cz.tacr.elza.repository;

import cz.tacr.elza.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApItemRepository extends JpaRepository<ApItem, Integer> {

    @Query("SELECT COUNT(i) FROM ApItem i WHERE i.itemType = ?1")
    long getCountByType(RulItemType dbItemType);

    @Query("SELECT i FROM ApItem i LEFT JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.part = :part")
    List<ApItem> findValidItemsByPart(@Param("part") ApPart part);

    @Query("SELECT i FROM ApItem i LEFT JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.part IN :parts")
    List<ApItem> findValidItemsByParts(@Param("parts") Collection<ApPart> parts);

    @Query("SELECT i FROM ApItem i LEFT JOIN FETCH i.data d WHERE i.deleteChange IS NULL AND i.part.partId = :partId")
    List<ApItem> findValidItemsByPartId(@Param("partId") Integer partId);

    @Query("SELECT i FROM ApItem i LEFT JOIN FETCH i.data d  JOIN FETCH i.part p WHERE i.deleteChange IS NULL AND p.accessPoint = :accessPoint")
    List<ApItem> findValidItemsByAccessPoint(@Param("accessPoint") ApAccessPoint accessPoint);

    @Query("SELECT i FROM ApItem i LEFT JOIN FETCH i.data d  JOIN FETCH i.part p WHERE i.deleteChange IS NULL AND p.accessPoint IN :accessPoints")
    List<ApItem> findValidItemsByAccessPoints(@Param("accessPoints") Collection<ApAccessPoint> accessPoints);

    @Query("SELECT i FROM ApItem i LEFT JOIN FETCH i.data d  JOIN FETCH i.part p JOIN FETCH i.itemType it WHERE i.deleteChange IS NULL AND p.accessPoint = :accessPoint")
    List<ApItem> findValidItemsByAccessPointMultiFetch(@Param("accessPoint") ApAccessPoint accessPoint);
}
