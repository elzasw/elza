package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBodyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApBodyItemRepository extends JpaRepository<ApBodyItem, Integer> {

    @Query("SELECT bi FROM ApBodyItem bi LEFT JOIN FETCH bi.data d WHERE bi.deleteChange IS NULL AND bi.accessPoint = :accessPoint")
    List<ApBodyItem> findValidItemsByAccessPoint(@Param("accessPoint") ApAccessPoint accessPoint);
}
