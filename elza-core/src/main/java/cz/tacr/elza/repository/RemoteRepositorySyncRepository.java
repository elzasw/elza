package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaRemoteRepositorySync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RemoteRepositorySyncRepository extends JpaRepository<DaRemoteRepositorySync, Integer> {

}
