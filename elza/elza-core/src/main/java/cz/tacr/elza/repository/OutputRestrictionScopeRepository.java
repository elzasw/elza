package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrOutputRestrictionScope;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutputRestrictionScopeRepository extends ElzaJpaRepository<ArrOutputRestrictionScope, Integer> {

    @Query("DELETE FROM arr_output_restriction_scope res WHERE res.output.outputId = :outputId AND res.scope.scopeId = :scopeId")
    @Modifying
    void deleteByOutputAndScope(@Param("outputId") Integer outputId, @Param("scopeId") Integer scopeId);

    @Query("SELECT res FROM arr_output_restriction_scope res JOIN res.scope WHERE res.output.outputId = :outputId")
    List<ArrOutputRestrictionScope> findByOutput(@Param("outputId") Integer outputId);
}
