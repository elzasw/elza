package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArrFundRepository extends JpaRepository<ArrFund, Integer> {

    Optional<ArrFund> findByInternalCode(String code);
}
