package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApAccessPointItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.service.ByType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApAccessPointItemRepository extends JpaRepository<ApAccessPointItem, Integer>, ByType<ApAccessPoint> {

    @Query("SELECT bi FROM ApAccessPointItem bi LEFT JOIN FETCH bi.data d WHERE bi.deleteChange IS NULL AND bi.accessPoint = :accessPoint")
    List<ApAccessPointItem> findValidItemsByAccessPoint(@Param("accessPoint") ApAccessPoint accessPoint);

    @Modifying
    @Query("DELETE FROM ApAccessPointItem bi WHERE bi.itemId IN (SELECT i.itemId FROM ApAccessPointItem i JOIN i.accessPoint ap WHERE ap.state = 'TEMP')")
    void removeTempItems();

    @Modifying
    @Query("DELETE FROM ApAccessPointItem bi WHERE bi.itemId IN (SELECT i.itemId FROM ApAccessPointItem i JOIN i.accessPoint ap WHERE ap.state = 'TEMP' AND i.accessPoint = :accessPoint)")
    void removeTempItems(@Param("accessPoint") ApAccessPoint accessPoint);

    @Query("SELECT bi FROM ApAccessPointItem bi LEFT JOIN FETCH bi.data d WHERE bi.deleteChange IS NULL AND bi.accessPoint = :accessPoint AND bi.itemType = :itemType")
    List<ApItem> findValidItemsByType(@Param("accessPoint") ApAccessPoint accessPoint, @Param("itemType") RulItemType itemType);
}
