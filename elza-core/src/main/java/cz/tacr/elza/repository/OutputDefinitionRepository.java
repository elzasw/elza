package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulTemplate;

/**
 * Respozitory pro výstup z archivního souboru.
 */
@Repository
public interface OutputDefinitionRepository extends JpaRepository<ArrOutputDefinition, Integer>, OutputDefinitionRepositoryCustom {

    /**
     * Najde platné AP (není deleted), pro které existuje otevřená verze.
     *
     * @param fund archivní fond
     * @return seznam platných AP pro fond
     */
    @Query("SELECT DISTINCT no FROM arr_output o JOIN o.outputDefinition no WHERE no.fund=?1 AND no.deleted = false AND o.lockChange IS NULL")
    List<ArrOutputDefinition> findValidOutputDefinitionByFund(ArrFund fund);

    /**
     * Najde platné AP (není deleted), pro které existuje alespoň jedna uzavřená verze
     *
     * @param fund archivní fond
     * @return seznam platných historických AP pro fond
     */
    @Query("SELECT DISTINCT no FROM arr_output_definition no JOIN FETCH no.outputs o LEFT JOIN FETCH o.lockChange lc WHERE no.fund=?1 AND no.deleted = false AND o.lockChange IS NOT NULL")
    List<ArrOutputDefinition> findHistoricalOutputDefinitionByFund(ArrFund fund);

    List<ArrOutputDefinition> findByFund(ArrFund fund);

    @Query("SELECT no FROM arr_output_definition no JOIN no.outputs o WHERE o.outputId=?1")
    ArrOutputDefinition findByOutputId(Integer outputId);

    @Query("SELECT od FROM arr_output_definition od JOIN FETCH od.outputType ot JOIN FETCH od.template t JOIN FETCH od.fund f WHERE od.outputDefinitionId=?1")
    ArrOutputDefinition findOneFetchTypeAndTemplateAndFund(int outputDefinitionId);

    @Modifying
    @Query("UPDATE arr_output_definition o SET o.state = ?2, o.error = ?3 WHERE o.state IN ?1")
    int setStateFromStateWithError(List<OutputState> statesToFind, OutputState stateToSet, String errorMessage);

    @Query("SELECT no FROM arr_output_definition no WHERE no.template IN ?1 AND no.state IN ?2 AND NOT deleted = TRUE")
    List<ArrOutputDefinition> findNonDeletedByTemplatesAndStates(List<RulTemplate> rulTemplateToDelete, List<OutputState> states);

    @Query("SELECT no FROM arr_output_definition no WHERE no.outputType IN ?1")
    List<ArrOutputDefinition> findByOutputTypes(List<RulOutputType> rulOutputTypes);

    @Query("SELECT COUNT(no) > 0 FROM arr_output_definition no WHERE no.name LIKE :name")
    boolean existsByName(@Param("name") String name);

    @Modifying
    void deleteByFund(ArrFund fund);

    @Modifying
    @Query("UPDATE arr_output_definition d SET d.template = :value WHERE d.template = :key")
    void updateTemplateByTemplate(@Param(value = "key") RulTemplate key,
                                  @Param(value = "value") RulTemplate value);
}
