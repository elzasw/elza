package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AipStateRepository extends JpaRepository<DaAipState, Integer> {


    DaAipState findByDaAipAndDeleteChangeIsNull(@Param("daAip") DaAip daAip);

    @Query("SELECT das FROM da_aip_state das WHERE das.daAip = :daAip")
    DaAipState findByAip(@Param("daAip") DaAip daAip);
}
