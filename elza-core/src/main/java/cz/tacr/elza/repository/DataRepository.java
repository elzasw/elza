package cz.tacr.elza.repository;

import java.util.Collection;

import cz.tacr.elza.domain.ArrStructuredObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrOutputDefinition;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DataRepository extends JpaRepository<ArrData, Integer>, DataRepositoryCustom {

    @Modifying
    @Query("DELETE FROM arr_data d WHERE d.dataId IN :ids")
    void deleteByIds(@Param("ids") Collection<Integer> ids);

    @Query("SELECT d.dataId FROM arr_data d WHERE d IN (SELECT o.data FROM arr_output_item o WHERE o.outputDefinition = :outputDefinition)")
    Collection<Integer> findByIdsOutputDefinition(@Param("outputDefinition") ArrOutputDefinition outputDefinition);

    @Modifying
    @Query("DELETE FROM arr_data d WHERE d.dataId IN (SELECT i.dataId FROM arr_structured_item i WHERE i.structuredObject = :structuredObject)")
    void deleteByStructuredObject(@Param("structuredObject") ArrStructuredObject structuredObject);
}
