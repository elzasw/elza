package cz.tacr.elza.api;

import java.util.List;

/**
 * Rozšíření {@link RulDescItemType} o podtypy a pravidla.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 *
 * @param <RT> {@link RulDataType}
 * @param <RS> {@link RulDescItemSpecExt}
 */
public interface RulDescItemTypeExt<RT extends RulDataType, RS extends RulDescItemSpecExt, P extends RulPackage> extends RulDescItemType<RT, P> {

    /**
     * 
     * @return podtypy typů atributů.
     */
    List<RS> getRulDescItemSpecList();

    /**
     * 
     * @param rulDescItemSpecList podtypy typů atributů.
     */
    void setRulDescItemSpecList(List<RS> rulDescItemSpecList);
}
