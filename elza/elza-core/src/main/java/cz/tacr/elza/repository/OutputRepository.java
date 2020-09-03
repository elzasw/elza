package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutput.OutputState;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.repository.vo.ItemChange;
import cz.tacr.elza.service.arrangement.DeleteFundHistory;

/**
 * Respozitory pro výstup z archivního souboru.
 */
@Repository
public interface OutputRepository extends ElzaJpaRepository<ArrOutput, Integer>, OutputRepositoryCustom, DeleteFundHistory {

    /**
     * Najde platné AP (není deleted), pro které existuje otevřená verze.
     *
     * @param fund archivní fond
     * @return seznam platných AP pro fond
     */
    @Query("SELECT o FROM arr_output o WHERE o.fund=?1 AND o.deleteChange IS NULL")
    List<ArrOutput> findValidOutputByFund(ArrFund fund);

    /**
     * Najde platné AP (není deleted), pro které existuje alespoň jedna uzavřená verze
     *
     * @param fund archivní fond
     * @return seznam platných historických AP pro fond
     */
    // @Query("SELECT o FROM arr_output o LEFT JOIN FETCH o.deleteChange lc WHERE o.fund=?1 AND o.deleteChange IS NULL AND o.lockChange IS NOT NULL")
    // List<ArrOutput> findHistoricalOutputByFund(ArrFund fund);

    @Query("SELECT o FROM arr_output o WHERE o.outputId=?1")
    ArrOutput findByOutputId(Integer outputId);

    @Query("SELECT o FROM arr_output o JOIN FETCH o.outputType ot JOIN FETCH o.fund f WHERE o.outputId=?1")
    ArrOutput findOneFetchTypeAndFund(int outputId);

    @Modifying
    @Query("UPDATE arr_output o SET o.state = ?2, o.error = ?3 WHERE o.state IN ?1")
    int updateStateFromStateWithError(List<OutputState> statesToFind, OutputState stateToSet, String errorMessage);

    @Query("SELECT o FROM arr_output o WHERE o.outputType IN ?1")
    List<ArrOutput> findByOutputTypes(List<RulOutputType> rulOutputTypes);

    @Query("SELECT COUNT(o) > 0 FROM arr_output o WHERE o.name LIKE :name")
    boolean existsByName(@Param("name") String name);

    void deleteByFund(ArrFund fund);

    @Query("SELECT o FROM arr_output o" +
            " JOIN o.fund f" +
            " JOIN f.versions v" +
            " WHERE v = :fundVersion" +
            " AND o.deleteChange IS NULL" +
            " ORDER BY o.lastUpdate DESC, o.name ASC")
    List<ArrOutput> findByFundVersionSorted(@Param("fundVersion") ArrFundVersion fundVersion);

    @Query("SELECT o FROM arr_output o" +
            " JOIN o.fund f" +
            " JOIN f.versions v" +
            " WHERE v = :fundVersion" +
            " AND o.deleteChange IS NULL" +
            " AND o.state = :state" +
            " ORDER BY o.lastUpdate DESC, o.name ASC")
    List<ArrOutput> findByFundVersionAndStateSorted(@Param("fundVersion") ArrFundVersion fundVersion, @Param("state") OutputState state);

    @Override
    @Query("SELECT new cz.tacr.elza.repository.vo.ItemChange(o.outputId, o.createChange.changeId) FROM arr_output o "
            + "WHERE o.fund = :fund")
    List<ItemChange> findByFund(@Param("fund") ArrFund fund);

    @Override
    @Modifying
    @Query("UPDATE arr_output SET createChange = :change WHERE outputId IN :ids")
    void updateCreateChange(@Param("ids") Collection<Integer> ids, @Param("change") ArrChange change);

    void deleteByFundAndDeleteChangeIsNotNull(ArrFund fund);
}
