package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AipStateRepository extends JpaRepository<DaAipState, Integer> {
    DaAipState findByDaAipAndDeleteChangeIsNull(@Param("daAip") DaAip daAip);

    List<DaAipState> findByDaAipInAndDeleteChangeIsNull(List<DaAip> aipList);

    /**
     * All states paired with the fund, the historical ones included.
     */
    List<DaAipState> findByFund(ArrFund fund);
}
