package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApBindingItemRepository extends ElzaJpaRepository<ApBindingItem, Integer> {

    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.binding = :binding AND bi.value = :uuid")
    ApBindingItem findByBindingAndUuid(@Param("binding") ApBinding binding, @Param("uuid") String uuid);
}
