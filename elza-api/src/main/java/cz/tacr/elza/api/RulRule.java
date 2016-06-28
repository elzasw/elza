package cz.tacr.elza.api;

import java.io.Serializable;


public interface RulRule<P extends RulPackage, RS extends RulRuleSet> extends Serializable {

    /**
     * Typy pravidel.
     */
    enum RuleType {
        CONFORMITY_INFO,
        CONFORMITY_IMPACT,
        ATTRIBUTE_TYPES,
        OUTPUT_ATTRIBUTE_TYPES,
        NEW_LEVEL
    }


    /**
     * @return identifikátor entity
     */
    Integer getRuleId();


    /**
     * @param ruleId identifikátor entity
     */
    void setRuleId(Integer ruleId);


    /**
     * @return pravidla
     */
    RS getRuleSet();


    /**
     * @param ruleSet pravidla
     */
    void setRuleSet(RS ruleSet);


    /**
     * @return balíček
     */
    P getPackage();


    /**
     * @param rulPackage balíček
     */
    void setPackage(P rulPackage);


    /**
     * @return název souboru
     */
    String getFilename();


    /**
     * @param filename název souboru
     */
    void setFilename(String filename);


    /**
     * @return typ pravidel
     */
    RuleType getRuleType();


    /**
     * @param ruleType typ pravidel
     */
    void setRuleType(RuleType ruleType);


    /**
     * @return priorita vykonávání
     */
    Integer getPriority();


    /**
     * @param priority priorita vykonávání
     */
    void setPriority(Integer priority);

}
