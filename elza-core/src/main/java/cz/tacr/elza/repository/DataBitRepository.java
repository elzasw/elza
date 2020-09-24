package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDataBit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataBitRepository extends JpaRepository<ArrDataBit, Integer> {

}
