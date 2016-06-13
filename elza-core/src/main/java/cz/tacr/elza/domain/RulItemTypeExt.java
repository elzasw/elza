package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;


/**
 * popis {@link cz.tacr.elza.api.RulItemTypeExt}.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public class RulItemTypeExt extends RulItemType implements cz.tacr.elza.api.RulItemTypeExt<RulDataType, RulItemSpecExt, RulPackage> {

    private List<RulItemSpecExt> rulItemSpecList = new LinkedList<>();

    @Override
    public List<RulItemSpecExt> getRulItemSpecList() {
        return this.rulItemSpecList;
    }

    public void setRulItemSpecList(List<RulItemSpecExt> rulDescItemSpecList) {
        this.rulItemSpecList = rulDescItemSpecList;
    }
}
