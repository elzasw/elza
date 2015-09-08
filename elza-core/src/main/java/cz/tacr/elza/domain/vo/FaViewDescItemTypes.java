package cz.tacr.elza.domain.vo;

import java.util.List;

import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulFaView;


/**
 * Obalující objekt pro {@link RulFaView} a {@link RulDescItemType}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 9. 2015
 */
public class FaViewDescItemTypes implements cz.tacr.elza.api.vo.FaViewDescItemTypes<RulDescItemType, RulFaView> {

    private RulFaView rulFaView;

    private List<RulDescItemType> descItemTypes;

    @Override
    public RulFaView getRulFaView() {
        return rulFaView;
    }

    @Override
    public void setRulFaView(RulFaView rulFaView) {
        this.rulFaView = rulFaView;
    }

    @Override
    public List<RulDescItemType> getDescItemTypes() {
        return descItemTypes;
    }

    @Override
    public void setDescItemTypes(List<RulDescItemType> descItemTypes) {
        this.descItemTypes = descItemTypes;
    }

}
