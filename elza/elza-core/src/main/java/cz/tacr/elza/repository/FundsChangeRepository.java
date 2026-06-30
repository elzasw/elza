package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFundsChange;

/**
 * Repository pro seskupení operací zasahujících více archivních souborů.
 */
@Repository
public interface FundsChangeRepository extends JpaRepository<ArrFundsChange, Integer> {
}
