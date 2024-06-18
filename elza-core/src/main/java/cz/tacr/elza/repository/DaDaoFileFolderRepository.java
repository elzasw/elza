package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaDaoFileFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DaDaoFileFolderRepository extends JpaRepository<DaDaoFileFolder, Integer> {
}
