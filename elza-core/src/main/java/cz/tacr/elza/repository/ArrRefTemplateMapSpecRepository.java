package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrRefTemplate;
import cz.tacr.elza.domain.ArrRefTemplateMapSpec;
import cz.tacr.elza.domain.ArrRefTemplateMapType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArrRefTemplateMapSpecRepository extends ElzaJpaRepository<ArrRefTemplateMapSpec, Integer>  {

    @Query("SELECT rtms FROM arr_ref_template_map_spec rtms JOIN rtms.refTemplateMapType rtmt WHERE rtmt.refTemplate = :refTemplate")
    List<ArrRefTemplateMapSpec> findByRefTemplate(@Param("refTemplate") ArrRefTemplate refTemplate);

    @Query("SELECT rtms FROM arr_ref_template_map_spec rtms JOIN rtms.refTemplateMapType rtmt WHERE rtmt.refTemplate IN :refTemplates")
    List<ArrRefTemplateMapSpec> findByRefTemplates(@Param("refTemplates") List<ArrRefTemplate> refTemplates);

    @Modifying
    @Query("DELETE FROM arr_ref_template_map_spec rtms WHERE rtms.refTemplateMapType = :refTemplateMapType")
    void deleteByRefTemplateMapType(@Param("refTemplateMapType") ArrRefTemplateMapType refTemplateMapType);

    @Modifying
    @Query("DELETE FROM arr_ref_template_map_spec rtms WHERE rtms.refTemplateMapType IN :refTemplateMapTypes")
    void deleteByRefTemplateMapTypes(@Param("refTemplateMapTypes") List<ArrRefTemplateMapType> refTemplateMapTypes);

    @Modifying
    @Query("DELETE FROM arr_ref_template_map_spec rtms WHERE rtms.refTemplateMapType IN " + 
            "(SELECT rtmt FROM arr_ref_template_map_type rtmt WHERE rtmt.refTemplate IN (SELECT rt FROM arr_ref_template rt WHERE rt.fund = :fund))")
    void deleteByFund(@Param("fund") ArrFund fund);
}
