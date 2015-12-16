package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;


/**
 * popis {@link cz.tacr.elza.api.RulDescItemTypeExt}.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public class RulDescItemTypeExt extends RulDescItemType implements cz.tacr.elza.api.RulDescItemTypeExt<RulDataType, RulDescItemConstraint, RulDescItemSpecExt, RulPackage> {

    private List<RulDescItemSpecExt> rulDescItemSpecList = new LinkedList<>();

    private List<RulDescItemConstraint> rulDescItemConstraintList = new LinkedList<>();

    @Override
    public List<RulDescItemSpecExt> getRulDescItemSpecList() {
        return this.rulDescItemSpecList;
    }

    @Override
    public List<RulDescItemConstraint> getRulDescItemConstraintList() {
        return this.rulDescItemConstraintList;
    }

    public void setRulDescItemSpecList(List<RulDescItemSpecExt> rulDescItemSpecList) {
        this.rulDescItemSpecList = rulDescItemSpecList;
    }

    public void setRulDescItemConstraintList(List<RulDescItemConstraint> rulDescItemConstraintList) {
        this.rulDescItemConstraintList = rulDescItemConstraintList;
    }
}
