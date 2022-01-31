package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApRevItemRepository extends JpaRepository<ApRevItem, Integer> {

    @Query("SELECT i FROM ApRevItem i LEFT JOIN FETCH i.data d WHERE i.part IN :parts AND i.deleteChange IS NULL")
    List<ApRevItem> findByParts(@Param("parts") Collection<ApRevPart> parts);

    @Query("SELECT i FROM ApRevItem i LEFT JOIN FETCH i.data d WHERE i.part = :part AND i.deleteChange IS NULL")
    List<ApRevItem> findByPart(@Param("part") ApRevPart part);
}
