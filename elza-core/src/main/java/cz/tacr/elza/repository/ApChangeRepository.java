package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApState;

@Repository
public interface ApChangeRepository extends ElzaJpaRepository<ApChange, Integer> {

    ApChange findTop1ByOrderByChangeIdDesc();

    ApChange findTop1ByOrderByChangeIdAsc();

    @Query("""
        select c from ap_change c
        left join ap_state s on s.createChange = c
		left join ap_rev_state rs on rs.createChange = c
		left join rs.revision rv
		left join rv.state rvs
		where rvs.accessPoint = :ap or s.accessPoint = :ap
    """)
    List<ApChange> findByApState(ApAccessPoint ap);
}
