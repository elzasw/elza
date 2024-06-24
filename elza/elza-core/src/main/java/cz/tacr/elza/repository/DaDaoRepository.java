package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaDao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DaDaoRepository extends JpaRepository<DaDao, Integer> {

    List<DaDao> findByAipAndDeleteChangeIsNull(DaAip aip);
}
