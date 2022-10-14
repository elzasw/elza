package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;

@Repository
public interface ApPartRepository extends JpaRepository<ApPart, Integer> {

    @Query("SELECT p FROM ApPart p WHERE p.accessPoint = :accessPoint AND p.deleteChange IS NULL")
    List<ApPart> findValidPartByAccessPoint(@Param("accessPoint") ApAccessPoint accessPoint);

    @Query("SELECT p FROM ApPart p LEFT JOIN FETCH p.keyValue WHERE p.accessPoint IN :accessPoints AND p.deleteChange IS NULL")
    List<ApPart> findValidPartByAccessPoints(@Param("accessPoints") Collection<ApAccessPoint> accessPoints);

    @Query("SELECT COUNT(p) FROM ApPart p WHERE p.parentPart = :parentPart AND p.deleteChange IS NULL")
    int countApPartsByParentPartAndDeleteChangeIsNull(@Param("parentPart") ApPart parentPart);

    @Query("SELECT p FROM ApPart p WHERE p.parentPart = :parentPart AND p.deleteChange IS NULL")
    List<ApPart> findPartsByParentPartAndDeleteChangeIsNull(@Param("parentPart") ApPart parentPart);

    @Query("SELECT part FROM ApPart part WHERE part.accessPoint.accessPointId IN (:accessPointIds)")
    List<ApPart> findPartsByAccessPointIdIn(@Param(value= "accessPointIds") Collection<Integer> accessPointIds);

    @Query("SELECT part FROM ApPart part WHERE part.accessPoint = :accessPoint AND part.deleteChange IS NULL AND part.createChange.changeId > :changeId")
    List<ApPart> findNewerValidPartsByAccessPoint(@Param("accessPoint") ApAccessPoint accessPoint, @Param("changeId") Integer changeId);

    @Query(value = "select count(*) from ap_part p" +
            " join ap_part p2 on p2.parent_part_id = p.part_id and p2.delete_change_id is null" +
            " where p.delete_change_id is not null", nativeQuery = true)
    int countDeletedPartsWithUndeletedChild();

    @Query(value = "select count(*) from ap_part p" +
            " join ap_binding_item bi on bi.part_id = p.part_id and bi.delete_change_id is null" +
            " where p.delete_change_id is not null", nativeQuery = true)
    int countDeletedPartsWithUndeletedBindingItem();

    @Query(value = "select count(*) from ap_part p" +
            " join ap_item i on i.part_id = p.part_id and i.delete_change_id is null" +
            " where p.delete_change_id is not null", nativeQuery = true)
    int countDeletedPartsWithUndeletedItem();
}
