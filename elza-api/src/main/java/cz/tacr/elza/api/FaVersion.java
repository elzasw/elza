package cz.tacr.elza.api;

import java.io.Serializable;

public interface FaVersion<FA extends FindingAid, FC extends FaChange,FL extends FaLevel,
        AT extends ArrangementType, RS extends RuleSet> extends Serializable {

    Integer getFaVersionId();

    void setFaVersionId(Integer faVersionId);

    FC getCreateChange();

    void setCreateChange(FC createChange);

    FC getLockChange();

    void setLockChange(FC lockChange);

    FaLevel getRootNode();

    void setRootNode(FL rootNode);

    FA getFindingAid();

    void setFindingAid(FA findingAid);

    AT getArrangementType();

    void setArrangementType(AT arrangementType);

    RS getRuleSet();

    void setRuleSet(RS ruleSet);
}
