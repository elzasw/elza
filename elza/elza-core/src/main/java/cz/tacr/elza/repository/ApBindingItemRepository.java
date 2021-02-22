package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ApBindingItemRepository extends ElzaJpaRepository<ApBindingItem, Integer> {

    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.binding = :binding AND bi.value = :uuid AND bi.deleteChange IS NULL")
    ApBindingItem findByBindingAndUuid(@Param("binding") ApBinding binding, @Param("uuid") String uuid);

    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.binding IN :bindings")
    List<ApBindingItem> findByBindings(@Param("bindings") Collection<ApBinding> bindingList);

    @Query("SELECT bi FROM ap_binding_item bi LEFT JOIN FETCH bi.part LEFT JOIN FETCH bi.item i LEFT JOIN FETCH i.data WHERE bi.binding = :binding AND bi.deleteChange IS NULL")
    List<ApBindingItem> findByBinding(@Param("binding") ApBinding binding);

    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.part IS NOT NULL AND bi.part = :part AND bi.deleteChange IS NULL")
    List<ApBindingItem> findByPart(@Param("part") ApPart part);

    /*
    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.item IS NOT NULL AND bi.item = :item")
    List<ApBindingItem> findByItem(@Param("item") ApItem item);
    */

    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.part IS NOT NULL AND bi.part IN :parts AND bi.deleteChange IS NULL")
    List<ApBindingItem> findByParts(@Param("parts") List<ApPart> parts);

    @Query("SELECT bi FROM ap_binding_item bi WHERE bi.item IS NOT NULL AND bi.item IN :items AND bi.deleteChange IS NULL")
    List<ApBindingItem> findByItems(@Param("items") List<ApItem> items);

    void deleteByBinding(ApBinding binding);

    @Query("SELECT bi FROM ap_binding_item bi LEFT JOIN FETCH bi.part WHERE bi.binding = :binding AND bi.part IS NOT NULL AND bi.deleteChange IS NULL")
    List<ApBindingItem> findPartsByBinding(@Param("binding") ApBinding binding);

    @Query("SELECT bi FROM ap_binding_item bi LEFT JOIN FETCH bi.item i LEFT JOIN FETCH i.data WHERE bi.binding = :binding AND bi.item IS NOT NULL AND bi.deleteChange IS NULL")
    List<ApBindingItem> findItemsByBinding(@Param("binding") ApBinding binding);

    /**
     * Zneplatni vsechny doposud platne vazby itemu a partu
     * 
     * @param accessPoint
     * @param deleteChange
     */
    @Modifying
    @Query("UPDATE ap_binding_item bi SET bi.deleteChange = :deleteChange WHERE bi.deleteChange IS NULL AND bi.binding IN (SELECT bs.binding FROM ap_binding_state bs WHERE bs.accessPoint = :accessPoint)")
    void invalidateByAccessPoint(@Param("accessPoint") ApAccessPoint accessPoint,
                                 @Param("deleteChange") ApChange deleteChange);
}
