package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;


/**
 * Rozšíření {@link RulItemType} o podtypy a pravidla.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public class RulItemTypeExt extends RulItemType {

    private List<RulItemSpecExt> rulItemSpecList = new LinkedList<>();

    /**
     *
     * @return podtypy typů atributů.
     */
    public List<RulItemSpecExt> getRulItemSpecList() {
        return this.rulItemSpecList;
    }

    /**
     *
     * @param rulDescItemSpecList podtypy typů atributů.
     */
    public void setRulItemSpecList(final List<RulItemSpecExt> rulDescItemSpecList) {
        this.rulItemSpecList = rulDescItemSpecList;
    }
}
