package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.RulStructureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozitory pro {@link ArrStructureData}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructureDataRepository extends JpaRepository<ArrStructureData, Integer>, StructureDataRepositoryCustom {

    List<ArrStructureData> findByStructureTypeAndFundAndDeleteChangeIsNull(RulStructureType structureType, ArrFund fund);

    @Query("SELECT sd FROM arr_structure_data sd WHERE sd.structureType = :structureType AND sd.fund = :fund AND sd.deleteChange IS NULL AND value IS NOT NULL AND sd.state = 'OK'")
    List<ArrStructureData> findValidByStructureTypeAndFund(@Param("structureType") RulStructureType structureType,
                                                           @Param("fund") ArrFund fund);

    @Query("SELECT sd FROM arr_structure_data sd JOIN FETCH sd.structureType WHERE sd.structureDataId = :structureDataId")
    ArrStructureData findOneFetch(@Param("structureDataId") Integer structureDataId);
}
