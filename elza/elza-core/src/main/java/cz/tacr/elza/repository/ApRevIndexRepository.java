package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApRevIndex;
import cz.tacr.elza.domain.ApRevPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApRevIndexRepository extends JpaRepository<ApRevIndex, Integer> {

    @Query("SELECT i FROM ApRevIndex i WHERE i.part IN :parts AND i.deleteChange IS NULL")
    List<ApRevIndex> findByParts(@Param("parts") List<ApRevPart> parts);

    @Query("SELECT i FROM ApRevIndex i WHERE i.part.partId = :partId")
    List<ApRevIndex> findByPartId(@Param("partId") Integer partId);
}
