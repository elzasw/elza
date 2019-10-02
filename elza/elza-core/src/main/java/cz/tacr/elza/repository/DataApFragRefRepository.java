package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDataApFragRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataApFragRefRepository extends JpaRepository<ArrDataApFragRef, Integer> {

}
