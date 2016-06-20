package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * @author Martin Šlapa
 * @since 17.06.2016
 */
@Repository
public interface ItemRepository extends JpaRepository<ArrItem, Integer> {


}
