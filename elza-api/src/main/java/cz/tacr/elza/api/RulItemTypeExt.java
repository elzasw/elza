package cz.tacr.elza.api;

import java.util.List;

/**
 * Rozšíření {@link RulItemType} o podtypy a pravidla.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 *
 * @param <RT> {@link RulDataType}
 * @param <RS> {@link RulItemSpecExt}
 */
public interface RulItemTypeExt<RT extends RulDataType, RS extends RulItemSpecExt, P extends RulPackage> extends RulItemType<RT, P> {

    /**
     * 
     * @return podtypy typů atributů.
     */
    List<RS> getRulItemSpecList();

    /**
     * 
     * @param rulDescItemSpecList podtypy typů atributů.
     */
    void setRulItemSpecList(List<RS> rulDescItemSpecList);
}
