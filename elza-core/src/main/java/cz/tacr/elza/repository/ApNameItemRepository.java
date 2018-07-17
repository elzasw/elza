package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApNameItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApNameItemRepository extends JpaRepository<ApNameItem, Integer> {

}
