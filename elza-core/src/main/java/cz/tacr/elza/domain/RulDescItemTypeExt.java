package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;


/**
 * popis {@link cz.tacr.elza.api.RulDescItemTypeExt}.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public class RulDescItemTypeExt extends RulDescItemType implements cz.tacr.elza.api.RulDescItemTypeExt<RulDataType, RulDescItemSpecExt, RulPackage> {

    private List<RulDescItemSpecExt> rulDescItemSpecList = new LinkedList<>();

    @Override
    public List<RulDescItemSpecExt> getRulDescItemSpecList() {
        return this.rulDescItemSpecList;
    }

    public void setRulDescItemSpecList(List<RulDescItemSpecExt> rulDescItemSpecList) {
        this.rulDescItemSpecList = rulDescItemSpecList;
    }
}
