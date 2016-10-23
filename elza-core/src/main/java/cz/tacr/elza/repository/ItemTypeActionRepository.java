package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Repository pro {@link RulItemTypeAction}
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Repository
public interface ItemTypeActionRepository extends JpaRepository<RulItemTypeAction, Integer> {

    void deleteByAction(RulAction rulAction);

    List<RulItemTypeAction> findByAction(RulAction rulAction);

    @Query("SELECT i FROM rul_item_type_action i JOIN i.itemType it WHERE it.code = :code")
    RulItemTypeAction findOneByItemTypeCode(@Param("code") String code);

    @Query("SELECT i FROM rul_item_type_action i JOIN i.itemType it WHERE it.code = :code AND i.action = :action")
    RulItemTypeAction findOneByItemTypeCodeAndAction(@Param("code") String code, @Param("action") RulAction action);

    @Query("SELECT it FROM rul_item_type_action i JOIN i.itemType it WHERE i.action IN (:actions)")
    List<RulItemType> findByAction(@Param("actions") List<RulAction> actions);
}
