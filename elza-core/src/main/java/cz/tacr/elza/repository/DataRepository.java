package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ArrStructureData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutputDefinition;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DataRepository extends JpaRepository<ArrData, Integer>, DataRepositoryCustom {

    /*@Query(value = "SELECT d FROM arr_data d join fetch d.item i "
            + "left join fetch i.createChange cc "
            + "left join fetch i.deleteChange dc "
            + "left join fetch i.itemType it "
            + "left join fetch i.itemSpec dis "
            + "WHERE i.node in (?1) and i.deleteChange is null")
    List<ArrData> findByNodesAndDeleteChangeIsNull(Collection<ArrNode> nodes);*/

    @Modifying
    @Query("DELETE FROM arr_data d WHERE d.dataId IN :ids")
    void deleteByIds(@Param("ids") Collection<Integer> ids);

    @Query("SELECT d.dataId FROM arr_data d WHERE d IN (SELECT o.data FROM arr_output_item o WHERE o.outputDefinition = :outputDefinition)")
    Collection<Integer> findByIdsOutputDefinition(@Param("outputDefinition") ArrOutputDefinition outputDefinition);

    @Modifying
    @Query("DELETE FROM arr_data d WHERE d.dataId IN (SELECT i.dataId FROM arr_structure_item i WHERE i.structureData = :structureData)")
    void deleteByStructureData(@Param("structureData") ArrStructureData structureData);
}
