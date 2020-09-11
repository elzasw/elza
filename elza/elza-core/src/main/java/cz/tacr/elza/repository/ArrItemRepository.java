package cz.tacr.elza.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrItem;

@Repository
public interface ArrItemRepository extends CrudRepository<ArrItem, Integer> {
}
