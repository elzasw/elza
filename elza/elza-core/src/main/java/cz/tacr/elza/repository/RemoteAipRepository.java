package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaRemoteAip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RemoteAipRepository extends JpaRepository<DaRemoteAip, Integer> {

}
