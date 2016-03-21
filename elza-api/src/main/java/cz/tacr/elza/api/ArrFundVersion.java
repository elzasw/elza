package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Vytvářená nebo již schválená verze archivní pomůcky. Základem archivní pomůcky je hierarchický archivní
 * popis. Každá pomůcka je vytvářena podle určitých pravidel tvorby. Pravidla tvorby mohou definovat
 * různé typy finální pomůcky (například manipulační seznam, inventární seznam, katalog v případě
 * ZP)
 * 
 * @author vavrejn
 *
 * @param <FA> {@link ArrFund}
 * @param <FC> {@link ArrChange}
 * @param <FN> {@link ArrNode}
 * @param <RS> {@link RulRuleSet}
 */
public interface ArrFundVersion<FA extends ArrFund, FC extends ArrChange, FN extends ArrNode, RS extends RulRuleSet>
        extends
            Versionable,
            Serializable {

    Integer getFundVersionId();

    void setFundVersionId(Integer fundVersionId);

    /**
     * @return číslo změny vytvoření pomůcky.
     */
    FC getCreateChange();

    /**
     * @param createChange číslo změny vytvoření pomůcky.
     */
    void setCreateChange(FC createChange);

    /**
     * @return číslo změny uzamčení pomůcky.
     */
    FC getLockChange();

    /**
     * @param lockChange číslo změny uzamčení pomůcky.
     */
    void setLockChange(FC lockChange);

    /**
     * @return odkaz na root uzel struktury archivního popisu.
     */
    FN getRootNode();

    /**
     * @param rootNode odkaz na root uzel struktury archivního popisu .
     */
    void setRootNode(FN rootNode);

    /**
     * @return identifikátor archívní pomůcky.
     */
    FA getFund();

    /**
     * @param fund identifikátor archívní pomůcky.
     */
    void setFund(FA fund);

    /**
     * @return odkaz na pravidla tvorby.
     */
    RS getRuleSet();

    /**
     * @param ruleSet odkaz na pravidla tvorby.
     */
    void setRuleSet(RS ruleSet);

    /**
     * @return poslední uživatelská změna nad verzí AP
     */
    FC getLastChange();

    /**
     * @param lastChange poslední uživatelská změna nad verzí AP
     */
    void setLastChange(FC lastChange);

    /**
     * @return vysčítaná informace o časovém rozsahu fondu - sdruženo po typech kalendářů
     */
    String getDateRange();

    /**
     * @param dateRange vysčítaná informace o časovém rozsahu fondu - sdruženo po typech kalendářů
     */
    void setDateRange(String dateRange);
}
