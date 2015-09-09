package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.RulFaView;
import cz.tacr.elza.domain.RulRuleSet;


/**
 * Respozitory pro nastavení zobrazení archivního popisu pomůcky.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface FaViewRepository extends JpaRepository<RulFaView, Integer> {

    @Query("SELECT fv FROM rul_fa_view fv WHERE fv.ruleSet = ?1 and fv.arrangementType = ?2")
    List<RulFaView> findByRuleSetAndArrangementType(RulRuleSet ruleSet, ArrArrangementType arrangementType);
}
