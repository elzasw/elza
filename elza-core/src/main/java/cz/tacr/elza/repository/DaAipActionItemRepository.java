package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.domain.DaAipActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DaAipActionItemRepository extends JpaRepository<DaAipActionItem, Integer> {

    List<DaAipActionItem> findByAipActionOrderByAipActionItemId(DaAipAction aipAction);
}
