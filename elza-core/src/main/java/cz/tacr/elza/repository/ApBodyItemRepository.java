package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApBodyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApBodyItemRepository extends JpaRepository<ApBodyItem, Integer> {

}
