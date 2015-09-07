package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public class RulDescItemSpecExt extends RulDescItemSpec implements cz.tacr.elza.api.RulDescItemSpecExt<RulDescItemType, RulDescItemConstraint, RegRegisterType> {

    private List<RulDescItemConstraint> rulDescItemConstraintList = new LinkedList<>();

    @Override
    public List<RulDescItemConstraint> getRulDescItemConstraintList() {
        return this.rulDescItemConstraintList;
    }

    public void setRulDescItemConstraintList(List<RulDescItemConstraint> rulDescItemConstraintList) {
        this.rulDescItemConstraintList = rulDescItemConstraintList;
    }
}
