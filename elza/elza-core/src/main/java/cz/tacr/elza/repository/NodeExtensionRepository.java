package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeExtension;
import cz.tacr.elza.repository.vo.ItemChange;
import cz.tacr.elza.service.arrangement.DeleteFundHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


/**
 * Repository pro {@link ArrNodeExtension}.
 *
 * @since 23.10.2017
 */
@Repository
public interface NodeExtensionRepository extends JpaRepository<ArrNodeExtension, Integer>, NodeExtensionRepositoryCustom, DeleteFundHistory {

    @Query("SELECT i FROM arr_node_extension i JOIN i.arrangementExtension n WHERE n.arrangementExtensionId IN :ids AND i.node = :node AND i.deleteChange IS NULL")
    List<ArrNodeExtension> findByArrExtensionIdsAndNodeNotDeleted(@Param("ids") final Collection<Integer> ids, @Param("node") final ArrNode node);

    void deleteByNodeFund(ArrFund fund);

    void deleteByNodeIdIn(Collection<Integer> nodeIds);

    @Override
    @Query("SELECT new cz.tacr.elza.repository.vo.ItemChange(ne.nodeExtensionId, ne.createChange.changeId) FROM arr_node_extension ne JOIN ne.node n WHERE n.fund = :fund")
    List<ItemChange> findByFund(@Param("fund") ArrFund fund);

    @Override
    @Modifying
    @Query("UPDATE arr_node_extension SET createChange = :change WHERE nodeExtensionId IN :ids")
    void updateCreateChange(@Param("ids") Collection<Integer> ids, @Param("change") ArrChange change);
}
