package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Typy kontrol, validací, archivního popisu. Každá chyba validace má přiřazen právě jeden typ kontroly (validace),
 * který je k chybě přiřazen na výstupu z pravidel (drools).
 *
 * @author Martin Šlapa
 * @since 22.3.2016
 *
 */
public interface RulPolicyType<P extends RulPackage, RS extends RulRuleSet> extends Serializable {

    /**
     * @return identifikátor položky
     */
    Integer getPolicyTypeId();

    /**
     * @param policyTypeId identifikátor položky
     */
    void setPolicyTypeId(Integer policyTypeId);

    /**
     * @return kód typu
     */
    String getCode();

    /**
     * @param code kód typu
     */
    void setCode(String code);

    /**
     * @return název typu
     */
    String getName();

    /**
     * @param name název typu
     */
    void setName(String name);

    /**
     * @return balíček pravidel
     */
    P getRulPackage();

    /**
     * @param rulPackage balíček pravidel
     */
    void setRulPackage(P rulPackage);

    /**
     * @return pravidla
     */
    RS getRuleSet();

    /**
     * @param ruleSet pravidla
     */
    void setRuleSet(RS ruleSet);

}
