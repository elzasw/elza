package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DaDaoFileRepository extends JpaRepository<DaDaoFile, Integer> {

    List<DaDaoFile> findByDaoInAndDeleteChangeIsNull(List<DaDao> daDaoList);

    DaDaoFile findByDao(DaDao dao);
}
