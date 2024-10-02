package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoFileFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DaDaoFileFolderRepository extends JpaRepository<DaDaoFileFolder, Integer> {

    List<DaDaoFileFolder> findByRepresentationDaoInAndDeleteChangeIsNull(List<DaDao> daDaoList);
}
