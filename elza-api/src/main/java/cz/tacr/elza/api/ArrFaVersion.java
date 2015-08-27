package cz.tacr.elza.api;

import java.io.Serializable;

public interface ArrFaVersion<FA extends ArrFindingAid, FC extends ArrFaChange,FL extends ArrFaLevel,
        AT extends ArrArrangementType, RS extends RulRuleSet> extends Versionable, Serializable {

    Integer getFaVersionId();

    void setFaVersionId(Integer faVersionId);

    FC getCreateChange();

    void setCreateChange(FC createChange);

    FC getLockChange();

    void setLockChange(FC lockChange);

    FL getRootNode();

    void setRootNode(FL rootNode);

    FA getFindingAid();

    void setFindingAid(FA findingAid);

    AT getArrangementType();

    void setArrangementType(AT arrangementType);

    RS getRuleSet();

    void setRuleSet(RS ruleSet);
}
