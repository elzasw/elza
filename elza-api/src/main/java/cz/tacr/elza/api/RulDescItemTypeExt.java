package cz.tacr.elza.api;

import java.util.List;

/**
 * Rozšíření {@link RulDescItemType} o podtypy a pravidla.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 *
 * @param <RT> {@link RulDataType}
 * @param <RC> {@link RulDescItemConstraint}
 * @param <RS> {@link RulDescItemSpecExt}
 */
public interface RulDescItemTypeExt<RT extends RulDataType, RC extends RulDescItemConstraint, RS extends RulDescItemSpecExt, P extends RulPackage> extends RulDescItemType<RT, P> {

    /**
     * 
     * @return podtypy typů atributů.
     */
    List<RS> getRulDescItemSpecList();

    /**
     * 
     * @return pravidla typů atributů.
     */
    List<RC> getRulDescItemConstraintList();

    /**
     * 
     * @param rulDescItemSpecList podtypy typů atributů.
     */
    void setRulDescItemSpecList(List<RS> rulDescItemSpecList);

    /**
     * 
     * @param rulDescItemConstraintList pravidla typů atributů.
     */
    void setRulDescItemConstraintList(List<RC> rulDescItemConstraintList);
}
