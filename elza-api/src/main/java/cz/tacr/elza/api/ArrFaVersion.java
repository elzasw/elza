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

    FC getCreateChange();

    void setCreateChange(FC createChange);

    FC getLockChange();

    void setLockChange(FC lockChange);

    FL getRootFaLevel();

    void setRootFaLevel(FL rootNode);

    FA getFindingAid();

    void setFindingAid(FA findingAid);

    AT getArrangementType();

    void setArrangementType(AT arrangementType);

    RS getRuleSet();

    void setRuleSet(RS ruleSet);
}
