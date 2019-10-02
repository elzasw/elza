package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDataJsonTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * @author Martin Šlapa
 * @since 21.06.2016
 */
@Repository
public interface DataJsonTableRepository extends JpaRepository<ArrDataJsonTable, Integer> {

}
