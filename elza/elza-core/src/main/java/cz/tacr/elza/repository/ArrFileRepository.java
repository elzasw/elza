package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;

@Repository
public interface ArrFileRepository extends JpaRepository<ArrFile, Integer> {

    @Query("SELECT af.fileId FROM arr_file af WHERE af.fund = :fund AND af.createChange >= :change")
    List<Integer> findIdByFundAndGreaterOrEqualCreateChange(@Param("fund") ArrFund fund, @Param("change") ArrChange change);

    @Query("SELECT af FROM arr_file af WHERE af.fund = :fund AND af.deleteChange IS NOT NULL")
    List<ArrFile> findHistoricalByFund(@Param("fund") ArrFund fund);

}
