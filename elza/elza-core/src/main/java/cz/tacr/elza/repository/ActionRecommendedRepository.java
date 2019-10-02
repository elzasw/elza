package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulActionRecommended;
import cz.tacr.elza.domain.RulOutputType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Repository pro {@link RulActionRecommended}
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Repository
public interface ActionRecommendedRepository extends JpaRepository<RulActionRecommended, Integer> {


    void deleteByAction(RulAction rulAction);

    List<RulActionRecommended> findByAction(RulAction rulAction);

    @Query("SELECT r FROM rul_action_recommended r JOIN r.outputType ot WHERE ot.code = :code AND r.action = :action")
    RulActionRecommended findOneByOutputTypeCodeAndAction(@Param("code") String code, @Param("action") RulAction rulAction);

    List<RulActionRecommended> findByOutputType(RulOutputType outputType);
}
