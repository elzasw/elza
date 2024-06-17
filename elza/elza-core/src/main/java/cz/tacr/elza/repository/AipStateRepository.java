package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AipStateRepository extends JpaRepository<DaAipState, Integer> {


    @Query("Select das from da_aip_state das where das.deleteChange is null and da_aip = :daAip")
    DaAipState findByDaAipAndDeleteChangeIsNull(@Param("daAip") DaAip daAip);
}
