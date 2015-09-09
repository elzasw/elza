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
 * @param <FA> {@link ArrFindingAid}
 * @param <FC> {@link ArrFaChange}
 * @param <FL> {@link ArrFaLevel}
 * @param <AT> {@link RulArrangementType}
 * @param <RS> {@link RulRuleSet}
 */
public interface ArrFaVersion<FA extends ArrFindingAid, FC extends ArrFaChange, FL extends ArrFaLevel, AT extends RulArrangementType, RS extends RulRuleSet>
        extends
            Versionable,
            Serializable {

    Integer getFaVersionId();

    void setFaVersionId(Integer faVersionId);

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
    FL getRootFaLevel();

    /**
     * @param rootNode odkaz na root uzel struktury archivního popisu .
     */
    void setRootFaLevel(FL rootNode);

    /**
     * @return identifikátor archívní pomůcky.
     */
    FA getFindingAid();

    /**
     * @param findingAid identifikátor archívní pomůcky.
     */
    void setFindingAid(FA findingAid);

    /**
     * @return odkaz na pravidla výstupu.
     */
    AT getArrangementType();

    /**
     * @param arrangementType odkaz na pravidla výstupu.
     */
    void setArrangementType(AT arrangementType);

    /**
     * @return odkaz na pravidla tvorby.
     */
    RS getRuleSet();

    /**
     * @param ruleSet odkaz na pravidla tvorby.
     */
    void setRuleSet(RS ruleSet);
}
