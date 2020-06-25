package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrRefTemplate;
import cz.tacr.elza.domain.ArrRefTemplateMapType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;

public interface ArrRefTemplateMapTypeRepository extends ElzaJpaRepository<ArrRefTemplateMapType, Integer> {

    @Query("SELECT rtmt FROM arr_ref_template_map_type rtmt WHERE rtmt.refTemplate = :refTemplate")
    List<ArrRefTemplateMapType> findByRefTemplate(@Param("refTemplate") ArrRefTemplate refTemplate);

    @Query("SELECT rtmt FROM arr_ref_template_map_type rtmt WHERE rtmt.refTemplate IN :refTemplates")
    List<ArrRefTemplateMapType> findByRefTemplates(@Param("refTemplates") List<ArrRefTemplate> refTemplates);
}
