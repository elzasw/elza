package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ImpItem;
import cz.tacr.elza.domain.ImpItemResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ImpItemResultRepository extends JpaRepository<ImpItemResult, Integer> {

    List<ImpItemResult> findByItemOrderByItemResultIdAsc(ImpItem item);

    @Query("SELECT DISTINCT r.fund.fundId FROM cz.tacr.elza.domain.ImpItemResult r"
            + " WHERE r.item.itemId = :itemId AND r.fund IS NOT NULL"
            + " ORDER BY r.fund.fundId")
    List<Integer> findDistinctFundIdsByItem(@Param("itemId") int itemId);
}
