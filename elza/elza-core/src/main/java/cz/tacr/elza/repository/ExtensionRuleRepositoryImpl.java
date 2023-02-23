package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulExtensionRule;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;

/**
 * Implementace repository pro {@link RulExtensionRule} - Custom.
 *
 * @since 23.10.2017
 */
@Component
public class ExtensionRuleRepositoryImpl implements ExtensionRuleRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<RulExtensionRule> findExtensionRules(final List<RulArrangementExtension> arrangementExtensions,
                                                     final RulExtensionRule.RuleType ruleType) {
        if (arrangementExtensions.isEmpty()) {
            return Collections.emptyList();
        }

        String hql = "SELECT er FROM rul_extension_rule er JOIN er.arrangementExtension ae WHERE ae IN :arrangementExtensions AND er.ruleType = :ruleType";

        TypedQuery<RulExtensionRule> query = entityManager.createQuery(hql, RulExtensionRule.class);
        query.setParameter("arrangementExtensions", arrangementExtensions);
        query.setParameter("ruleType", ruleType);

        List<RulExtensionRule> resultList = query.getResultList();

        resultList.sort((o1, o2) -> {

            // 1. seřadit podle priority
            Integer p1 = o1.getPriority();
            Integer p2 = o1.getPriority();

            int pComp = p1.compareTo(p2);
            if (pComp != 0) {
                return pComp;
            } else {
                // 2. seřadit podle seřazení definicí
                RulArrangementExtension arrangementExtension1 = o1.getArrangementExtension();
                RulArrangementExtension arrangementExtension2 = o2.getArrangementExtension();
                Integer ae1 = arrangementExtensions.indexOf(arrangementExtension1);
                Integer ae2 = arrangementExtensions.indexOf(arrangementExtension2);
                return ae1.compareTo(ae2);
            }
        });

        return resultList;
    }
}
