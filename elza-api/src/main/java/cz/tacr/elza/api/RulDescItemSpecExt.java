package cz.tacr.elza.api;

import java.util.List;

/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface RulDescItemSpecExt<RIT extends RulDescItemType, RC extends RulDescItemConstraint> extends RulDescItemSpec<RIT> {

    List<RC> getRulDescItemConstraintList();

    void setRulDescItemConstraintList(List<RC> rulDescItemConstraintList);

}
