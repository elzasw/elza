package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaRemoteRepositorySync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DaRemoteRepositorySyncRepository extends JpaRepository<DaRemoteRepositorySync, Integer> {

    DaRemoteRepositorySync findByDigitalRepository(ArrDigitalRepository digitalRepository);
}
