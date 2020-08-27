package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ApBindingItemRepository extends ElzaJpaRepository<ApBindingItem, Integer> {

    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.binding = :binding AND bi.value = :uuid")
    ApBindingItem findByBindingAndUuid(@Param("binding") ApBinding binding, @Param("uuid") String uuid);

    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.binding IN :bindings")
    List<ApBindingItem> findByBindings(@Param("bindings") Collection<ApBinding> bindingList);

    @Query("SELECT bi FROM ap_binding_item bi LEFT JOIN FETCH bi.part LEFT JOIN FETCH bi.item i LEFT JOIN FETCH i.data WHERE bi.binding = :binding")
    List<ApBindingItem> findByBinding(@Param("binding") ApBinding binding);

    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.part IS NOT NULL AND bi.part = :part")
    List<ApBindingItem> findByPart(@Param("part") ApPart part);

    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.item IS NOT NULL AND bi.item = :item")
    List<ApBindingItem> findByItem(@Param("item") ApItem item);

    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.part IS NOT NULL AND bi.part IN :parts")
    List<ApBindingItem> findByParts(@Param("parts") List<ApPart> parts);

    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.item IS NOT NULL AND bi.item IN :items")
    List<ApBindingItem> findByItems(@Param("items") List<ApItem> items);

    void deleteByBinding(ApBinding binding);

    @Query("SELECT bi FROM ap_binding_item bi LEFT JOIN FETCH bi.part WHERE bi.binding = :binding AND bi.part IS NOT NULL")
    List<ApBindingItem> findPartsByBinding(@Param("binding") ApBinding binding);

    @Query("SELECT bi FROM ap_binding_item bi LEFT JOIN FETCH bi.item i LEFT JOIN FETCH i.data WHERE bi.binding = :binding AND bi.item IS NOT NULL")
    List<ApBindingItem> findItemsByBinding(@Param("binding") ApBinding binding);
}
