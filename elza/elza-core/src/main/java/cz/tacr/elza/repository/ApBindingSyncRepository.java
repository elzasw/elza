package cz.tacr.elza.repository;


import cz.tacr.elza.domain.ApBindingSync;
import cz.tacr.elza.domain.ApExternalSystem;
import org.springframework.stereotype.Repository;

@Repository
public interface ApBindingSyncRepository extends ElzaJpaRepository<ApBindingSync, Integer> {

    ApBindingSync findByApExternalSystem(ApExternalSystem apExternalSystem);

}
