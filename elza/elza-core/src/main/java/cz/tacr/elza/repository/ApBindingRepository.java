package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApScope;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApBindingRepository extends ElzaJpaRepository<ApBinding, Integer> {

    @Query("SELECT bin FROM ap_binding bin JOIN bin.apExternalSystem aes WHERE bin.scope = :scope AND bin.value = :archiveEntityId AND aes.code = :externalSystemCode")
    ApBinding findByScopeAndValueAndApExternalSystem(@Param("scope") ApScope scope,
                                                     @Param("archiveEntityId") String archiveEntityId,
                                                     @Param("externalSystemCode") String externalSystemCode);
}
