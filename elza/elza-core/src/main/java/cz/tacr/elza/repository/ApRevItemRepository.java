package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;

@Repository
public interface ApRevItemRepository extends JpaRepository<ApRevItem, Integer> {

    @Query("SELECT i FROM ApRevItem i LEFT JOIN FETCH i.data d WHERE i.part IN :parts AND i.deleteChange IS NULL")
    List<ApRevItem> findByParts(@Param("parts") Collection<ApRevPart> parts);

    @Query("SELECT i FROM ApRevItem i LEFT JOIN FETCH i.data d WHERE i.part = :part AND i.deleteChange IS NULL")
    List<ApRevItem> findByPart(@Param("part") ApRevPart part);

    @Query("SELECT i FROM ApRevItem i JOIN FETCH i.part p JOIN FETCH p.revision r JOIN FETCH r.state s JOIN FETCH s.accessPoint JOIN FETCH i.data JOIN arr_data_record_ref d ON i.data = d WHERE d.record = :record AND i.deleteChange IS NULL")
    List<ApRevItem> findItemByEntity(@Param("record") ApAccessPoint replaced);
}
