package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaRemoteRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RemoteRepositoryRepository extends JpaRepository<DaRemoteRepository, Integer> {

}
