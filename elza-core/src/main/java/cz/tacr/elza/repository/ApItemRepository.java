package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ApItemRepository extends JpaRepository<ApItem, Integer> {

}
