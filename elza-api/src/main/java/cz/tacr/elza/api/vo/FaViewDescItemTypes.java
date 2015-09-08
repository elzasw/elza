package cz.tacr.elza.api.vo;

import java.util.List;

import cz.tacr.elza.api.RulFaView;

/**
 * Zapouzdření {@link RulFaView} a {@link FaViewDescItemTypes}.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 9. 2015
 *
 * @param <RDIT> {@link FaViewDescItemTypes}
 * @param <RFV> {@link RulFaView}
 */
public interface FaViewDescItemTypes<RDIT, RFV> {

    RFV getRulFaView();

    void setRulFaView(RFV rulFaView);

    List<RDIT> getDescItemTypes();

    void setDescItemTypes(List<RDIT> descItemTypes);
}
