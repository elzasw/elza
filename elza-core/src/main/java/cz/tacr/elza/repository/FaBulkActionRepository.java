package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrFaBulkAction;


@Repository
public interface FaBulkActionRepository extends JpaRepository<ArrFaBulkAction, Integer> {

    @Query(value = "SELECT ba FROM arr_fa_bulk_action ba JOIN ba.faVersion v WHERE v.findingAidVersionId = :faVersionId")
    List<ArrFaBulkAction> findByFaVersionId(@Param(value = "faVersionId") Integer faVersionId);


    @Query(value = "SELECT ba FROM arr_fa_bulk_action ba JOIN ba.faVersion v WHERE v.findingAidVersionId = :faVersionId AND ba.bulkActionCode = :code")
    List<ArrFaBulkAction> findByFaVersionIdAndBulkActionCode(@Param(value = "faVersionId") final Integer faVersionId,
                                                             @Param(value = "code") final String code);
}
