package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApRevIndex;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ApRevision;

@Repository
public interface ApRevIndexRepository extends JpaRepository<ApRevIndex, Integer> {

    @Query("SELECT i FROM ap_rev_index i WHERE i.part IN :parts")
    List<ApRevIndex> findByParts(@Param("parts") List<ApRevPart> parts);

    @Query("SELECT i FROM ap_rev_index i WHERE i.part.revision=:revision")
    List<ApRevIndex> findByRevision(@Param("revision") ApRevision rev);

    @Query("SELECT i FROM ap_rev_index i WHERE i.part = :part")
    List<ApRevIndex> findByPart(@Param("part") ApRevPart part);

    @Query("SELECT i FROM ap_rev_index i WHERE i.part IN :parts AND i.indexType = :indexType")
    List<ApRevIndex> findByPartsAndIndexType(@Param("parts") Collection<ApRevPart> parts, @Param("indexType") String indexType);
}
