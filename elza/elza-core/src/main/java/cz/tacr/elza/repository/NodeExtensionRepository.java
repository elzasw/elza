package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeExtension;


/**
 * Repository pro {@link ArrNodeExtension}.
 *
 * @since 23.10.2017
 */
@Repository
public interface NodeExtensionRepository extends JpaRepository<ArrNodeExtension, Integer>, NodeExtensionRepositoryCustom {

    @Query("SELECT i FROM arr_node_extension i JOIN i.arrangementExtension n WHERE n.arrangementExtensionId IN :ids AND i.node = :node AND i.deleteChange IS NULL")
    List<ArrNodeExtension> findByArrExtensionIdsAndNodeNotDeleted(@Param("ids") final Collection<Integer> ids, @Param("node") final ArrNode node);

    void deleteByNodeFund(ArrFund fund);
}
