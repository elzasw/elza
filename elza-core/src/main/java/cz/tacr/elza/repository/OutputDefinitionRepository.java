package cz.tacr.elza.repository;

import cz.tacr.elza.api.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Respozitory pro výstup z archivního souboru.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
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

    @Modifying
    @Query("UPDATE arr_output_definition o SET o.state = ?2, o.error = ?3 WHERE o.state IN ?1")
    int setStateFromStateWithError(List<OutputState> statesToFind, OutputState stateToSet, String errorMessage);
}
