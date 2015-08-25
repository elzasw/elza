package cz.tacr.elza.api;

import java.util.List;

/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface RulDescItemTypeExt<RT extends RulDataType, RC extends RulDescItemConstraint, RS extends RulDescItemSpecExt> extends RulDescItemType<RT> {

    List<RS> getRulDescItemSpecList();

    List<RC> getRulDescItemConstraintList();

    void setRulDescItemSpecList(List<RS> rulDescItemSpecList);

    void setRulDescItemConstraintList(List<RC> rulDescItemConstraintList);
}
