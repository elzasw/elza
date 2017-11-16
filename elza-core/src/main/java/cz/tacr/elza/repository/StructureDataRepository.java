package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.RulStructureExtension;
import cz.tacr.elza.domain.RulStructureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    @Query("SELECT sd FROM arr_structure_data sd JOIN FETCH sd.structureType WHERE sd.structureDataId IN :structureDataIds")
    List<ArrStructureData> findByIdsFetch(@Param("structureDataIds") List<Integer> structureDataIds);

    @Query("SELECT sd.structureDataId FROM arr_structure_data sd WHERE sd.structureType IN :structureTypes AND sd.deleteChange IS NULL")
    List<Integer> findStructureDataIdByStructureTypes(@Param("structureTypes") Collection<RulStructureType> structureTypes);

    @Query("SELECT sd.structureDataId FROM arr_structure_data sd JOIN sd.structureType st " +
            "JOIN rul_structure_extension se ON se.structureType = st JOIN arr_fund_structure_extension fse ON fse.structureExtension = se " +
            "WHERE se IN :structureExtensions AND fse.fund = sd.fund AND fse.deleteChange IS NULL AND sd.deleteChange IS NULL")
    List<Integer> findStructureDataIdByActiveStructureExtensions(@Param("structureExtensions") Collection<RulStructureExtension> structureExtensions);

    @Modifying
    @Query("DELETE FROM arr_structure_data WHERE state = 'TEMP'")
    void deleteByStateTemp();

    @Query("SELECT sd.createChange FROM arr_structure_data sd WHERE sd.state = 'TEMP'")
    List<ArrChange> findTempChange();

    @Query("SELECT sd.createChange FROM arr_structure_data sd WHERE sd.state = 'TEMP' AND sd = :structureData")
    ArrChange findTempChangeByStructureData(@Param("structureData") ArrStructureData structureData);
}
