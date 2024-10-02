package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DaDaoItemRepository extends JpaRepository<DaDaoItem, Integer> {

    List<DaDaoItem> findByDaoInAndDeleteChangeIsNull(List<DaDao> daDaoList);
}
