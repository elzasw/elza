package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;

public interface ApBindingRepository extends ElzaJpaRepository<ApBinding, Integer> {

    @Query("SELECT bin FROM ap_binding bin WHERE bin.value = :archiveEntityId AND bin.apExternalSystem = :externalSystem")
    ApBinding findByValueAndExternalSystem(@Param("archiveEntityId") String archiveEntityId,
                                                     @Param("externalSystem") ApExternalSystem externalSystem);

    @Query("SELECT bin FROM ap_binding bin WHERE bin.value IN :values AND bin.apExternalSystem = :externalSystem")
    List<ApBinding> findByValuesAndExternalSystem(@Param("values") Collection<String> values,
                                                  @Param("externalSystem") ApExternalSystem externalSystem);

    @Query("SELECT bin FROM ap_binding bin JOIN bin.apExternalSystem aes WHERE bin.value = :archiveEntityId AND aes.code = :externalSystemCode")
    ApBinding findByValueAndExternalSystemCode(@Param("archiveEntityId") String archiveEntityId,
                                           @Param("externalSystemCode") String externalSystemCode);

    @Query("SELECT bin FROM ap_binding bin JOIN bin.apExternalSystem aes WHERE bin.value IN :values AND aes.type = :type")
    List<ApBinding> findByValuesAndExternalSystemType(@Param("values") Collection<String> values,
                                                      @Param("type") ApExternalSystemType type);
}
