package cz.tacr.elza.api;

import java.util.List;

/**
 * Rozšíření {@link RulDescItemSpec} o omezení {@link RulDescItemConstraint}.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 *
 * @param <RIT> {@link RulDescItemType}
 * @param <RC> {@link RulDescItemConstraint}
 */
public interface RulDescItemSpecExt<RIT extends RulDescItemType, RC extends RulDescItemConstraint,
        RT extends RegRegisterType> extends RulDescItemSpec<RIT> {

    List<RC> getRulDescItemConstraintList();

    void setRulDescItemConstraintList(List<RC> rulDescItemConstraintList);

}
